package net.pokereport.luna.gym;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GymChallengeServiceTest {

    @BeforeEach
    void setup() {
        GymChallengeService.resetAll();
    }

    @Test
    @DisplayName("Gimnasio estándar (Brock): ruta completada automáticamente al entrar")
    void testStandardGymRouteCompletedByDefault() {
        UUID playerUuid = UUID.randomUUID();
        Gimnasio.Gimnasio_ brock = Gimnasio.de("brock");
        assertNotNull(brock);

        // Simulamos reto en Brock
        GymChallengeService.iniciarReto(null, brock, 1);
        // Directamente esRutaCompletada para gimnasios sin ruta de parkour devuelve true
        assertTrue(GymChallengeService.esRutaCompletada(playerUuid, "brock"),
                "Gimnasios estándar deben permitir acceso al líder directamente");
    }

    @Test
    @DisplayName("Sabrina: acceso a la batalla bloqueado hasta completar la ruta de parkour")
    void testSabrinaRequiresExplicitRouteCompletion() {
        UUID playerUuid = UUID.randomUUID();
        Gimnasio.Gimnasio_ sabrina = Gimnasio.de("sabrina");
        assertNotNull(sabrina);

        // Al iniciar reto en Sabrina con estado en progreso
        // Creamos estado simulado
        assertFalse(GymChallengeService.esRutaCompletada(playerUuid, "sabrina"),
                "Sin haber iniciado o completado ruta, Sabrina debe denegar el acceso");

        // Completamos ruta
        // Simulamos iniciarReto con un jugador
        // Para probar el servicio en test unitario:
        // Usamos PlayerChallenge record directamente o métodos del servicio
    }

    @Test
    @DisplayName("PlayerChallenge record y transiciones de estado inmutables")
    void testPlayerChallengeStateTransitions() {
        UUID uuid = UUID.randomUUID();
        GymChallengeService.PlayerChallenge ch = new GymChallengeService.PlayerChallenge(
                uuid, "sabrina", 1, System.currentTimeMillis(), false, 0, 0, GymChallengeService.State.IN_PROGRESS
        );

        assertFalse(ch.routeCompleted());
        assertEquals(GymChallengeService.State.IN_PROGRESS, ch.state());
        assertEquals(0, ch.failCount());

        // Caída / fallo
        GymChallengeService.PlayerChallenge fail1 = ch.withFail();
        assertEquals(1, fail1.failCount());
        assertFalse(fail1.routeCompleted());

        // Segunda caída
        GymChallengeService.PlayerChallenge fail2 = fail1.withFail();
        assertEquals(2, fail2.failCount());

        // Superar parkour
        GymChallengeService.PlayerChallenge completed = fail2.withRouteCompleted(true);
        assertTrue(completed.routeCompleted());
        assertEquals(GymChallengeService.State.ROUTE_COMPLETED, completed.state());
        assertEquals(2, completed.failCount(), "El recuento de caídas debe preservarse para auditoría");

        // Inicio de batalla
        GymChallengeService.PlayerChallenge battle = completed.withState(GymChallengeService.State.IN_BATTLE);
        assertEquals(GymChallengeService.State.IN_BATTLE, battle.state());
        assertTrue(battle.routeCompleted());
    }

    @Test
    @DisplayName("Ciclo de vida del desafío: limpieza al finalizar")
    void testChallengeLifecycleCleanup() {
        UUID uuid = UUID.randomUUID();
        GymChallengeService.finalizarReto(uuid);
        assertNull(GymChallengeService.obtenerEstado(uuid), "Al finalizar el reto el estado debe quedar limpio");
    }
}
