package net.pokereport.luna.gym;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GymTeamPersistenceTest {

    @Test
    @DisplayName("Gestión del ciclo de vida del equipo activo en batalla")
    void testCicloDeVidaEquipoActivo() {
        UUID jugador = UUID.randomUUID();

        // Sin combate activo
        assertTrue(GymAdaptiveTeamService.getEquipoActivo(jugador).isEmpty(),
                "Inicialmente no debe haber equipo activo para el jugador");

        // Limpieza de combate es idempotente
        assertDoesNotThrow(() -> GymAdaptiveTeamService.limpiarCombate(jugador));
        assertTrue(GymAdaptiveTeamService.getEquipoActivo(jugador).isEmpty());
    }
}
