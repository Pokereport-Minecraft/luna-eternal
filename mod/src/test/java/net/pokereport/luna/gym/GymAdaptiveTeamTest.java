package net.pokereport.luna.gym;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GymAdaptiveTeamTest {

    @Test
    @DisplayName("Gimnasios tienen líderes y niveles bien definidos")
    void testGimnasiosDefinidos() {
        for (var gym : Gimnasio.TODOS) {
            assertNotNull(gym.id(), "El ID del gimnasio no debe ser nulo");
            assertNotNull(gym.lider(), "El líder del gimnasio no debe ser nulo");
            assertTrue(gym.nivel() > 0 && gym.nivel() <= 100, "Nivel de líder debe estar entre 1 y 100");
            assertTrue(gym.pericia() >= 0, "Pericia no puede ser negativa");
        }
    }

    @Test
    @DisplayName("Brock existe como benchmark con nivel 14")
    void testBrockBenchmark() {
        var brock = Gimnasio.de("brock");
        assertNotNull(brock, "Brock debe existir en Gimnasio.TODOS");
        assertEquals("brock", brock.id());
        assertEquals(15, brock.nivel(), "El nivel benchmark de Brock es 15");
        assertTrue(Gimnasio.equipoMinimo(brock) >= 1, "Mínimo 1 Pokémon");
    }

    @Test
    @DisplayName("Repertorio canónico de líderes contiene firmas")
    void testRepertorioLideres() {
        var repertorioBrock = Repertorio.de("brock");
        assertFalse(repertorioBrock.isEmpty(), "Brock debe tener repertorio canónico");
        assertTrue(repertorioBrock.stream().anyMatch(Repertorio.Ficha::firma), "Brock debe tener al menos un Pokémon firma");
    }
}
