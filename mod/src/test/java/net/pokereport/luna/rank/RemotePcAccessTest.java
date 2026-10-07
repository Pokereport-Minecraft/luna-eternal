package net.pokereport.luna.rank;
import net.pokereport.luna.ui.Tablist.Rank;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RemotePcAccessTest {
    @Test void ordinaryPlayersNeedChampion() {
        assertFalse(RemotePcAccess.allows(null, false));
        assertFalse(RemotePcAccess.allows(Rank.ENTRENADOR, false));
        assertFalse(RemotePcAccess.allows(Rank.ELITE, false));
        for (Rank rank : new Rank[]{Rank.CAMPEON, Rank.MAESTRO, Rank.LEYENDA})
            assertTrue(RemotePcAccess.allows(rank, false), rank.name());
    }
    @Test void staffAndOperatorDoNotBypassCommercialRank() {
        for (Rank rank : Rank.values()) {
            boolean eligible = rank == Rank.CAMPEON || rank == Rank.MAESTRO || rank == Rank.LEYENDA;
            assertEquals(eligible, RemotePcAccess.allows(rank, true), rank.name());
            assertEquals(eligible, RemotePcAccess.allows(rank, false), rank.name());
        }
        assertFalse(RemotePcAccess.allows(null, true));
    }
}
