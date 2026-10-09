package net.pokereport.luna.chat;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;

/**
 * Canales de comunicación en Chat System 2.0.
 */
public enum ChatChannel {
    GLOBAL("global", "g", "§8[§aGlobal§8] ", Formatting.GREEN),
    LOCAL("local", "l", "§8[§eLocal§8] ", Formatting.YELLOW),
    COMERCIO("comercio", "tr", "§8[§6Comercio§8] ", Formatting.GOLD),
    CLAN("clan", "c", "§8[§bClan§8] ", Formatting.AQUA),
    STAFF("staff", "sc", "§8[§cStaff§8] ", Formatting.RED),
    PRIVADO("privado", "msg", "§8[§dPrivado§8] ", Formatting.LIGHT_PURPLE);

    private final String id;
    private final String alias;
    private final String tag;
    private final Formatting color;

    ChatChannel(String id, String alias, String tag, Formatting color) {
        this.id = id;
        this.alias = alias;
        this.tag = tag;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public String getTag() {
        return tag;
    }

    public Formatting getColor() {
        return color;
    }

    public MutableText getFormattedTag() {
        return Text.literal(tag);
    }

    public static ChatChannel fromString(String str) {
        if (str == null) return GLOBAL;
        String s = str.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "local", "l" -> LOCAL;
            case "comercio", "trade", "tr" -> COMERCIO;
            case "clan", "c" -> CLAN;
            case "staff", "sc", "s" -> STAFF;
            case "privado", "msg", "p" -> PRIVADO;
            default -> GLOBAL;
        };
    }
}
