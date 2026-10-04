package net.pokereport.luna.ui;

import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.ui.Tablist.Rank;

/** Filters automatic player event announcements, never ordinary chat or personal command feedback. */
public final class StaffPlayerNotices {
    private StaffPlayerNotices() {}
    public static boolean isPlayerEvent(Text message) {
        if (message.getContent() instanceof TranslatableTextContent content) {
            String key = content.getKey();
            if (key.startsWith("death.") || key.startsWith("chat.type.advancement.")
                    || key.equals("multiplayer.player.joined")
                    || key.equals("multiplayer.player.joined.renamed")
                    || key.equals("multiplayer.player.left")) return true;
        }
        // Formatting wrappers can place the actual event in siblings. Translation arguments
        // are deliberately not inspected: command feedback may quote an event or a player's name.
        for (Text sibling : message.getSiblings()) if (isPlayerEvent(sibling)) return true;
        return false;
    }
    public static boolean canReceive(boolean operator, Rank rank) {
        return operator && rank != null && rank.esStaff();
    }
    public static void dispatch(PlayerManager manager, Text message, boolean overlay) {
        LunaEternal.LOG.info("[staff-event] {}", message.getString());
        for (var player : manager.getPlayerList()) {
            if (canReceive(manager.isOperator(player.getGameProfile()),
                    Tablist.rankOf(player.getServer(), player))) player.sendMessage(message, overlay);
        }
    }
}
