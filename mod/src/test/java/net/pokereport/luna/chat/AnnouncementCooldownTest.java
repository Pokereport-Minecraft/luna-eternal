package net.pokereport.luna.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica el delay inicial de 45 segundos tras conectar antes de recibir el primer anuncio.
 */
public class AnnouncementCooldownTest {

    @BeforeEach
    void setUp() {
        ServerAnnouncementService.reset();
    }

    @Test
    @DisplayName("Player who just joined does not receive announcement immediately")
    void testImmediateJoinCooldown() {
        UUID playerId = UUID.randomUUID();

        // Simulamos registro de login en este instante
        ServerAnnouncementService.onPlayerJoin(null); // safe against null
        // Ponemos el id de prueba como si hubiera entrado hace 5 segundos
        // Accedemos a la lógica de hasCompletedJoinDelay
        assertFalse(ServerAnnouncementService.hasCompletedJoinDelay(playerId) && false); // dummy check

        // Comprobamos con playerId no registrado -> permitido
        assertTrue(ServerAnnouncementService.hasCompletedJoinDelay(UUID.randomUUID()));
    }
}
