package net.pokereport.luna.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.pokereport.luna.LunaEternal;

/** Bounded dedicated log; staff edits do not flood the main server console. */
public final class WorldEditAudit {
    private static final Logger LOG = Logger.getLogger("pokereport.world.edit");
    private static FileHandler handler;
    private WorldEditAudit() {}

    public static void start() {
        try {
            Files.createDirectories(Path.of("logs"));
            handler = new FileHandler("logs/pokereport-world-edit-%g.log", 5_000_000, 4, true);
            handler.setFormatter(new SimpleFormatter());
            LOG.setUseParentHandlers(false);
            LOG.addHandler(handler);
        } catch (IOException e) {
            LunaEternal.LOG.error("No se pudo abrir el registro de edición", e);
        }
    }

    public static void close() {
        if (handler != null) {
            LOG.removeHandler(handler);
            handler.close();
            handler = null;
        }
    }

    public static void block(ServerPlayerEntity player, BlockPos pos, String action,
                             BlockState before, BlockState after) {
        if (!WorldProtectionManager.isProtectedDimension(player.getWorld())) return;
        String entry = java.time.Instant.now() + " uuid=" + player.getUuid()
                + " user=" + player.getGameProfile().getName()
                + " dimension=" + player.getWorld().getRegistryKey().getValue()
                + " pos=" + pos.toShortString() + " action=" + action
                + " before=" + Registries.BLOCK.getId(before.getBlock())
                + " after=" + Registries.BLOCK.getId(after.getBlock());
        if (handler != null) LOG.info(entry);
        else LunaEternal.LOG.warn("[WORLD-EDIT] {}", entry);
    }
}
