package net.pokereport.luna.battle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleQueueWatchdogTest {
    @Test void legitimatePlayerThinkingNeverTimesOut() {
        var guard = new BattleQueueWatchdog();
        Object barrier = new Object(), head = new Object();
        for (int i = 0; i < 12_000; i++)
            assertEquals(BattleQueueWatchdog.Result.OK, guard.observe(barrier, head, 3, true, true));
    }

    @Test void continuousQueueProgressNeverCountsAsDeadlock() {
        var guard = new BattleQueueWatchdog();
        Object barrier = new Object();
        for (int i = 0; i < 2400; i++)
            assertEquals(BattleQueueWatchdog.Result.OK, guard.observe(barrier, new Object(), 3, true, false));
    }

    @Test void unchangedQueueWarnsThenAbortsWithoutForcingAnAction() {
        var guard = new BattleQueueWatchdog();
        Object barrier = new Object(), head = new Object();
        guard.observe(barrier, head, 3, true, false);
        int warnings = 0;
        for (int i = 1; i < 1200; i++) {
            var result = guard.observe(barrier, head, 3, true, false);
            assertNotEquals(BattleQueueWatchdog.Result.ABORT, result);
            if (result == BattleQueueWatchdog.Result.WARN) warnings++;
        }
        assertEquals(2, warnings);
        assertEquals(BattleQueueWatchdog.Result.ABORT, guard.observe(barrier, head, 3, true, false));
    }

    @Test void NewTurnAndReleasedBarrierResetStallClock() {
        var guard = new BattleQueueWatchdog();
        Object barrier = new Object(), head = new Object();
        for (int turn = 1; turn <= 5; turn++) {
            for (int i = 0; i < 1100; i++)
                assertNotEquals(BattleQueueWatchdog.Result.ABORT, guard.observe(barrier, head, turn, true, false));
            assertEquals(BattleQueueWatchdog.Result.OK, guard.observe(barrier, head, turn, false, false));
        }
    }
}
