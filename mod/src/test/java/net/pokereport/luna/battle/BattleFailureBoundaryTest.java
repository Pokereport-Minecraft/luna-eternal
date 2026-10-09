package net.pokereport.luna.battle;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleFailureBoundaryTest {
    private static final class BrokenSingleton {
        static final Object INSTANCE = fail();
        private static Object fail() { throw new IllegalStateException("failed singleton initialization"); }
    }

    @Test void firstAndRepeatedInitializationFailureDoNotEscapeTick() {
        AtomicReference<Throwable> caught = new AtomicReference<>();
        BattleFailureBoundary.run(() -> { Object ignored = BrokenSingleton.INSTANCE; }, caught::set);
        assertInstanceOf(ExceptionInInitializerError.class, caught.get());
        BattleFailureBoundary.run(() -> { Object ignored = BrokenSingleton.INSTANCE; }, caught::set);
        assertInstanceOf(NoClassDefFoundError.class, caught.get());
        AtomicReference<Boolean> nextBattle = new AtomicReference<>(false);
        BattleFailureBoundary.run(() -> nextBattle.set(true), caught::set);
        assertTrue(nextBattle.get());
    }

    @Test void vmMemoryFailureIsNotSilenced() {
        assertThrows(OutOfMemoryError.class, () -> BattleFailureBoundary.run(
                () -> { throw new OutOfMemoryError("test"); }, error -> fail("fatal error swallowed")));
    }
}
