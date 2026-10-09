package net.pokereport.luna.chat;

import net.minecraft.util.math.Vec3d;
import net.pokereport.luna.ui.Tablist;
import net.pokereport.luna.world.LunaDimensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que el canal Clan esté suprimido en el Lobby.
 */
public class LobbyClanChatSuppressionTest {

    @Test
    @DisplayName("Lobby player cannot receive Clan chat")
    void testLobbyPlayerCannotReceiveClanChat() {
        Tablist.ClanInfo clan = new Tablist.ClanInfo("POKE", 'b', "#55FFFF", "#55FFFF", false, false);
        boolean canReceive = ChatDimensionPolicy.canReceiveChat(
                LunaDimensions.LOBBY, new Vec3d(40, 72, 48), Tablist.Rank.ENTRENADOR, clan,
                LunaDimensions.CIUDADELA, new Vec3d(4, 70, 0), Tablist.Rank.ENTRENADOR, clan,
                ChatChannel.CLAN
        );
        assertFalse(canReceive, "El Lobby NO debe recibir mensajes de Clan");
    }

    @Test
    @DisplayName("Lobby player cannot send Clan chat")
    void testLobbyPlayerCannotSendClanChat() {
        Tablist.ClanInfo clan = new Tablist.ClanInfo("POKE", 'b', "#55FFFF", "#55FFFF", false, false);
        boolean canSend = ChatDimensionPolicy.canSend(
                LunaDimensions.LOBBY, Tablist.Rank.ENTRENADOR, clan, ChatChannel.CLAN
        );
        assertFalse(canSend, "Un jugador en Lobby NO puede emitir mensajes de Clan");
    }
}
