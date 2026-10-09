package net.pokereport.luna.chat;

import net.pokereport.luna.world.LunaDimensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica el filtrado estricto por dimensión antes del envío de anuncios.
 */
public class AnnouncementDimensionFilterTest {

    @Test
    @DisplayName("Announcement delivers to allowed dimensions and blocks others")
    void testDimensionFiltering() {
        ServerAnnouncementService.Announcement a = new ServerAnnouncementService.Announcement(
                "test", true, "Title", "Body", "https://pokereport.online/wiki/", "Hover",
                List.of("lunaeternal:lobby", "lunaeternal:ciudadela")
        );

        UUID player = UUID.randomUUID();

        // En Lobby -> permitido
        assertTrue(ServerAnnouncementService.canDeliverTo(LunaDimensions.LOBBY, player, a));

        // En Ciudadela -> permitido
        assertTrue(ServerAnnouncementService.canDeliverTo(LunaDimensions.CIUDADELA, player, a));

        // En Hogar (overworld) -> bloqueado
        assertFalse(ServerAnnouncementService.canDeliverTo(LunaDimensions.HOGAR, player, a));

        // En Salvaje -> bloqueado
        assertFalse(ServerAnnouncementService.canDeliverTo(LunaDimensions.SALVAJE, player, a));

        // En Gimnasios -> bloqueado
        assertFalse(ServerAnnouncementService.canDeliverTo(LunaDimensions.GIMNASIOS, player, a));
    }
}
