package net.pokereport.luna.moderation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PunishmentHistoryTest {

    @Test
    @DisplayName("Formateo de tiempo restante legible")
    void testFormatearRestante() {
        long future = System.currentTimeMillis() + 65000; // ~1 min 5 seg
        String formatted = ModerationService.formatearRestante(future);
        assertNotNull(formatted);
        assertTrue(formatted.contains("m") || formatted.contains("s"), "Debe contener unidades de tiempo");
    }
}
