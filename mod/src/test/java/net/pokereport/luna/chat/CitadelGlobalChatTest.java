package net.pokereport.luna.chat;

import net.minecraft.util.math.Vec3d;
import net.pokereport.luna.ui.Tablist;
import net.pokereport.luna.world.LunaDimensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que el chat global funcione plenamente en la Ciudadela.
 */
public class CitadelGlobalChatTest {

    @Test
    @DisplayName("Players in Ciudadela can receive Global chat")
    void testCitadelPlayerCanReceiveGlobalChat() {
        boolean canReceive = ChatDimensionPolicy.canReceiveChat(
                LunaDimensions.CIUDADELA, new Vec3d(4, 70, 0), Tablist.Rank.ENTRENADOR, null,
                LunaDimensions.HOGAR, new Vec3d(100, 64, 100), Tablist.Rank.ENTRENADOR, null,
                ChatChannel.GLOBAL
        );
        assertTrue(canReceive, "Un jugador en Ciudadela debe recibir chat global");
    }

    @Test
    @DisplayName("Players in Ciudadela can send Global chat")
    void testCitadelPlayerCanSendGlobalChat() {
        boolean canSend = ChatDimensionPolicy.canSend(
                LunaDimensions.CIUDADELA, Tablist.Rank.ENTRENADOR, null, ChatChannel.GLOBAL
        );
        assertTrue(canSend, "Un jugador en Ciudadela debe poder enviar chat global");
    }
}
