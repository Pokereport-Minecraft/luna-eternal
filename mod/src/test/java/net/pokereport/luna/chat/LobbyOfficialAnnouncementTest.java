package net.pokereport.luna.chat;

import net.pokereport.luna.world.LunaDimensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que los anuncios oficiales con allowedDimensions conteniendo el Lobby puedan entregarse en Lobby.
 */
public class LobbyOfficialAnnouncementTest {

    @Test
    @DisplayName("Official announcements targeting Lobby are delivered in Lobby")
    void testOfficialAnnouncementsDeliverToLobby() {
        ServerAnnouncementService.Announcement wiki = ServerAnnouncementService.getAnnouncements().stream()
                .filter(a -> "wiki".equals(a.id()))
                .findFirst()
                .orElse(null);

        assertNotNull(wiki, "El anuncio wiki debe existir");
        assertTrue(wiki.allowedDimensions().contains("lunaeternal:lobby"), "El anuncio debe permitir Lobby");

        UUID playerId = UUID.randomUUID();
        // Sin registro de join timestamp reciente, pasa la comprobación de delay
        boolean canDeliver = ServerAnnouncementService.canDeliverTo(LunaDimensions.LOBBY, playerId, wiki);
        assertTrue(canDeliver, "El anuncio oficial debe entregarse en el Lobby");
    }
}
