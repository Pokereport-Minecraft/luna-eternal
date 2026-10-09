package net.pokereport.luna.chat;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.ui.Tablist;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio centralizado de canales de chat, enrutamiento y formato de mensajes.
 */
public final class ChatChannelService {

    private static final Map<UUID, ChatChannel> ACTIVE_CHANNELS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> LAST_PRIVATE_SENDER = new ConcurrentHashMap<>();

    private ChatChannelService() {}

    public static ChatChannel getActiveChannel(ServerPlayerEntity player) {
        if (player == null) return ChatChannel.GLOBAL;
        return ACTIVE_CHANNELS.getOrDefault(player.getUuid(), ChatChannel.GLOBAL);
    }

    public static void setActiveChannel(ServerPlayerEntity player, ChatChannel channel) {
        if (player == null || channel == null) return;
        ACTIVE_CHANNELS.put(player.getUuid(), channel);
    }

    public static void onPlayerDisconnect(UUID uuid) {
        if (uuid != null) {
            ACTIVE_CHANNELS.remove(uuid);
            LAST_PRIVATE_SENDER.remove(uuid);
        }
    }

    public static MutableText formatPlayerMessage(ServerPlayerEntity sender, ChatChannel channel, String rawMessage) {
        MutableText header = Text.empty();
        header.append(channel.getFormattedTag());

        if (sender.server != null) {
            Tablist.Rank rank = Tablist.rankOf(sender.server, sender);
            header.append(Tablist.prefixOf(rank));
        }

        Tablist.ClanInfo clan = Tablist.clanDe(sender);
        if (clan != null && !clan.etiqueta().isEmpty()) {
            header.append(Tablist.tagText(clan)).append(Text.literal(" "));
        }

        header.append(sender.getName().copy().formatted(Formatting.WHITE));
        header.append(Text.literal(" §8» §f").append(Text.literal(rawMessage)));
        return header;
    }

    public static boolean broadcastMessage(ServerPlayerEntity sender, ChatChannel channel, String rawMessage) {
        if (sender == null || channel == null || rawMessage == null || rawMessage.isBlank()) {
            return false;
        }

        if (ChatDimensionPolicy.isLobby(sender)) {
            if (channel != ChatChannel.STAFF) {
                sender.sendMessage(Text.literal("§6§lPOKEREPORT §8» §7El chat estará disponible cuando ingreses a la aventura."), false);
                return false;
            }
        }

        if (!ChatDimensionPolicy.canSend(sender, channel)) {
            if (channel == ChatChannel.CLAN) {
                sender.sendMessage(Text.literal("§cNo perteneces a ningún clan."), false);
            } else if (channel == ChatChannel.STAFF) {
                sender.sendMessage(Text.literal("§cNo tienes permisos para hablar en el canal de Staff."), false);
            } else {
                sender.sendMessage(Text.literal("§cNo puedes enviar mensajes en este canal."), false);
            }
            return false;
        }

        MutableText formatted = formatPlayerMessage(sender, channel, rawMessage);

        if (sender.server != null) {
            for (ServerPlayerEntity recipient : sender.server.getPlayerManager().getPlayerList()) {
                if (ChatDimensionPolicy.canReceiveChat(recipient, sender, channel)) {
                    recipient.sendMessage(formatted, false);
                }
            }
            // Registro en consola del servidor
            sender.server.sendMessage(Text.literal("[" + channel.getId() + "] " + sender.getName().getString() + ": " + rawMessage));
        }

        return true;
    }

    public static boolean sendPrivateMessage(ServerPlayerEntity sender, ServerPlayerEntity target, String message) {
        if (sender == null || target == null || message == null || message.isBlank()) return false;

        // Comprobación de emisor en lobby
        if (ChatDimensionPolicy.isLobby(sender) && !ChatDimensionPolicy.isPrivateMessagesInLobby()) {
            sender.sendMessage(Text.literal("§6§lPOKEREPORT §8» §cNo puedes enviar mensajes privados desde el Lobby."), false);
            return false;
        }

        // Comprobación de destinatario en lobby
        if (ChatDimensionPolicy.isLobby(target) && !ChatDimensionPolicy.isPrivateMessagesInLobby()) {
            sender.sendMessage(Text.literal("§6§lPOKEREPORT §8» §cEse jugador está en el Lobby y no puede recibir mensajes privados."), false);
            return false;
        }

        LAST_PRIVATE_SENDER.put(target.getUuid(), sender.getUuid());

        MutableText toTarget = Text.literal("§d[De " + sender.getName().getString() + "]: §f" + message);
        MutableText toSender = Text.literal("§d[Para " + target.getName().getString() + "]: §f" + message);

        target.sendMessage(toTarget, false);
        sender.sendMessage(toSender, false);
        return true;
    }

    public static UUID getLastPrivateSender(UUID uuid) {
        if (uuid == null) return null;
        return LAST_PRIVATE_SENDER.get(uuid);
    }
}
