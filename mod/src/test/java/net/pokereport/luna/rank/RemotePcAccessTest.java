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
    @Test void existingStaffAndOperatorAccessIsPreserved() {
        for (Rank rank : Rank.values()) if (rank.equipo)
            assertTrue(RemotePcAccess.allows(rank, false), rank.name());
        assertTrue(RemotePcAccess.allows(Rank.ENTRENADOR, true));
    }
}
