package net.pokereport.luna.progression;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProgressionRegressionTest {

    @Test
    @DisplayName("Invariantes del sistema de progresión y licencias de entrenador")
    void testInvariantesProgresion() {
        for (TrainerLicense license : TrainerLicense.values()) {
            assertNotNull(license.id);
            assertNotNull(license.formatted);
            assertTrue(license.requiredBadges >= 0);
            assertTrue(license.requiredDexPct >= 0.0 && license.requiredDexPct <= 1.0);
            assertTrue(license.requiredQuests >= 0);
            assertTrue(license.upgradeFee >= 0L);
        }
    }

    @Test
    @DisplayName("Licencias iniciales y finales tienen requisitos crecientes")
    void testRequisitosCrecientesLicencias() {
        assertEquals(0, TrainerLicense.NOVATO.requiredBadges, "Novato requiere 0 medallas");
        assertEquals(12, TrainerLicense.CAMPEON.requiredBadges, "Campeón requiere 12 medallas");
        assertTrue(TrainerLicense.MAESTRO.requiredBadges < TrainerLicense.CAMPEON.requiredBadges);
        assertTrue(TrainerLicense.NOVATO.upgradeFee == 0L);
        assertTrue(TrainerLicense.CAMPEON.upgradeFee > TrainerLicense.MAESTRO.upgradeFee);
    }
}
