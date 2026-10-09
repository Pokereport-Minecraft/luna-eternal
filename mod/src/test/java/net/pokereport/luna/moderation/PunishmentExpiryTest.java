package net.pokereport.luna.moderation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PunishmentExpiryTest {

    @Test
    @DisplayName("Cálculo y detección de expiración de sanciones temporales")
    void testExpiracionTemporal() {
        UUID target = UUID.randomUUID();
        UUID staff = UUID.randomUUID();
        long now = System.currentTimeMillis();

        // Castigo que ya expiró en el pasado
        ModerationService.Punishment expirado = new ModerationService.Punishment(
                2L, target, "ExpiradoPlayer", staff, "ModAdmin",
                ModerationService.Type.TEMPMUTE, "Spam", now - 10000, now - 1000, true
        );
        assertTrue(expirado.isExpired(), "El castigo con fecha de expiración pasada debe retornar true");

        // Castigo que vence en el futuro
        ModerationService.Punishment activo = new ModerationService.Punishment(
                3L, target, "ActivoPlayer", staff, "ModAdmin",
                ModerationService.Type.TEMPMUTE, "Spam", now, now + 60000, true
        );
        assertFalse(activo.isExpired(), "El castigo con fecha futura no debe expirar todavía");
    }

    @Test
    @DisplayName("Parseo de duraciones en formatos estándar")
    void testParseoDuraciones() {
        assertEquals(30 * 1000L, ModerationService.parsearDuracion("30s"));
        assertEquals(15 * 60 * 1000L, ModerationService.parsearDuracion("15m"));
        assertEquals(2 * 3600 * 1000L, ModerationService.parsearDuracion("2h"));
        assertEquals(3 * 86400 * 1000L, ModerationService.parsearDuracion("3d"));
        assertEquals(7 * 86400 * 1000L, ModerationService.parsearDuracion("1w"));
        assertEquals(0L, ModerationService.parsearDuracion("invalido"));
        assertEquals(0L, ModerationService.parsearDuracion(""));
        assertEquals(0L, ModerationService.parsearDuracion(null));
    }
}
