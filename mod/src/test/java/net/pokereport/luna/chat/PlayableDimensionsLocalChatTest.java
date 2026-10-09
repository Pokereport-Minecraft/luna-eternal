package net.pokereport.luna.chat;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.pokereport.luna.ui.Tablist;
import net.pokereport.luna.world.LunaDimensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de regresión que comprueba el funcionamiento del chat local (radio 64 bloques)
 * en todas las dimensiones jugables de PokeReport Network:
 * - Overworld (Hogar)
 * - Salvaje
 * - Gimnasios
 * - Ciudadela
 * Y verifica que en Lobby el chat local quede estrictamente bloqueado,
 * así como el aislamiento absoluto entre dimensiones distintas.
 */
public class PlayableDimensionsLocalChatTest {

    private static final RegistryKey<World>[] PLAYABLE_DIMENSIONS = new RegistryKey[]{
            LunaDimensions.HOGAR, // minecraft:overworld
            LunaDimensions.SALVAJE, // lunaeternal:salvaje
            LunaDimensions.GIMNASIOS, // lunaeternal:gimnasios
            LunaDimensions.CIUDADELA // lunaeternal:ciudadela
    };

    @Test
    @DisplayName("Local chat is delivered within 64 blocks in all playable dimensions")
    void testLocalChatWithinRadiusInPlayableDimensions() {
        Vec3d posA = new Vec3d(100, 64, 100);
        Vec3d posB = new Vec3d(140, 64, 100); // 40 bloques (< 64)

        for (RegistryKey<World> dim : PLAYABLE_DIMENSIONS) {
            boolean canSend = ChatDimensionPolicy.canSend(dim, Tablist.Rank.ENTRENADOR, null, ChatChannel.LOCAL);
            assertTrue(canSend, "El envío en LOCAL debe estar habilitado en " + dim.getValue());

            boolean canReceive = ChatDimensionPolicy.canReceiveChat(
                    dim, posB, Tablist.Rank.ENTRENADOR, null,
                    dim, posA, Tablist.Rank.ENTRENADOR, null,
                    ChatChannel.LOCAL
            );
            assertTrue(canReceive, "Jugadores a 40 bloques en " + dim.getValue() + " deben recibir chat local");
        }
    }

    @Test
    @DisplayName("Local chat is blocked beyond 64 blocks in all playable dimensions")
    void testLocalChatBeyondRadiusInPlayableDimensions() {
        Vec3d posA = new Vec3d(0, 64, 0);
        Vec3d posFar = new Vec3d(65, 64, 0); // 65 bloques (> 64)

        for (RegistryKey<World> dim : PLAYABLE_DIMENSIONS) {
            boolean canReceive = ChatDimensionPolicy.canReceiveChat(
                    dim, posFar, Tablist.Rank.ENTRENADOR, null,
                    dim, posA, Tablist.Rank.ENTRENADOR, null,
                    ChatChannel.LOCAL
            );
            assertFalse(canReceive, "Jugadores a 65 bloques en " + dim.getValue() + " no deben recibir chat local");
        }
    }

    @Test
    @DisplayName("Local chat is strictly BLOCKED in Lobby for sending and receiving")
    void testLocalChatBlockedInLobby() {
        Vec3d posA = new Vec3d(0, 100, 0);
        Vec3d posB = new Vec3d(10, 100, 0); // 10 bloques

        boolean canSend = ChatDimensionPolicy.canSend(LunaDimensions.LOBBY, Tablist.Rank.ENTRENADOR, null, ChatChannel.LOCAL);
        assertFalse(canSend, "No se debe poder enviar chat LOCAL en el Lobby");

        boolean canReceive = ChatDimensionPolicy.canReceiveChat(
                LunaDimensions.LOBBY, posB, Tablist.Rank.ENTRENADOR, null,
                LunaDimensions.LOBBY, posA, Tablist.Rank.ENTRENADOR, null,
                ChatChannel.LOCAL
        );
        assertFalse(canReceive, "No se debe poder recibir chat LOCAL en el Lobby");
    }

    @Test
    @DisplayName("Local chat NEVER crosses different dimensions even within 0 distance")
    void testLocalChatCrossDimensionIsolation() {
        Vec3d identicalPos = new Vec3d(50, 70, 50);

        for (int i = 0; i < PLAYABLE_DIMENSIONS.length; i++) {
            for (int j = 0; j < PLAYABLE_DIMENSIONS.length; j++) {
                if (i == j) continue;
                RegistryKey<World> sendDim = PLAYABLE_DIMENSIONS[i];
                RegistryKey<World> recDim = PLAYABLE_DIMENSIONS[j];

                boolean canReceive = ChatDimensionPolicy.canReceiveChat(
                        recDim, identicalPos, Tablist.Rank.ENTRENADOR, null,
                        sendDim, identicalPos, Tablist.Rank.ENTRENADOR, null,
                        ChatChannel.LOCAL
                );
                assertFalse(canReceive, "El chat local nunca debe cruzar de " + sendDim.getValue() + " a " + recDim.getValue());
            }
        }
    }
}
