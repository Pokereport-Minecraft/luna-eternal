package net.pokereport.luna.spectate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class StaffSpectateStealthTest {

    @Test
    @DisplayName("Estado de staff mode por defecto es inactivo")
    void testStaffModeDefault() {
        UUID uuid = UUID.randomUUID();
        assertFalse(StaffSpectateService.isStaffMode(uuid), "Por defecto el jugador no debe estar en staff mode");
    }
}
