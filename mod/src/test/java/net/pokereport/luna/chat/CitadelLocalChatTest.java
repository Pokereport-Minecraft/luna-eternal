package net.pokereport.luna.chat;

import net.minecraft.util.math.Vec3d;
import net.pokereport.luna.ui.Tablist;
import net.pokereport.luna.world.LunaDimensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica el chat local en Ciudadela con radio de 64 bloques y aislamiento respecto a otras dimensiones.
 */
public class CitadelLocalChatTest {

    @Test
    @DisplayName("Local chat is delivered within 64 blocks in Ciudadela")
    void testLocalChatWithinRadiusInCiudadela() {
        Vec3d posA = new Vec3d(4, 70, 0);
        Vec3d posB = new Vec3d(24, 70, 0); // 20 bloques de distancia (< 64)

        boolean canReceive = ChatDimensionPolicy.canReceiveChat(
                LunaDimensions.CIUDADELA, posB, Tablist.Rank.ENTRENADOR, null,
                LunaDimensions.CIUDADELA, posA, Tablist.Rank.ENTRENADOR, null,
                ChatChannel.LOCAL
        );
        assertTrue(canReceive, "Jugadores a menos de 64 bloques en Ciudadela deben escuchar el chat local");
    }

    @Test
    @DisplayName("Local chat is blocked beyond 64 blocks in Ciudadela")
    void testLocalChatBeyondRadiusInCiudadela() {
        Vec3d posA = new Vec3d(4, 70, 0);
        Vec3d posB = new Vec3d(75, 70, 0); // 71 bloques de distancia (> 64)

        boolean canReceive = ChatDimensionPolicy.canReceiveChat(
                LunaDimensions.CIUDADELA, posB, Tablist.Rank.ENTRENADOR, null,
                LunaDimensions.CIUDADELA, posA, Tablist.Rank.ENTRENADOR, null,
                ChatChannel.LOCAL
        );
        assertFalse(canReceive, "Jugadores a más de 64 bloques en Ciudadela no deben escuchar el chat local");
    }

    @Test
    @DisplayName("Local chat from Ciudadela never reaches Lobby players")
    void testLocalChatNeverLeakesToLobby() {
        Vec3d posCiudadela = new Vec3d(4, 70, 0);
        Vec3d posLobby = new Vec3d(4, 70, 0); // Mismas coordenadas pero distinta dimensión

        boolean canReceive = ChatDimensionPolicy.canReceiveChat(
                LunaDimensions.LOBBY, posLobby, Tablist.Rank.ENTRENADOR, null,
                LunaDimensions.CIUDADELA, posCiudadela, Tablist.Rank.ENTRENADOR, null,
                ChatChannel.LOCAL
        );
        assertFalse(canReceive, "El chat local jamás cruza dimensiones ni alcanza al Lobby");
    }
}
