package net.pokereport.luna.chat;

import net.minecraft.util.math.Vec3d;
import net.pokereport.luna.ui.Tablist;
import net.pokereport.luna.world.LunaDimensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que un jugador en el Lobby tenga el chat local completamente bloqueado.
 */
public class LobbyLocalChatSuppressionTest {

    @Test
    @DisplayName("Lobby player cannot receive Local chat even at identical coordinates")
    void testLobbyPlayerCannotReceiveLocalChat() {
        boolean canReceive = ChatDimensionPolicy.canReceiveChat(
                LunaDimensions.LOBBY, new Vec3d(40, 72, 48), Tablist.Rank.ENTRENADOR, null,
                LunaDimensions.LOBBY, new Vec3d(40, 72, 49), Tablist.Rank.ENTRENADOR, null,
                ChatChannel.LOCAL
        );
        assertFalse(canReceive, "El Lobby debe silenciar el chat local");
    }

    @Test
    @DisplayName("Lobby player cannot send Local chat")
    void testLobbyPlayerCannotSendLocalChat() {
        boolean canSend = ChatDimensionPolicy.canSend(
                LunaDimensions.LOBBY, Tablist.Rank.ENTRENADOR, null, ChatChannel.LOCAL
        );
        assertFalse(canSend, "Un jugador en el Lobby NO puede enviar chat local");
    }
}
