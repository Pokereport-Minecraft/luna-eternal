package net.pokereport.luna.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TrainerBattleStartIntegrationTest {

    @Test
    @DisplayName("Inicio de combate registra attemptId y traza pasos iniciales en BattleDiagnostics")
    void testInicioCombateDiagnostico() {
        UUID playerUuid = UUID.randomUUID();
        UUID trainerUuid = UUID.randomUUID();

        var builder = BattleDiagnostics.startAttempt(playerUuid, "kanto_brock", trainerUuid, "luna:gimnasios");
        assertNotNull(builder);
        assertNotNull(builder.getBattleAttemptId());
        assertTrue(builder.getBattleAttemptId().startsWith("btl-"));

        builder.trainerProfile("Brock")
                .teamSource("ADAPTIVE")
                .adaptiveEnabled(true)
                .addPokemon("geodude")
                .addPokemon("onix")
                .markActorCreated()
                .markBattleRegistered()
                .markShowdownStarted();

        var record = builder.build();
        assertEquals("kanto_brock", record.trainerId());
        assertEquals("Brock", record.trainerProfile());
        assertEquals("ADAPTIVE", record.teamSource());
        assertTrue(record.adaptiveEnabled());
        assertEquals(2, record.generatedTeam().size());
        assertTrue(record.battleActorCreated());
        assertTrue(record.battleRegistered());
        assertTrue(record.showdownStarted());
        assertFalse(record.completedSuccessfully());
    }
}
