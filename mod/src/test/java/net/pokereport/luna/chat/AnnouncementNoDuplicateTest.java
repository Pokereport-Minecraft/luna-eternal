package net.pokereport.luna.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que no ocurran anuncios duplicados consecutivos cuando hay múltiples habilitados.
 */
public class AnnouncementNoDuplicateTest {

    @BeforeEach
    void setUp() {
        ServerAnnouncementService.reset();
    }

    @Test
    @DisplayName("Consecutive announcements are never identical when multiple are enabled")
    void testNoConsecutiveDuplicates() {
        ServerAnnouncementService.Announcement prev = null;
        for (int i = 0; i < 20; i++) {
            ServerAnnouncementService.Announcement curr = ServerAnnouncementService.getNextAnnouncement();
            assertNotNull(curr);
            if (prev != null) {
                assertNotEquals(prev.id(), curr.id(), "Dos anuncios consecutivos no deben ser idénticos");
            }
            prev = curr;
        }
    }
}
