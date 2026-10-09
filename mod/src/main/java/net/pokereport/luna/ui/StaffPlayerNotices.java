package net.pokereport.luna.ui;

import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.ui.Tablist.Rank;

import java.util.Locale;

/** Filters automatic player event announcements, never ordinary chat or personal command feedback. */
public final class StaffPlayerNotices {
    private StaffPlayerNotices() {}

    public static boolean isPlayerEvent(Text message) {
        if (message == null) return false;
        if (message.getContent() instanceof TranslatableTextContent content) {
            String key = content.getKey();
            if (key.startsWith("death.") || key.startsWith("chat.type.advancement.")
                    || key.equals("multiplayer.player.joined")
                    || key.equals("multiplayer.player.joined.renamed")
                    || key.equals("multiplayer.player.left")) return true;
        }
        for (Text sibling : message.getSiblings()) if (isPlayerEvent(sibling)) return true;
        return false;
    }

    public static boolean isAdvancement(Text message) {
        if (message == null) return false;
        if (message.getContent() instanceof TranslatableTextContent content) {
            if (content.getKey().startsWith("chat.type.advancement.")) return true;
        }
        for (Text sibling : message.getSiblings()) if (isAdvancement(sibling)) return true;
        return false;
    }

    public static boolean isJoinLeave(Text message) {
        if (message == null) return false;
        if (message.getContent() instanceof TranslatableTextContent content) {
            String key = content.getKey();
            if (key.equals("multiplayer.player.joined")
                    || key.equals("multiplayer.player.joined.renamed")
                    || key.equals("multiplayer.player.left")) return true;
        }
        for (Text sibling : message.getSiblings()) if (isJoinLeave(sibling)) return true;
        return false;
    }

    public static boolean isMissionOrQuest(Text message) {
        if (message == null) return false;
        if (message.getContent() instanceof TranslatableTextContent content) {
            String key = content.getKey().toLowerCase(Locale.ROOT);
            if (key.contains("quest") || key.contains("mision") || key.contains("mission")) return true;
        }
        String str = message.getString().toLowerCase(Locale.ROOT);
        if (str.contains("misión completada") || str.contains("mision completada")
                || str.contains("quest completed") || str.contains("ha completado la misión")
                || str.contains("ha completado la mision") || str.contains("completed the quest")) {
            return true;
        }
        for (Text sibling : message.getSiblings()) if (isMissionOrQuest(sibling)) return true;
        return false;
    }

    public static boolean canReceive(boolean operator, Rank rank) {
        return operator && rank != null && rank.esStaff();
    }

    public static boolean canReceiveJoinLeave(boolean operator, Rank rank) {
        if (rank == null) return false;
        return rank == Rank.ADMIN || rank == Rank.OWNER || rank == Rank.HEAD_ADMIN;
    }

    public static void dispatch(PlayerManager manager, Text message, boolean overlay) {
        if (manager == null || message == null) return;
        if (isAdvancement(message) || isMissionOrQuest(message)) {
            // Ni avances ni misiones/quests se envían jamás al chat global ni de staff
            return;
        }
        LunaEternal.LOG.info("[staff-event] {}", message.getString());
        boolean joinLeave = isJoinLeave(message);
        for (var player : manager.getPlayerList()) {
            Rank rank = Tablist.rankOf(player.getServer(), player);
            boolean op = manager.isOperator(player.getGameProfile());
            boolean allowed = joinLeave ? canReceiveJoinLeave(op, rank) : canReceive(op, rank);
            if (allowed) {
                player.sendMessage(message, overlay);
            }
        }
    }
}
