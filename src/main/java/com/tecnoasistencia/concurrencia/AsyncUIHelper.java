package com.tecnoasistencia.concurrencia;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;

/**
 * Utilitario para coordinar de forma segura la interacción entre hilos de trabajo
 * y el hilo de interfaz gráfica de usuario de JavaFX (JavaFX Application Thread).
 */
public final class AsyncUIHelper {

    private AsyncUIHelper() {
        // Utilitario estático
    }

    /**
     * Ejecuta una acción en el hilo de JavaFX. Si la llamada ya se encuentra en dicho hilo,
     * la ejecuta inmediatamente; de lo contrario, la delega a Platform.runLater().
     *
     * @param action Acción a ejecutar.
     */
    public static void runOnFXThread(Runnable action) {
        if (action == null) return;
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    /**
     * Vincula el ciclo de vida de un Task con un indicador de progreso visual y una lista de controles
     * que deben deshabilitarse durante la ejecución para evitar condiciones de carrera o clics duplicados.
     *
     * @param task      Tarea en segundo plano.
     * @param indicator Spinner o barra de progreso (puede ser null).
     * @param controls  Controles a deshabilitar mientras la tarea se encuentre activa.
     */
    public static void vincularTareaAControles(Task<?> task, ProgressIndicator indicator, Node... controls) {
        if (task == null) return;

        if (indicator != null) {
            indicator.visibleProperty().bind(task.runningProperty());
            indicator.managedProperty().bind(task.runningProperty());
        }

        if (controls != null) {
            for (Node control : controls) {
                if (control != null) {
                    control.disableProperty().bind(task.runningProperty());
                }
            }
        }
    }

    /**
     * Desvincula propiedades de un control una vez finalizada la tarea para restablecer control manual.
     */
    public static void desvincularControles(ProgressIndicator indicator, Node... controls) {
        if (indicator != null) {
            indicator.visibleProperty().unbind();
            indicator.managedProperty().unbind();
            indicator.setVisible(false);
            indicator.setManaged(false);
        }
        if (controls != null) {
            for (Node control : controls) {
                if (control != null) {
                    control.disableProperty().unbind();
                }
            }
        }
    }
}
