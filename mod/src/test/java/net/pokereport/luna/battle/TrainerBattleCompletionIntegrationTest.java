package net.pokereport.luna.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TrainerBattleCompletionIntegrationTest {

    @Test
    @DisplayName("Cierre exitoso de combate archiva registro y limpia estado activo")
    void testCierreExitosoArchivado() {
        UUID playerUuid = UUID.randomUUID();
        UUID trainerUuid = UUID.randomUUID();

        var builder = BattleDiagnostics.startAttempt(playerUuid, "kanto_misty", trainerUuid, "luna:gimnasios");
        String id = builder.getBattleAttemptId();

        BattleDiagnostics.recordTurn(id, 4);
        BattleDiagnostics.recordSuccess(id);

        assertNull(BattleDiagnostics.getActive(id));
        assertNull(BattleDiagnostics.getActiveForPlayer(playerUuid));

        var history = BattleDiagnostics.getRecentHistory();
        assertTrue(history.stream().anyMatch(h -> h.battleAttemptId().equals(id) && h.completedSuccessfully()));
    }
}
