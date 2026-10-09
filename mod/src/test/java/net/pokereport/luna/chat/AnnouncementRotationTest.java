package net.pokereport.luna.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la rotación secuencial de anuncios oficiales.
 */
public class AnnouncementRotationTest {

    @BeforeEach
    void setUp() {
        ServerAnnouncementService.reset();
    }

    @Test
    @DisplayName("Announcements rotate sequentially without skipping")
    void testSequentialRotation() {
        ServerAnnouncementService.Announcement a1 = ServerAnnouncementService.getNextAnnouncement();
        ServerAnnouncementService.Announcement a2 = ServerAnnouncementService.getNextAnnouncement();
        ServerAnnouncementService.Announcement a3 = ServerAnnouncementService.getNextAnnouncement();
        ServerAnnouncementService.Announcement a4 = ServerAnnouncementService.getNextAnnouncement();
        ServerAnnouncementService.Announcement a5 = ServerAnnouncementService.getNextAnnouncement();
        ServerAnnouncementService.Announcement a6 = ServerAnnouncementService.getNextAnnouncement();

        assertNotNull(a1);
        assertNotNull(a2);
        assertNotNull(a3);
        assertNotNull(a4);
        assertNotNull(a5);
        assertNotNull(a6);

        assertEquals("wiki", a1.id());
        assertEquals("discord", a2.id());
        assertEquals("tienda", a3.id());
        assertEquals("soporte", a4.id());
        assertEquals("guias", a5.id());
        assertEquals("wiki", a6.id(), "La rotación debe recomenzar en wiki tras completar el ciclo");
    }
}
