package net.pokereport.luna.gym;

import java.util.Locale;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.pokereport.luna.world.LunaDimensions;

/**
 * Política centralizada de seguridad, restricciones de comandos,
 * anti-bypass y protecciones dentro de los gimnasios de PokeReport Network.
 */
public final class GymChallengePolicy {

    private static final Set<String> COMANDOS_BLOQUEADOS = Set.of(
            "home", "homes", "sethome", "delhome",
            "back", "spawn", "hogar", "salvaje",
            "tpa", "tpahere", "tpaccept", "tpdeny",
            "rtp", "wild", "warp", "warps", "pwarp", "pwarps",
            "clan home", "clan sethome"
    );

    private static final Set<String> COMANDOS_PERMITIDOS = Set.of(
            "gym", "gimnasio", "msg", "tell", "w", "r",
            "canal", "channel", "g", "l", "tr", "c", "sc",
            "help", "ayuda", "suicide", "ping", "rules", "ticket"
    );

    private GymChallengePolicy() {}

    public static boolean isInGymDimension(World world) {
        if (world == null) return false;
        var key = world.getRegistryKey();
        return LunaDimensions.GIMNASIOS.equals(key)
                || LunaDimensions.GIMNASIO_MORTI.equals(key)
                || LunaDimensions.ISLAS_NARANJA.equals(key);
    }

    public static boolean isChallengeActive(ServerPlayerEntity player) {
        if (player == null) return false;
        if (player.hasPermissionLevel(2) && player.isCreative()) return false;
        Ranuras.Asignacion asig = Ranuras.asignacionDe(player.getUuid());
        return asig != null || isInGymDimension(player.getWorld());
    }

    public static boolean isCommandBlocked(ServerPlayerEntity player, String command) {
        if (!isChallengeActive(player)) {
            return false;
        }
        if (command == null || command.isBlank()) {
            return false;
        }
        String clean = command.strip().toLowerCase(Locale.ROOT);
        if (clean.startsWith("/")) clean = clean.substring(1);
        String[] parts = clean.split("\\s+");
        String root = parts[0];
        int ns = root.indexOf(':');
        if (ns >= 0) root = root.substring(ns + 1);

        if (COMANDOS_PERMITIDOS.contains(root)) {
            return false;
        }

        if (COMANDOS_BLOQUEADOS.contains(root)) {
            return true;
        }

        if ("clan".equals(root) && parts.length > 1 && "home".equals(parts[1])) {
            return true;
        }

        return false;
    }

    public static boolean isItemBlocked(String itemId) {
        if (itemId == null || itemId.isBlank()) return false;
        String clean = itemId.toLowerCase(Locale.ROOT);
        return clean.endsWith("ender_pearl") || clean.endsWith("chorus_fruit");
    }

    public static boolean isItemBlocked(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            Item item = stack.getItem();
            return item == Items.ENDER_PEARL || item == Items.CHORUS_FRUIT;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isMountingAllowed(ServerPlayerEntity player, Entity mount) {
        if (!isChallengeActive(player)) return true;
        return false;
    }
}
