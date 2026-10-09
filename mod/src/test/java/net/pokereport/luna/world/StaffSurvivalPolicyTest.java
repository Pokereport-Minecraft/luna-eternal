package net.pokereport.luna.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StaffSurvivalPolicyTest {
    @Test void moderatorAdventureIsRestoredInAllSevenSurvivalWorlds() {
        assertTrue(StaffSurvivalPolicy.restore(true, true, "minecraft:overworld"));
        for (String suffix : new String[]{"", "2", "3", "4", "5", "6"})
            assertTrue(StaffSurvivalPolicy.restore(true, true, "lunaeternal:salvaje" + suffix));
    }
    @Test void protectedAndExternalWorldsKeepTheirMode() {
        for (String id : new String[]{"ciudadela", "lobby", "gimnasios", "gimnasio_morti", "torre", "islas_naranja", "salvaje7"})
            assertFalse(StaffSurvivalPolicy.restore(true, true, "lunaeternal:" + id));
        assertFalse(StaffSurvivalPolicy.restore(true, true, "minecraft:the_nether"));
        assertFalse(StaffSurvivalPolicy.restore(true, true, "otro:salvaje"));
    }
    @Test void noEscalationOrCreativeSpectatorChanges() {
        assertFalse(StaffSurvivalPolicy.restore(false, true, "minecraft:overworld"));
        assertFalse(StaffSurvivalPolicy.restore(true, false, "minecraft:overworld"));
        assertFalse(StaffSurvivalPolicy.restore(true, true, null));
    }
}
