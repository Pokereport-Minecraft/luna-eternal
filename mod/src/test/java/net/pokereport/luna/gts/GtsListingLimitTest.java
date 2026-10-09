package net.pokereport.luna.gts;

import net.pokereport.luna.progression.ProgressionPolicyService;
import net.pokereport.luna.ui.Tablist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GtsListingLimitTest {

    @Test
    @DisplayName("Límites de publicaciones activas simultáneas en GTS por rango")
    void testGtsListingLimitsPorRango() {
        // F2P (sin rango o rango Entrenador base)
        assertEquals(3, ProgressionPolicyService.maxGtsListingsForRank(null));
        assertEquals(3, ProgressionPolicyService.maxGtsListingsForRank(Tablist.Rank.ENTRENADOR));

        // Rango Élite
        assertEquals(5, ProgressionPolicyService.maxGtsListingsForRank(Tablist.Rank.ELITE));

        // Rango Campeón
        assertEquals(8, ProgressionPolicyService.maxGtsListingsForRank(Tablist.Rank.CAMPEON));

        // Rango Maestro
        assertEquals(12, ProgressionPolicyService.maxGtsListingsForRank(Tablist.Rank.MAESTRO));

        // Rango Leyenda
        assertEquals(20, ProgressionPolicyService.maxGtsListingsForRank(Tablist.Rank.LEYENDA));
    }
}
