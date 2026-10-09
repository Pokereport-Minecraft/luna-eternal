package net.pokereport.luna.chat;

import net.pokereport.luna.ui.Tablist;
import net.pokereport.luna.world.LunaDimensions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la política de mensajes privados en el Lobby (silencioso por defecto, configurable).
 */
public class LobbyPrivatePolicyTest {

    @BeforeEach
    void setUp() {
        ChatDimensionPolicy.setPrivateMessagesInLobby(false);
    }

    @AfterEach
    void tearDown() {
        ChatDimensionPolicy.setPrivateMessagesInLobby(false);
    }

    @Test
    @DisplayName("Private messages blocked by default in Lobby")
    void testPrivateMessagesBlockedByDefaultInLobby() {
        assertFalse(ChatDimensionPolicy.isPrivateMessagesInLobby(), "Por defecto debe ser false");
        assertFalse(ChatDimensionPolicy.canReceive(LunaDimensions.LOBBY, Tablist.Rank.ENTRENADOR, MessageCategory.PRIVATE),
                "En Lobby no se deben recibir privados por defecto");
        assertFalse(ChatDimensionPolicy.canSend(LunaDimensions.LOBBY, Tablist.Rank.ENTRENADOR, null, ChatChannel.PRIVADO),
                "En Lobby no se deben enviar privados por defecto");
    }

    @Test
    @DisplayName("Private messages allowed when configured in Lobby")
    void testPrivateMessagesAllowedWhenConfigured() {
        ChatDimensionPolicy.setPrivateMessagesInLobby(true);
        assertTrue(ChatDimensionPolicy.isPrivateMessagesInLobby());
        assertTrue(ChatDimensionPolicy.canReceive(LunaDimensions.LOBBY, Tablist.Rank.ENTRENADOR, MessageCategory.PRIVATE),
                "Si está configurado true, se permiten privados en Lobby");
        assertTrue(ChatDimensionPolicy.canSend(LunaDimensions.LOBBY, Tablist.Rank.ENTRENADOR, null, ChatChannel.PRIVADO));
    }
}
