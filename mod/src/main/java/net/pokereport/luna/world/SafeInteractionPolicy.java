package net.pokereport.luna.world;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.pokereport.luna.LunaEternal;

/** Runs after deliberate memorial/shop handlers; ordinary administrative containers remain closed. */
public final class SafeInteractionPolicy {
    private static Set<String> allowedContainers = Set.of();
    private SafeInteractionPolicy() {}

    /** A normal click on a door operates the block, even with building items in hand. */
    public static boolean usesDoor(net.minecraft.entity.player.PlayerEntity player, World world, BlockPos pos) {
        var block = world.getBlockState(pos).getBlock();
        return !player.isSneaking() && (block instanceof net.minecraft.block.DoorBlock
                || block instanceof net.minecraft.block.TrapdoorBlock
                || block instanceof net.minecraft.block.FenceGateBlock);
    }

    /** Public gameplay blocks operate normally; placement is still denied by BlockItem.place. */
    public static boolean publicGameplay(String id) {
        if(id.equals("cobblemon:pc")||id.equals("cobblemon:healing_machine"))return true;
        return id.startsWith("handcrafted:") && (id.endsWith("_chair")||id.endsWith("_bench")||id.endsWith("_couch"));
    }
    public static boolean usesPublicBlock(net.minecraft.entity.player.PlayerEntity player,World world,BlockPos pos){
        return usesDoor(player,world,pos)||(!player.isSneaking()&&publicGameplay(net.minecraft.registry.Registries.BLOCK.getId(world.getBlockState(pos).getBlock()).toString()));
    }

    public static void load() {
        allowedContainers = Set.of();
        Path file = Path.of("config", "lunaeternal", "interactions.properties");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, "# Explicit public containers only: namespace:dimension@x,y,z=true\n");
            }
            Properties config = new Properties();
            try (var input = Files.newInputStream(file)) { config.load(input); }
            var parsed = new java.util.HashSet<String>();
            for (String key : config.stringPropertyNames()) {
                if (!key.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+@-?\\d+,-?\\d+,-?\\d+")) {
                    throw new IllegalArgumentException("Invalid public container coordinate");
                }
                if (Boolean.parseBoolean(config.getProperty(key))) parsed.add(key);
            }
            allowedContainers = Set.copyOf(parsed);
        } catch (Exception e) {
            LunaEternal.LOG.error("Interacciones sensibles: configuración inválida; contenedores cerrados", e);
        }
    }

    public static boolean canInteract(ServerPlayerEntity player, World world, BlockPos pos) {
        if (WorldProtectionManager.canModifyWorld(player, world)) return true;
        var state = world.getBlockState(pos);
        if (state.isOf(Blocks.COMMAND_BLOCK) || state.isOf(Blocks.CHAIN_COMMAND_BLOCK)
                || state.isOf(Blocks.REPEATING_COMMAND_BLOCK) || state.isOf(Blocks.STRUCTURE_BLOCK)
                || state.isOf(Blocks.JIGSAW)) return false;
        if (publicGameplay(net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()).toString())) return true;
        if (world.getBlockEntity(pos) instanceof Inventory) {
            return allowedContainers.contains(world.getRegistryKey().getValue() + "@"
                    + pos.getX() + "," + pos.getY() + "," + pos.getZ());
        }
        return true;
    }

    public static void registrar() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity sp
                    && !canInteract(sp, world, hit.getBlockPos())) return ActionResult.FAIL;
            return ActionResult.PASS;
        });
    }
}
