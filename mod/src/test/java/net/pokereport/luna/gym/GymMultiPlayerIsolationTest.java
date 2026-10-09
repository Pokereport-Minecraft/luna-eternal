package net.pokereport.luna.gym;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.Vec3d;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GymMultiPlayerIsolationTest {

    @Test
    @DisplayName("Aislamiento espacial entre ranuras multijugador para todos los gimnasios")
    void testSpatialIsolationAcrossSlots() {
        for (Gimnasio.Gimnasio_ g : Gimnasio.TODOS) {
            int paso = Gimnasio.pasoRanura(g);
            for (int slotA = 1; slotA < Gimnasio.RANURAS; slotA++) {
                for (int slotB = slotA + 1; slotB < Gimnasio.RANURAS; slotB++) {
                    Vec3d entradaA = Gimnasio.entrada(g, slotA);
                    Vec3d entradaB = Gimnasio.entrada(g, slotB);

                    double distZ = Math.abs(entradaA.z - entradaB.z);
                    assertTrue(distZ >= paso - 0.001, "La distancia en Z entre ranura " + slotA + " y " + slotB +
                            " debe ser al menos de " + paso + " bloques para " + g.id());

                    Vec3d liderA = Gimnasio.lider(g, slotA);
                    Vec3d liderB = Gimnasio.lider(g, slotB);
                    double distLiderZ = Math.abs(liderA.z - liderB.z);
                    assertTrue(distLiderZ >= paso - 0.001, "La distancia entre líderes de ranura " + slotA + " y " + slotB +
                            " debe ser al menos de " + paso + " bloques");
                }
            }
        }
    }

    @Test
    @DisplayName("Aislamiento de estado e incidencias entre dos jugadores concurrentes")
    void testStateIsolationBetweenConcurrentPlayers() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();

        GymChallengeService.PlayerChallenge chA = new GymChallengeService.PlayerChallenge(
                playerA, "sabrina", 1, System.currentTimeMillis(), false, 0, 0, GymChallengeService.State.IN_PROGRESS
        );

        GymChallengeService.PlayerChallenge chB = new GymChallengeService.PlayerChallenge(
                playerB, "sabrina", 2, System.currentTimeMillis(), false, 0, 0, GymChallengeService.State.IN_PROGRESS
        );

        // Jugador A falla 3 veces en su ranura
        GymChallengeService.PlayerChallenge chAFail = chA.withFail().withFail().withFail();
        assertEquals(3, chAFail.failCount());
        assertFalse(chAFail.routeCompleted());

        // Jugador B nunca falló y completa la ruta
        GymChallengeService.PlayerChallenge chBComplete = chB.withRouteCompleted(true);
        assertEquals(0, chBComplete.failCount());
        assertTrue(chBComplete.routeCompleted());

        // Verificar que los estados no se contaminan
        assertNotEquals(chAFail.failCount(), chBComplete.failCount());
        assertNotEquals(chAFail.routeCompleted(), chBComplete.routeCompleted());
        assertNotEquals(chAFail.slot(), chBComplete.slot());
    }
}
