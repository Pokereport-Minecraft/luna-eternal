package net.pokereport.luna.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EditModeAuthorizationTest {
    @Test void survivalAndCreativeUseTheSameExplicitAuthorization() {
        assertTrue(WorldProtectionManager.permitsEditing(true, true, false));
        assertFalse(WorldProtectionManager.permitsEditing(false, true, false));
        assertFalse(WorldProtectionManager.permitsEditing(true, false, false));
        assertFalse(WorldProtectionManager.permitsEditing(true, true, true));
    }
}
