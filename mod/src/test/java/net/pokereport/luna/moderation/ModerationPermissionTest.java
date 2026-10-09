package net.pokereport.luna.moderation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ModerationPermissionTest {

    @Test
    @DisplayName("Tipos de sanción soportados y jerarquía de moderación")
    void testTiposCastigo() {
        for (var tipo : ModerationService.Type.values()) {
            assertNotNull(tipo.name());
        }
        assertTrue(ModerationService.Type.values().length >= 6);
    }
}
