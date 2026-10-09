package net.pokereport.luna.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TrainerConcurrentBattleTest {

    @Test
    @DisplayName("Múltiples combates concurrentes mantienen aislamiento de attemptId y estados")
    void testCombatesConcurrentesAislados() {
        int count = 10;
        List<String> ids = new ArrayList<>();
        List<UUID> players = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            UUID p = UUID.randomUUID();
            players.add(p);
            var b = BattleDiagnostics.startAttempt(p, "kanto_brock", UUID.randomUUID(), "luna:gimnasios");
            ids.add(b.getBattleAttemptId());
        }

        assertEquals(count, ids.stream().distinct().count(), "Todos los IDs de intento deben ser únicos");

        for (int i = 0; i < count; i++) {
            String id = ids.get(i);
            UUID p = players.get(i);
            assertEquals(id, BattleDiagnostics.getActiveForPlayer(p).getBattleAttemptId());
            BattleDiagnostics.recordTurn(id, i + 1);
            BattleDiagnostics.recordSuccess(id);
        }

        for (UUID p : players) {
            assertNull(BattleDiagnostics.getActiveForPlayer(p));
        }
    }
}
