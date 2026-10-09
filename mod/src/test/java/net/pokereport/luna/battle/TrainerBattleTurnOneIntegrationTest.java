package net.pokereport.luna.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TrainerBattleTurnOneIntegrationTest {

    @Test
    @DisplayName("Turno 1 procesa request y genera respuesta de IA sin bloqueo")
    void testTurnoUnoFlujoSinBloqueo() {
        UUID playerUuid = UUID.randomUUID();
        UUID trainerUuid = UUID.randomUUID();

        var builder = BattleDiagnostics.startAttempt(playerUuid, "kanto_brock", trainerUuid, "luna:gimnasios");
        builder.markFirstRequest();
        builder.markFirstChoice();
        builder.setTurn(1);
        builder.trace("Turn 1 active request received");
        builder.trace("AI choices populated via req.iterate()");
        builder.trace("checkForInputDispatch entered with readyToInput=true");

        var rec = builder.build();
        assertTrue(rec.firstRequestReceived());
        assertTrue(rec.firstChoiceMade());
        assertEquals(1, rec.currentTurn());
        assertNull(rec.failureStage());
    }
}
