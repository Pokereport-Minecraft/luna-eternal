package net.pokereport.luna.gym;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GymDifficultyPolicyTest {

    @Test
    @DisplayName("Nivel máximo inicial sin medallas es 15 (Brock)")
    void testInitialLevelCapWithoutBadges() {
        UUID testPlayer = UUID.randomUUID();
        // Con 0 medallas registradas, el tope debe ser exactamente el nivel del primer líder (15)
        int cap = GymLevelCapService.nivelMaximo(testPlayer);
        assertEquals(15, cap, "El nivel máximo inicial para un jugador novato debe ser 15");
    }

    @Test
    @DisplayName("Regiones y fronteras estrictas Gen 1 y Gen 2")
    void testRegionBoundariesGen1AndGen2() {
        List<Gimnasio.Gimnasio_> gyms = Gimnasio.TODOS;
        assertNotNull(gyms);
        assertEquals(23, gyms.size(), "Deben existir exactamente 23 gimnasios/retos registrados");

        long kantoCount = gyms.stream().filter(g -> g.region() == Gimnasio.Region.KANTO).count();
        long naranjaCount = gyms.stream().filter(g -> g.region() == Gimnasio.Region.NARANJA).count();
        long johtoCount = gyms.stream().filter(g -> g.region() == Gimnasio.Region.JOHTO).count();

        assertEquals(9, kantoCount, "Kanto debe tener 8 líderes + 1 Campeón");
        assertEquals(5, naranjaCount, "Liga Naranja debe tener 4 líderes + 1 Campeón");
        assertEquals(9, johtoCount, "Johto debe tener 8 líderes + 1 Campeón");
    }

    @Test
    @DisplayName("Escalado de pericia de IA y niveles mínimos competitivos")
    void testGymLeaderDifficultyAndAiExpertise() {
        for (Gimnasio.Gimnasio_ g : Gimnasio.TODOS) {
            assertTrue(g.pericia() >= 3, "La IA del líder " + g.lider() + " debe tener pericia competitiva >= 3");
            assertTrue(g.nivel() >= 15, "El nivel mínimo de gimnasio debe ser >= 15");
            assertTrue(g.nivel() <= 100, "El nivel máximo no puede superar 100");
        }
    }

    @Test
    @DisplayName("Invariante de unicidad de bits de medallas")
    void testBadgeBitUniqueness() {
        boolean[] bitsUsados = new boolean[64];
        for (Gimnasio.Gimnasio_ g : Gimnasio.TODOS) {
            assertFalse(bitsUsados[g.bit()], "Bit duplicado de medalla detectado: bit " + g.bit() + " en " + g.lider());
            bitsUsados[g.bit()] = true;
        }
    }
}
