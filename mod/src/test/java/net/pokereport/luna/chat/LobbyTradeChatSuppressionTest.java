package net.pokereport.luna.chat;

import net.minecraft.util.math.Vec3d;
import net.pokereport.luna.ui.Tablist;
import net.pokereport.luna.world.LunaDimensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que el canal Comercio/Trade esté suprimido en el Lobby.
 */
public class LobbyTradeChatSuppressionTest {

    @Test
    @DisplayName("Lobby player cannot receive Trade chat")
    void testLobbyPlayerCannotReceiveTradeChat() {
        boolean canReceive = ChatDimensionPolicy.canReceiveChat(
                LunaDimensions.LOBBY, new Vec3d(40, 72, 48), Tablist.Rank.ENTRENADOR, null,
                LunaDimensions.CIUDADELA, new Vec3d(4, 70, 0), Tablist.Rank.ENTRENADOR, null,
                ChatChannel.COMERCIO
        );
        assertFalse(canReceive, "El Lobby NO debe recibir anuncios de Comercio/Trade");
    }

    @Test
    @DisplayName("Lobby player cannot send Trade chat")
    void testLobbyPlayerCannotSendTradeChat() {
        boolean canSend = ChatDimensionPolicy.canSend(
                LunaDimensions.LOBBY, Tablist.Rank.ENTRENADOR, null, ChatChannel.COMERCIO
        );
        assertFalse(canSend, "Un jugador en Lobby NO puede enviar mensajes de Comercio");
    }
}
