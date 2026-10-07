package net.pokereport.luna.world;
import net.pokereport.luna.ui.Tablist.Rank;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RestrictedDimensionsTest {
    @Test void blocksBothDimensionsForEveryCommercialRankAndBareOperator() {
        for (String dimension : new String[]{"minecraft:the_nether", "minecraft:the_end"}) {
            assertTrue(RestrictedDimensions.denied(dimension, true, null));
            for (Rank rank : Rank.values()) {
                assertTrue(RestrictedDimensions.denied(dimension, false, rank));
                assertEquals(!rank.esStaff(), RestrictedDimensions.denied(dimension, true, rank));
            }
        }
    }
    @Test void preservesAllOtherDestinations() {
        for (String dimension : new String[]{"minecraft:overworld", "lunaeternal:lobby",
                "lunaeternal:ciudadela", "lunaeternal:salvaje", "lunaeternal:gimnasios"})
            assertFalse(RestrictedDimensions.denied(dimension, false, null));
    }
}
