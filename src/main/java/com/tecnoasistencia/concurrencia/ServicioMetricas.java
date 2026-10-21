package com.tecnoasistencia.concurrencia;

import com.tecnoasistencia.modelo.CitaTecnica;
import com.tecnoasistencia.modelo.Cliente;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio en segundo plano para procesar cálculos estadísticos y agregados
 * de citas técnicas y clientes sin bloquear el hilo principal de renderizado JavaFX.
 */
public final class ServicioMetricas {

    public record ResumenEstadisticas(
            int totalCitas,
            int pendientes,
            int enProgreso,
            int completadas,
            int canceladas,
            int totalClientes,
            int clientesActivos,
            double porcentajeCompletadas
    ) {}

    private ServicioMetricas() {
    }

    /**
     * Crea un Task que calcula de forma asíncrona el resumen estadístico sobre una copia
     * inmutable de las listas para prevenir ConcurrentModificationException.
     *
     * @param listaCitas    Lista actual de citas.
     * @param listaClientes Lista actual de clientes.
     * @return Task<ResumenEstadisticas> ejecutable en AppThreadPool.
     */
    public static Task<ResumenEstadisticas> calcularMetricasAsync(List<CitaTecnica> listaCitas, List<Cliente> listaClientes) {
        // Tomar copia defensiva para seguridad de hilos
        final List<CitaTecnica> snapshotCitas = (listaCitas != null) ? new ArrayList<>(listaCitas) : List.of();
        final List<Cliente> snapshotClientes = (listaClientes != null) ? new ArrayList<>(listaClientes) : List.of();

        return new Task<>() {
            @Override
            protected ResumenEstadisticas call() {
                int totalC = snapshotCitas.size();
                int pend = 0;
                int enProg = 0;
                int comp = 0;
                int canc = 0;

                for (CitaTecnica c : snapshotCitas) {
                    if (c == null || c.getEstado() == null) continue;
                    String estado = c.getEstado().trim().toLowerCase();
                    switch (estado) {
                        case "pendiente" -> pend++;
                        case "en progreso" -> enProg++;
                        case "completada" -> comp++;
                        case "cancelada" -> canc++;
                        default -> pend++;
                    }
                }

                int totalCl = snapshotClientes.size();
                int activosCl = 0;
                for (Cliente cl : snapshotClientes) {
                    if (cl != null && cl.isActivo()) {
                        activosCl++;
                    }
                }

                double pctComp = (totalC > 0) ? ((double) comp / totalC) * 100.0 : 0.0;

                return new ResumenEstadisticas(
                        totalC,
                        pend,
                        enProg,
                        comp,
                        canc,
                        totalCl,
                        activosCl,
                        pctComp
                );
            }
        };
    }
}
