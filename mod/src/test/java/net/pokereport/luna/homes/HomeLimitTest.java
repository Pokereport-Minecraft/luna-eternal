package net.pokereport.luna.homes;

import net.pokereport.luna.ui.Tablist.Rank;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HomeLimitTest {

    @Test
    public void testDefaultPlayerHasOneHome() {
        assertEquals(1, HomeService.limiteHomes(null, false));
    }

    @Test
    public void testRankedPlayersKeepLimits() {
        assertEquals(3, HomeService.limiteHomes(Rank.ELITE, false));
        assertEquals(4, HomeService.limiteHomes(Rank.CAMPEON, false));
        assertEquals(5, HomeService.limiteHomes(Rank.MAESTRO, false));
        assertEquals(7, HomeService.limiteHomes(Rank.LEYENDA, false));
    }

    @Test
    public void testStaffAndTeamHaveHighLimit() {
        assertEquals(100, HomeService.limiteHomes(null, true));
    }
}
