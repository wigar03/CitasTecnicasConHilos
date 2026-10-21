package com.tecnoasistencia.concurrencia;

import com.tecnoasistencia.modelo.CitaTecnica;
import javafx.beans.property.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Hilo demonio en segundo plano encargado de analizar periódicamente las citas técnicas
 * programadas para el día actual y emitir alertas preventivas sobre citas próximas o pendientes.
 */
public final class MonitorRecordatorios {

    private static final MonitorRecordatorios INSTANCIA = new MonitorRecordatorios();

    private final IntegerProperty citasHoyPendientes = new SimpleIntegerProperty(0);
    private final StringProperty mensajeAviso = new SimpleStringProperty("Sin citas próximas hoy.");
    private final BooleanProperty tieneAlertas = new SimpleBooleanProperty(false);

    private ScheduledFuture<?> tareaMonitor;
    private volatile boolean activo = false;
    private final List<CitaTecnica> cacheCitas = new ArrayList<>();

    private MonitorRecordatorios() {
    }

    public static MonitorRecordatorios getInstance() {
        return INSTANCIA;
    }

    public ReadOnlyIntegerProperty citasHoyPendientesProperty() {
        return citasHoyPendientes;
    }

    public ReadOnlyStringProperty mensajeAvisoProperty() {
        return mensajeAviso;
    }

    public ReadOnlyBooleanProperty tieneAlertasProperty() {
        return tieneAlertas;
    }

    public synchronized void actualizarCache(List<CitaTecnica> nuevasCitas) {
        cacheCitas.clear();
        if (nuevasCitas != null) {
            cacheCitas.addAll(nuevasCitas);
        }
        analizarEnSegundoPlano();
    }

    public synchronized void iniciar() {
        if (activo) return;
        activo = true;

        tareaMonitor = AppThreadPool.scheduleWithFixedDelay(this::analizarEnSegundoPlano, 3, 15, TimeUnit.SECONDS);
        System.out.println("[MonitorRecordatorios] Demonio de recordatorios iniciado (frecuencia: cada 15s).");
    }

    public synchronized void detener() {
        activo = false;
        if (tareaMonitor != null && !tareaMonitor.isDone()) {
            tareaMonitor.cancel(true);
        }
        System.out.println("[MonitorRecordatorios] Demonio de recordatorios detenido.");
    }

    private void analizarEnSegundoPlano() {
        final List<CitaTecnica> copia;
        synchronized (this) {
            copia = new ArrayList<>(cacheCitas);
        }

        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();

        int pendientesHoy = 0;
        int proximasDosHoras = 0;

        for (CitaTecnica cita : copia) {
            if (cita == null || cita.getFecha() == null || cita.getHora() == null) continue;

            if (cita.getFecha().equals(hoy)) {
                String est = cita.getEstado() != null ? cita.getEstado().trim().toLowerCase() : "";
                if ("pendiente".equals(est) || "en progreso".equals(est)) {
                    pendientesHoy++;

                    // Comprobar si está en las próximas 2 horas
                    long minutosDiferencia = java.time.Duration.between(ahora, cita.getHora()).toMinutes();
                    if (minutosDiferencia >= 0 && minutosDiferencia <= 120) {
                        proximasDosHoras++;
                    }
                }
            }
        }

        final int finalPendientes = pendientesHoy;
        final int finalProximas = proximasDosHoras;

        AsyncUIHelper.runOnFXThread(() -> {
            citasHoyPendientes.set(finalPendientes);
            tieneAlertas.set(finalPendientes > 0);

            if (finalProximas > 0) {
                mensajeAviso.set(String.format("🔔 ¡Atención! %d cita(s) programada(s) para las próximas 2 horas.", finalProximas));
            } else if (finalPendientes > 0) {
                mensajeAviso.set(String.format("📋 %d cita(s) técnica(s) pendiente(s) para hoy.", finalPendientes));
            } else {
                mensajeAviso.set("✅ Al día: No hay citas pendientes para hoy.");
            }
        });
    }
}
