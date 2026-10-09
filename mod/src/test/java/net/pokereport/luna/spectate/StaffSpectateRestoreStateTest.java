package net.pokereport.luna.spectate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StaffSpectateRestoreStateTest {

    @Test
    @DisplayName("isAuthorized devuelve false para entidades nulas")
    void testIsAuthorizedNull() {
        assertFalse(StaffSpectateService.isAuthorized(null));
    }
}
