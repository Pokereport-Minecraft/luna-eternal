package net.pokereport.luna.spectate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class StaffSpectateCrossDimensionTest {

    @Test
    @DisplayName("Gestión de mapa de sesiones activas es concurrente e inmutable")
    void testMapaSesionesInmutable() {
        var map = StaffSpectateService.getActiveSessions();
        assertNotNull(map);
        assertThrows(UnsupportedOperationException.class, () -> map.put(UUID.randomUUID(), null));
    }
}
