package net.pokereport.luna.gym;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class TrainerAdaptiveFallbackTest {

    @Test
    @DisplayName("Equipo vacío o inválido activa fallback e incrementa métrica de fallback")
    void testFallbackPorEquipoInvalido() {
        var misty = Gimnasio.de("misty");
        assertNotNull(misty);

        long countAntes = GymTeamValidator.getFallbackCount();

        // Validar equipo vacío
        var resVacio = GymTeamValidator.validar(Collections.emptyList(), misty);
        assertFalse(resVacio.valid());
        assertTrue(GymTeamValidator.getFallbackCount() > countAntes);

        long countDespues = GymTeamValidator.getFallbackCount();
        // Validar equipo nulo
        var resNulo = GymTeamValidator.validar(null, misty);
        assertFalse(resNulo.valid());
        assertEquals(countDespues + 1, GymTeamValidator.getFallbackCount());
    }
}
