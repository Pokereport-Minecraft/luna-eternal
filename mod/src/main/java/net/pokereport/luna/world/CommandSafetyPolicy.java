package net.pokereport.luna.world;

import java.util.Locale;
import java.util.Set;
import net.minecraft.server.network.ServerPlayerEntity;

/** Packet-level gate covers OP-2 bypasses through vanilla commands and editing tools. */
public final class CommandSafetyPolicy {
    private static final Set<String> CRITICAL = Set.of("op", "deop", "stop", "reload", "give", "execute",
            "function", "kill", "summon", "data", "gamemode", "gamerule", "defaultgamemode",
            "datapack", "schedule", "item", "loot", "place", "fill", "fillbiome", "clone", "setblock",
            "worldborder", "forceload", "setworldspawn", "npcspawn", "npcspawnat", "npcdelete", "npcedit",
            "givepokemon", "givepokemonother", "pokegive", "pokegiveother", "pokeeditother", "pokemoneditother",
            "runmolang", "runmolangscript", "tebex", "testcommand", "damage", "clear", "effect", "enchant",
            "experience", "xp", "attribute", "difficulty", "weather", "time", "rctmod",
            "teleport", "tp", "spreadplayers", "transfer", "save-off", "save-on", "reloadshowdown",
            "auth", "giveallmarks", "giveallpokemon", "givemark", "givepokemonegg", "givetm",
            "pokeedit", "pokemonedit", "spawnpokemon", "spawnpokemonat", "spawnpokemonfrompool",
            "clearparty", "testpartyslot", "lunita");
    private static final Set<String> LUNA_ADMIN = Set.of("dar", "constructor", "rango", "testrango",
            "gimnasio", "decorar", "naranja", "puerta", "traje", "pase", "torre_batalla",
            "paradas", "comercio", "enfermera", "kits", "rotacion", "reiniciarmision",
            "reiniciarinicial", "reiniciarmedalla", "darmedalla", "darmedallas", "via",
            "rotarcazas", "probar", "lunita", "quienpokemon", "borde");
    private static final Set<String> TERRAIN = Set.of("fill", "fillbiome", "clone", "setblock", "data",
            "execute", "function", "summon", "place", "item", "loot", "schedule");
    // These commands can target another dimension regardless of the source world.
    private static final Set<String> INDIRECT_TERRAIN = Set.of("execute", "function", "schedule", "clone");
    private static final Set<String> WORLDEDIT = Set.of("worldedit", "we", "brush", "br", "tool", "cs",
            "cycler", "deltree", "flood", "floodfill", "forestgen", "green", "lrbuild", "material",
            "mask", "gmask", "pickaxe", "pumpkins", "redo", "rem", "rement", "remove", "removeabove",
            "removebelow", "removenear", "repl", "replacenear", "restore", "schem", "schematic", "snow",
            "superpickaxe", "thaw", "toggleeditwand", "transformmodelpart", "tree", "undo", "!", ",", ";", ".s");
    private CommandSafetyPolicy() {}

    public record Access(boolean superiorStaff, boolean editGrant, boolean modeGrant, boolean commandGrant,
                         boolean creative, boolean editing, boolean safeWorld) {
        boolean mayEdit() { return superiorStaff && editGrant && modeGrant && creative && editing; }
    }

    public static boolean allowed(ServerPlayerEntity player, String command) {
        return allowed(new Access(player.hasPermissionLevel(4),
                WorldEditPermissions.allowed(player.getUuid(), WorldEditPermissions.EDIT),
                WorldEditPermissions.allowed(player.getUuid(), WorldEditPermissions.MODE),
                WorldEditPermissions.allowed(player.getUuid(), WorldEditPermissions.COMMANDS),
                player.isCreative(), WorldProtectionManager.editing(player),
                WorldProtectionManager.isProtectedDimension(player.getWorld())), command);
    }

    public static boolean allowed(Access access, String command) {
        String[] tokens = command.strip().split("\\s+", 3);
        String root = tokens[0].toLowerCase(Locale.ROOT);
        int namespace = root.indexOf(':');
        if (namespace >= 0) root = root.substring(namespace + 1);
        if ("luna".equals(root) && tokens.length > 1
                && "autotest".equalsIgnoreCase(tokens[1])) return false;
        if ("luna".equals(root) && tokens.length > 1
                && LUNA_ADMIN.contains(tokens[1].toLowerCase(Locale.ROOT))) {
            if (!access.superiorStaff() || !access.commandGrant()) return false;
            if (Set.of("gimnasio", "decorar", "naranja").contains(tokens[1].toLowerCase(Locale.ROOT))) {
                return access.mayEdit();
            }
        }
        boolean editor = root.startsWith("/") || WORLDEDIT.contains(root);
        if (editor) return access.mayEdit();
        if (CRITICAL.contains(root)) {
            if (!access.superiorStaff() || !access.commandGrant()) return false;
            if (INDIRECT_TERRAIN.contains(root) || (TERRAIN.contains(root) && access.safeWorld())) {
                return access.mayEdit();
            }
        }
        return true;
    }
}
