package net.pokereport.luna.chat;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.pokereport.luna.ui.Tablist;
import net.pokereport.luna.world.LunaDimensions;

/**
 * Política centralizada de comunicación por dimensión.
 * Controla qué mensajes puede enviar y recibir cada jugador según su entorno físico.
 */
public final class ChatDimensionPolicy {

    public static final String LOBBY_ID = "lunaeternal:lobby";
    public static final String CIUDADELA_ID = "lunaeternal:ciudadela";
    public static final double DEFAULT_LOCAL_RADIUS = 64.0;

    private static double localRadius = DEFAULT_LOCAL_RADIUS;
    private static boolean privateMessagesInLobby = false;

    private ChatDimensionPolicy() {}

    public static double getLocalRadius() {
        return localRadius;
    }

    public static void setLocalRadius(double radius) {
        localRadius = Math.max(1.0, radius);
    }

    public static boolean isPrivateMessagesInLobby() {
        return privateMessagesInLobby;
    }

    public static void setPrivateMessagesInLobby(boolean allow) {
        privateMessagesInLobby = allow;
    }

    public static boolean isLobby(ServerPlayerEntity player) {
        if (player == null || player.getWorld() == null) return false;
        return isLobby(player.getWorld().getRegistryKey());
    }

    public static boolean isLobby(RegistryKey<World> key) {
        if (key == null) return false;
        return LunaDimensions.LOBBY.equals(key) || key.getValue().toString().equals(LOBBY_ID);
    }

    public static boolean isCiudadela(ServerPlayerEntity player) {
        if (player == null || player.getWorld() == null) return false;
        return isCiudadela(player.getWorld().getRegistryKey());
    }

    public static boolean isCiudadela(RegistryKey<World> key) {
        if (key == null) return false;
        return LunaDimensions.CIUDADELA.equals(key) || key.getValue().toString().equals(CIUDADELA_ID);
    }

    // -------------------------------------------------------------------------
    // canSend
    // -------------------------------------------------------------------------

    public static boolean canSend(ServerPlayerEntity sender, ChatChannel channel) {
        if (sender == null || channel == null) return false;
        RegistryKey<World> world = sender.getWorld() != null ? sender.getWorld().getRegistryKey() : null;
        Tablist.Rank rank = sender.server != null ? Tablist.rankOf(sender.server, sender) : null;
        Tablist.ClanInfo clan = Tablist.clanDe(sender);
        return canSend(world, rank, clan, channel);
    }

    public static boolean canSend(RegistryKey<World> world, Tablist.Rank rank, Tablist.ClanInfo clan, ChatChannel channel) {
        if (channel == null) return false;

        boolean inLobby = isLobby(world);

        if (inLobby) {
            if (channel == ChatChannel.STAFF) {
                return rank != null && rank.esStaff();
            }
            if (channel == ChatChannel.PRIVADO) {
                return privateMessagesInLobby;
            }
            // En Lobby, el chat de jugador estándar (GLOBAL, LOCAL, COMERCIO, CLAN) queda bloqueado
            return false;
        }

        if (channel == ChatChannel.STAFF) {
            return rank != null && rank.esStaff();
        }

        if (channel == ChatChannel.CLAN) {
            return clan != null && !clan.etiqueta().isEmpty();
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // canReceive
    // -------------------------------------------------------------------------

    public static boolean canReceive(ServerPlayerEntity recipient, MessageCategory category) {
        if (recipient == null || category == null) return false;
        RegistryKey<World> world = recipient.getWorld() != null ? recipient.getWorld().getRegistryKey() : null;
        Tablist.Rank rank = recipient.server != null ? Tablist.rankOf(recipient.server, recipient) : null;
        return canReceive(world, rank, category);
    }

    public static boolean canReceive(RegistryKey<World> world, Tablist.Rank rank, MessageCategory category) {
        if (category == null) return false;
        boolean inLobby = isLobby(world);

        return switch (category) {
            case PLAYER_CHAT -> !inLobby;
            case PRIVATE -> inLobby ? privateMessagesInLobby : true;
            case PERSONAL_SYSTEM, AUTHENTICATION, MISSION_PERSONAL -> true;
            case OFFICIAL_ANNOUNCEMENT -> true;
            case STAFF_CHAT -> rank != null && rank.esStaff();
            case JOIN_LEAVE_ADMIN -> rank == Tablist.Rank.ADMIN || rank == Tablist.Rank.OWNER || rank == Tablist.Rank.HEAD_ADMIN;
        };
    }

    // -------------------------------------------------------------------------
    // canReceiveChat
    // -------------------------------------------------------------------------

    public static boolean canReceiveChat(ServerPlayerEntity recipient, ServerPlayerEntity sender, ChatChannel channel) {
        if (recipient == null || sender == null || channel == null) return false;
        RegistryKey<World> recWorld = recipient.getWorld() != null ? recipient.getWorld().getRegistryKey() : null;
        Vec3d recPos = recipient.getPos();
        Tablist.Rank recRank = recipient.server != null ? Tablist.rankOf(recipient.server, recipient) : null;
        Tablist.ClanInfo recClan = Tablist.clanDe(recipient);

        RegistryKey<World> sendWorld = sender.getWorld() != null ? sender.getWorld().getRegistryKey() : null;
        Vec3d sendPos = sender.getPos();
        Tablist.Rank sendRank = sender.server != null ? Tablist.rankOf(sender.server, sender) : null;
        Tablist.ClanInfo sendClan = Tablist.clanDe(sender);

        return canReceiveChat(recWorld, recPos, recRank, recClan, sendWorld, sendPos, sendRank, sendClan, channel);
    }

    public static boolean canReceiveChat(RegistryKey<World> recWorld, Vec3d recPos, Tablist.Rank recRank, Tablist.ClanInfo recClan,
                                         RegistryKey<World> sendWorld, Vec3d sendPos, Tablist.Rank sendRank, Tablist.ClanInfo sendClan,
                                         ChatChannel channel) {
        if (channel == null) return false;

        // Si el destinatario está en el Lobby:
        if (isLobby(recWorld)) {
            if (channel == ChatChannel.STAFF) {
                return recRank != null && recRank.esStaff();
            }
            if (channel == ChatChannel.PRIVADO) {
                return privateMessagesInLobby;
            }
            // El Lobby es modo silencioso: jamás recibe GLOBAL, LOCAL, TRADE o CLAN
            return false;
        }

        // Si el emisor está en el Lobby y no es staff ni privado:
        if (isLobby(sendWorld) && channel != ChatChannel.STAFF && channel != ChatChannel.PRIVADO) {
            return false;
        }

        return switch (channel) {
            case GLOBAL, COMERCIO -> true;
            case LOCAL -> {
                if (recWorld == null || sendWorld == null || !recWorld.equals(sendWorld)) yield false;
                if (isLobby(recWorld) || isLobby(sendWorld)) yield false;
                if (recPos != null && sendPos != null) {
                    double distSq = recPos.squaredDistanceTo(sendPos);
                    yield distSq <= (localRadius * localRadius);
                }
                yield false;
            }
            case CLAN -> {
                if (sendClan == null || recClan == null) yield false;
                yield sendClan.etiqueta() != null && sendClan.etiqueta().equalsIgnoreCase(recClan.etiqueta());
            }
            case STAFF -> recRank != null && recRank.esStaff();
            case PRIVADO -> true;
        };
    }
}
