package com.tecnoasistencia.concurrencia;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gestor centralizado de grupos de hilos (Thread Pools) para la aplicación.
 * Proporciona hilos demonio (daemon threads) para tareas asíncronas y tareas periódicas
 * garantizando que la JVM pueda cerrarse limpiamente sin hilos huérfanos.
 */
public final class AppThreadPool {

    private static final int CORES = Runtime.getRuntime().availableProcessors();
    private static final int POOL_SIZE = Math.max(2, Math.min(CORES * 2, 8));

    private static final AtomicInteger workerCount = new AtomicInteger(1);
    private static final AtomicInteger cronCount = new AtomicInteger(1);

    private static final ThreadFactory workerFactory = runnable -> {
        Thread thread = new Thread(runnable, "citas-worker-" + workerCount.getAndIncrement());
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    };

    private static final ThreadFactory cronFactory = runnable -> {
        Thread thread = new Thread(runnable, "citas-cron-" + cronCount.getAndIncrement());
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    };

    private static final ExecutorService workerPool = Executors.newFixedThreadPool(POOL_SIZE, workerFactory);
    private static final ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(2, cronFactory);

    private AppThreadPool() {
        // Clase utilitaria estática
    }

    public static ExecutorService getExecutor() {
        return workerPool;
    }

    public static ScheduledExecutorService getScheduledExecutor() {
        return scheduledPool;
    }

    public static void execute(Runnable command) {
        workerPool.execute(command);
    }

    public static Future<?> submit(Runnable task) {
        return workerPool.submit(task);
    }

    public static <T> Future<T> submit(Callable<T> task) {
        return workerPool.submit(task);
    }

    public static ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return scheduledPool.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    /**
     * Cierra de forma ordenada los grupos de hilos de la aplicación.
     * Espera un tiempo prudencial para que las tareas en curso finalicen.
     */
    public static void shutdown() {
        System.out.println("[AppThreadPool] Iniciando apagado ordenado de grupos de hilos...");
        scheduledPool.shutdown();
        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(2, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
            if (!scheduledPool.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduledPool.shutdownNow();
            }
            System.out.println("[AppThreadPool] Grupos de hilos detenidos exitosamente.");
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            scheduledPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
