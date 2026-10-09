package net.pokereport.luna.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TrainerBattleFailureRecoveryTest {

    @Test
    @DisplayName("Fallo abrupto de combate registra causa raíz y limpia sesión del jugador")
    void testFalloAbruptoRegistro() {
        UUID playerUuid = UUID.randomUUID();
        UUID trainerUuid = UUID.randomUUID();

        var builder = BattleDiagnostics.startAttempt(playerUuid, "kanto_surge", trainerUuid, "luna:gimnasios");
        String id = builder.getBattleAttemptId();

        BattleDiagnostics.recordFailure(id, "TURN_DISPATCH", "Showdown interpreter stall / timeout", new RuntimeException("Test forced stall"));

        assertNull(BattleDiagnostics.getActive(id));
        assertNull(BattleDiagnostics.getActiveForPlayer(playerUuid));

        var history = BattleDiagnostics.getRecentHistory();
        var failed = history.stream().filter(h -> h.battleAttemptId().equals(id)).findFirst().orElse(null);
        assertNotNull(failed);
        assertFalse(failed.completedSuccessfully());
        assertEquals("TURN_DISPATCH", failed.failureStage());
        assertTrue(failed.rootCause().contains("Test forced stall"));
    }
}
