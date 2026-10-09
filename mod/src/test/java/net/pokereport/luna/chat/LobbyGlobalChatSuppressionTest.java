package net.pokereport.luna.chat;

import net.minecraft.util.math.Vec3d;
import net.pokereport.luna.ui.Tablist;
import net.pokereport.luna.world.LunaDimensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que un jugador en el Lobby tenga el chat global completamente suprimido (Silent Mode).
 */
public class LobbyGlobalChatSuppressionTest {

    @Test
    @DisplayName("Lobby player cannot receive Global chat from other players")
    void testLobbyPlayerCannotReceiveGlobalChat() {
        boolean canReceive = ChatDimensionPolicy.canReceiveChat(
                LunaDimensions.LOBBY, new Vec3d(40, 72, 48), Tablist.Rank.ENTRENADOR, null,
                LunaDimensions.CIUDADELA, new Vec3d(4, 70, 0), Tablist.Rank.ENTRENADOR, null,
                ChatChannel.GLOBAL
        );
        assertFalse(canReceive, "Un jugador en el Lobby NO debe recibir mensajes de chat global");
    }

    @Test
    @DisplayName("Lobby player cannot send Global chat")
    void testLobbyPlayerCannotSendGlobalChat() {
        boolean canSend = ChatDimensionPolicy.canSend(
                LunaDimensions.LOBBY, Tablist.Rank.ENTRENADOR, null, ChatChannel.GLOBAL
        );
        assertFalse(canSend, "Un jugador en el Lobby NO puede emitir mensajes al canal global");
    }

    @Test
    @DisplayName("Category PLAYER_CHAT is blocked in Lobby")
    void testPlayerChatCategoryBlockedInLobby() {
        assertFalse(ChatDimensionPolicy.canReceive(LunaDimensions.LOBBY, Tablist.Rank.ENTRENADOR, MessageCategory.PLAYER_CHAT),
                "La categoría PLAYER_CHAT debe estar bloqueada en Lobby");
    }
}
