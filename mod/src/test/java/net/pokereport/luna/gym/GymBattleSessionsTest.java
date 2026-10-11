package net.pokereport.luna.gym;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GymBattleSessionsTest {
    @Test void onlyTheRegisteredBattleCanCompleteOnce() {
        var sessions = new GymBattleSessions();
        var player = UUID.randomUUID(); var battle = UUID.randomUUID();
        sessions.start(player, battle);
        assertFalse(sessions.finish(player, UUID.randomUUID()));
        assertTrue(sessions.finish(player, battle));
        assertFalse(sessions.finish(player, battle));
    }
    @Test void lateResultCannotCompleteANewChallenge() {
        var sessions = new GymBattleSessions();
        var player = UUID.randomUUID(); var old = UUID.randomUUID(); var next = UUID.randomUUID();
        sessions.start(player, old); sessions.remove(player); sessions.start(player, next);
        assertFalse(sessions.finish(player, old));
        assertTrue(sessions.finish(player, next));
    }
    @Test void concurrentChallengersHaveIndependentOwnership() {
        var sessions = new GymBattleSessions();
        var a = UUID.randomUUID(); var b = UUID.randomUUID();
        var first = UUID.randomUUID(); var second = UUID.randomUUID();
        sessions.start(a, first); sessions.start(b, second);
        assertThrows(IllegalStateException.class, () -> sessions.start(a, second));
        assertFalse(sessions.finish(a, second));
        assertTrue(sessions.finish(b, second)); assertTrue(sessions.finish(a, first));
    }
}
