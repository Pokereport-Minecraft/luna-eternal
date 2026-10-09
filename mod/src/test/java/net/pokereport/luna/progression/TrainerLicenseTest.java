package net.pokereport.luna.progression;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TrainerLicenseTest {

    @Test
    @DisplayName("Jerarquía y progresión de TrainerLicense")
    void testProgresionLicencias() {
        assertEquals(TrainerLicense.BRONCE, TrainerLicense.NOVATO.next());
        assertEquals(TrainerLicense.PLATA, TrainerLicense.BRONCE.next());
        assertEquals(TrainerLicense.ORO, TrainerLicense.PLATA.next());
        assertEquals(TrainerLicense.MAESTRO, TrainerLicense.ORO.next());
        assertEquals(TrainerLicense.CAMPEON, TrainerLicense.MAESTRO.next());
        assertNull(TrainerLicense.CAMPEON.next(), "CAMPEON es el rango máximo y next() debe ser null");
    }

    @Test
    @DisplayName("Requisitos de ascenso estrictos por nivel de licencia")
    void testRequisitosPorNivel() {
        // Novato
        assertEquals(0, TrainerLicense.NOVATO.requiredBadges);
        assertEquals(0.0, TrainerLicense.NOVATO.requiredDexPct);
        assertEquals(0, TrainerLicense.NOVATO.requiredQuests);
        assertEquals(0L, TrainerLicense.NOVATO.upgradeFee);

        // Bronce (Liga inicial)
        assertEquals(2, TrainerLicense.BRONCE.requiredBadges);
        assertEquals(0.15, TrainerLicense.BRONCE.requiredDexPct);
        assertEquals(5, TrainerLicense.BRONCE.requiredQuests);
        assertEquals(5_000L, TrainerLicense.BRONCE.upgradeFee);

        // Plata (Ruta intermedia)
        assertEquals(4, TrainerLicense.PLATA.requiredBadges);
        assertEquals(0.30, TrainerLicense.PLATA.requiredDexPct);
        assertEquals(15, TrainerLicense.PLATA.requiredQuests);
        assertEquals(20_000L, TrainerLicense.PLATA.upgradeFee);

        // Oro (Veterano)
        assertEquals(6, TrainerLicense.ORO.requiredBadges);
        assertEquals(0.50, TrainerLicense.ORO.requiredDexPct);
        assertEquals(30, TrainerLicense.ORO.requiredQuests);
        assertEquals(50_000L, TrainerLicense.ORO.upgradeFee);

        // Maestro (Alto Mando / 8 Medallas)
        assertEquals(8, TrainerLicense.MAESTRO.requiredBadges);
        assertEquals(0.70, TrainerLicense.MAESTRO.requiredDexPct);
        assertEquals(50, TrainerLicense.MAESTRO.requiredQuests);
        assertEquals(150_000L, TrainerLicense.MAESTRO.upgradeFee);

        // Campeón (Liga Completa)
        assertEquals(12, TrainerLicense.CAMPEON.requiredBadges);
        assertEquals(0.85, TrainerLicense.CAMPEON.requiredDexPct);
        assertEquals(75, TrainerLicense.CAMPEON.requiredQuests);
        assertEquals(500_000L, TrainerLicense.CAMPEON.upgradeFee);
    }

    @Test
    @DisplayName("Parseo de nombres y valores por defecto")
    void testParseoLicencias() {
        assertEquals(TrainerLicense.NOVATO, TrainerLicense.de("novato"));
        assertEquals(TrainerLicense.BRONCE, TrainerLicense.de("Bronce"));
        assertEquals(TrainerLicense.PLATA, TrainerLicense.de("plata"));
        assertEquals(TrainerLicense.ORO, TrainerLicense.de("ORO"));
        assertEquals(TrainerLicense.MAESTRO, TrainerLicense.de("maestro"));
        assertEquals(TrainerLicense.CAMPEON, TrainerLicense.de("Campeón"));
        assertEquals(TrainerLicense.CAMPEON, TrainerLicense.de("campeon"));

        // Fallbacks seguros ante entradas nulas o inválidas
        assertEquals(TrainerLicense.NOVATO, TrainerLicense.de(null));
        assertEquals(TrainerLicense.NOVATO, TrainerLicense.de(""));
        assertEquals(TrainerLicense.NOVATO, TrainerLicense.de("invalido_xyz"));
    }
}
