package com.tecnoasistencia.concurrencia;

import com.tecnoasistencia.conexion.ConexionDB;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Hilo demonio de monitoreo continuo de conexión (Heartbeat / Health Check).
 * Ejecuta periódicamente un ping (SELECT 1) contra la base de datos PostgreSQL en un hilo
 * en segundo plano programado mediante ScheduledExecutorService, informando el estado
 * y la latencia a la interfaz gráfica sin provocar bloqueos.
 */
public final class MonitorConexion {

    public enum EstadoConexion {
        COMPROBANDO("🟡 Comprobando...", "#eab308"),
        EN_LINEA("🟢 DB En línea", "#16a34a"),
        DESCONECTADO("🔴 DB Desconectada", "#dc2626");

        private final String etiqueta;
        private final String colorHex;

        EstadoConexion(String etiqueta, String colorHex) {
            this.etiqueta = etiqueta;
            this.colorHex = colorHex;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public String getColorHex() {
            return colorHex;
        }
    }

    private static final MonitorConexion INSTANCIA = new MonitorConexion();
    private static final DateTimeFormatter HORA_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final SimpleObjectProperty<EstadoConexion> estadoProperty = new SimpleObjectProperty<>(EstadoConexion.COMPROBANDO);
    private final StringProperty detalleProperty = new SimpleStringProperty("Iniciando monitoreo...");

    private ScheduledFuture<?> tareaPeriodica;
    private volatile boolean activo = false;

    private MonitorConexion() {
    }

    public static MonitorConexion getInstance() {
        return INSTANCIA;
    }

    public ReadOnlyObjectProperty<EstadoConexion> estadoProperty() {
        return estadoProperty;
    }

    public StringProperty detalleProperty() {
        return detalleProperty;
    }

    public EstadoConexion getEstadoActual() {
        return estadoProperty.get();
    }

    /**
     * Inicia el hilo demonio de comprobación periódica cada 6 segundos.
     */
    public synchronized void iniciar() {
        if (activo) return;
        activo = true;

        tareaPeriodica = AppThreadPool.scheduleWithFixedDelay(() -> {
            comprobarConexion();
        }, 1, 6, TimeUnit.SECONDS);

        System.out.println("[MonitorConexion] Demonio de monitoreo de PostgreSQL iniciado (frecuencia: cada 6s).");
    }

    /**
     * Detiene el demonio de monitoreo.
     */
    public synchronized void detener() {
        activo = false;
        if (tareaPeriodica != null && !tareaPeriodica.isDone()) {
            tareaPeriodica.cancel(true);
        }
        System.out.println("[MonitorConexion] Demonio de monitoreo detenido.");
    }

    /**
     * Realiza la verificación física de conexión en el hilo actual de trabajo.
     */
    public void comprobarConexion() {
        long inicio = System.currentTimeMillis();
        boolean conecto = false;
        String mensajeError = null;

        try (Connection conn = ConexionDB.conectar()) {
            if (conn != null && !conn.isClosed()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("SELECT 1");
                    conecto = true;
                }
            }
        } catch (Exception e) {
            mensajeError = e.getMessage();
        }

        long latencia = System.currentTimeMillis() - inicio;
        final boolean exito = conecto;
        final long tiempoMs = latencia;
        final String hora = LocalTime.now().format(HORA_FORMAT);
        final String err = mensajeError;

        AsyncUIHelper.runOnFXThread(() -> {
            if (exito) {
                estadoProperty.set(EstadoConexion.EN_LINEA);
                detalleProperty.set(String.format("Conectado (%d ms) a las %s", tiempoMs, hora));
            } else {
                estadoProperty.set(EstadoConexion.DESCONECTADO);
                detalleProperty.set(String.format("Sin conexión [%s] a las %s", (err != null ? err : "Fallo"), hora));
            }
        });
    }
}
