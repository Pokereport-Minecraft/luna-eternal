package net.pokereport.luna.chat;

import net.pokereport.luna.world.LunaDimensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que los anuncios oficiales se entreguen correctamente en la Ciudadela.
 */
public class CitadelOfficialAnnouncementTest {

    @Test
    @DisplayName("Official announcements targeting Ciudadela are delivered in Ciudadela")
    void testOfficialAnnouncementsDeliverToCiudadela() {
        ServerAnnouncementService.Announcement store = ServerAnnouncementService.getAnnouncements().stream()
                .filter(a -> "tienda".equals(a.id()))
                .findFirst()
                .orElse(null);

        assertNotNull(store, "El anuncio tienda debe existir");
        assertTrue(store.allowedDimensions().contains("lunaeternal:ciudadela"), "El anuncio debe permitir Ciudadela");

        UUID playerId = UUID.randomUUID();
        boolean canDeliver = ServerAnnouncementService.canDeliverTo(LunaDimensions.CIUDADELA, playerId, store);
        assertTrue(canDeliver, "El anuncio oficial de tienda debe entregarse en la Ciudadela");
    }
}
