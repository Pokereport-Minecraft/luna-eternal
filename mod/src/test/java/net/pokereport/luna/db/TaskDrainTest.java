package net.pokereport.luna.db;

import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class TaskDrainTest {
    @Test void todosLosGuardadosAceptadosTerminanAntesDelCierre() {
        var saves = Executors.newVirtualThreadPerTaskExecutor();
        var writes = new AtomicInteger();
        for (int i = 0; i < 50; i++) saves.execute(writes::incrementAndGet);
        assertTrue(TaskDrain.finish(saves, 2, TimeUnit.SECONDS));
        assertEquals(50, writes.get());
        assertThrows(RejectedExecutionException.class, () -> saves.execute(writes::incrementAndGet));
    }
    @Test void esperaUnGuardadoEnCursoSinCerrarAntes() throws Exception {
        var saves = Executors.newVirtualThreadPerTaskExecutor();
        var started = new CountDownLatch(1);var release = new CountDownLatch(1);
        saves.execute(() -> { started.countDown(); try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
        assertTrue(started.await(2, TimeUnit.SECONDS));
        var waiting = CompletableFuture.supplyAsync(() -> TaskDrain.finish(saves, 2, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> waiting.get(20, TimeUnit.MILLISECONDS));
        release.countDown();assertTrue(waiting.get(2, TimeUnit.SECONDS));
    }
    @Test void unaTareaBloqueadaNoImpideApagarIndefinidamente() throws Exception {
        var saves = Executors.newVirtualThreadPerTaskExecutor();var started = new CountDownLatch(1);
        saves.execute(() -> { started.countDown(); try { new CountDownLatch(1).await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertFalse(TaskDrain.finish(saves, 10, TimeUnit.MILLISECONDS));
        assertTrue(saves.awaitTermination(2, TimeUnit.SECONDS));
    }
}
