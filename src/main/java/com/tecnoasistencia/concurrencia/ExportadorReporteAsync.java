package com.tecnoasistencia.concurrencia;

import com.tecnoasistencia.modelo.CitaTecnica;
import javafx.concurrent.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Tarea en segundo plano para la exportación asíncrona de citas técnicas a formato CSV.
 * Implementa actualización de progreso continuo (updateProgress) y mensajes descriptivos (updateMessage)
 * sin bloquear el hilo de renderizado JavaFX.
 */
public final class ExportadorReporteAsync {

    private ExportadorReporteAsync() {
    }

    /**
     * Construye una tarea asíncrona que escribe un archivo CSV en disco.
     *
     * @param citas          Lista de citas a exportar.
     * @param archivoDestino Archivo de salida seleccionado por el usuario.
     * @return Task<File> ejecutable en AppThreadPool.
     */
    public static Task<File> exportarCitasACSVAsync(List<CitaTecnica> citas, File archivoDestino) {
        final List<CitaTecnica> copia = (citas != null) ? new ArrayList<>(citas) : List.of();

        return new Task<>() {
            @Override
            protected File call() throws Exception {
                updateTitle("Exportación de Citas Técnicas");
                int total = copia.size();
                updateProgress(0, Math.max(1, total));
                updateMessage("Iniciando generación de archivo CSV...");

                if (archivoDestino == null) {
                    throw new IllegalArgumentException("El archivo destino no puede ser nulo.");
                }

                // Asegurar directorio padre
                File parent = archivoDestino.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(archivoDestino), StandardCharsets.UTF_8))) {

                    // Escribir BOM UTF-8 para compatibilidad con Excel
                    writer.write('\ufeff');

                    // Encabezados
                    writer.write("ID,Cliente,Fecha,Hora,Tipo de Servicio,Descripcion,Estado");
                    writer.newLine();

                    for (int i = 0; i < total; i++) {
                        if (isCancelled()) {
                            updateMessage("Exportación cancelada.");
                            break;
                        }

                        CitaTecnica c = copia.get(i);
                        String linea = String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                                c.getId(),
                                escaparCSV(c.getNombreCliente()),
                                c.getFechaFormateada(),
                                c.getHoraFormateada(),
                                escaparCSV(c.getTipoServicio()),
                                escaparCSV(c.getDescripcion()),
                                escaparCSV(c.getEstado())
                        );

                        writer.write(linea);
                        writer.newLine();

                        updateProgress(i + 1, total);
                        updateMessage(String.format("Exportando registro %d de %d...", i + 1, total));

                        // Pausa breve para permitir apreciación del progreso concurrente en la UI
                        Thread.sleep(12);
                    }

                    writer.flush();
                }

                updateMessage("Exportación completada exitosamente.");
                return archivoDestino;
            }
        };
    }

    private static String escaparCSV(String valor) {
        if (valor == null) return "";
        return valor.replace("\"", "\"\"");
    }
}
