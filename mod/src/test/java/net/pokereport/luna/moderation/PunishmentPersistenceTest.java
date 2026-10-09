package net.pokereport.luna.moderation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PunishmentPersistenceTest {

    @Test
    @DisplayName("Invariantes de objeto Punishment")
    void testPunishmentInvariants() {
        UUID target = UUID.randomUUID();
        UUID staff = UUID.randomUUID();
        long now = System.currentTimeMillis();

        ModerationService.Punishment p = new ModerationService.Punishment(
                1L, target, "TrollPlayer", staff, "ModAdmin",
                ModerationService.Type.WARN, "Griefing menor", now, null, true
        );

        assertEquals(1L, p.id());
        assertEquals(target, p.targetUuid());
        assertEquals("TrollPlayer", p.targetName());
        assertEquals(staff, p.staffUuid());
        assertEquals("ModAdmin", p.staffName());
        assertEquals(ModerationService.Type.WARN, p.type());
        assertEquals("Griefing menor", p.reason());
        assertTrue(p.active());
        assertFalse(p.isExpired(), "Un castigo permanente no debe expirar");
    }
}
