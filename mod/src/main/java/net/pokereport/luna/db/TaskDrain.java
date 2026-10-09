package net.pokereport.luna.db;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/** Stop accepting work and wait for already accepted SQL tasks before closing their pool. */
public final class TaskDrain {
    private TaskDrain() {}

    public static boolean finish(ExecutorService executor, long timeout, TimeUnit unit) {
        executor.shutdown();
        try {
            if (executor.awaitTermination(timeout, unit)) return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdownNow();
        return false;
    }
}
