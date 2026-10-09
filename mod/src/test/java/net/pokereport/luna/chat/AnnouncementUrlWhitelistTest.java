package net.pokereport.luna.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la lista blanca de dominios permitidos para OpenURL en anuncios.
 */
public class AnnouncementUrlWhitelistTest {

    @Test
    @DisplayName("Official domains are whitelisted")
    void testOfficialDomainsWhitelisted() {
        assertTrue(ServerAnnouncementService.isUrlWhitelisted("https://pokereport.online/wiki/"));
        assertTrue(ServerAnnouncementService.isUrlWhitelisted("https://pokereport.online/tienda/"));
        assertTrue(ServerAnnouncementService.isUrlWhitelisted("https://play.pokereport.online"));
        assertTrue(ServerAnnouncementService.isUrlWhitelisted("https://discord.gg/JsUTQWRH8H"));
    }

    @Test
    @DisplayName("Untrusted or malicious domains are rejected")
    void testUntrustedDomainsRejected() {
        assertFalse(ServerAnnouncementService.isUrlWhitelisted("https://malicious.com/stealer"));
        assertFalse(ServerAnnouncementService.isUrlWhitelisted("https://phishing.net"));
        assertFalse(ServerAnnouncementService.isUrlWhitelisted("http://127.0.0.1:8080"));
        assertFalse(ServerAnnouncementService.isUrlWhitelisted("javascript:alert(1)"));
        assertFalse(ServerAnnouncementService.isUrlWhitelisted(""));
        assertFalse(ServerAnnouncementService.isUrlWhitelisted(null));
    }
}
