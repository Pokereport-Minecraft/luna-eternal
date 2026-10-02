package net.pokereport.luna.test;

import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.OperatorEntry;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.pokereport.luna.LunaConfig;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.world.LunaDimensions;
import net.pokereport.luna.world.WorldEditPermissions;
import net.pokereport.luna.world.WorldProtectionManager;

/** Actual mixin/event checks with an unconnected synthetic player, restricted to staging. */
public final class ProtectionSelfTest {
    private static final UUID ID = UUID.fromString("00000000-0000-3000-8000-000000009999");
    private static int passed, failed;
    private ProtectionSelfTest() {}

    private static void check(String name, boolean result) {
        if (result) passed++; else failed++;
        LunaEternal.LOG.info("[PROTECTION-QA] {} {}", result ? "PASS" : "FAIL", name);
    }

    public static int run(net.minecraft.server.command.ServerCommandSource source) {
        passed = 0; failed = 0;
        MinecraftServer server = source.getServer();
        Path permissions = Path.of("config", "lunaeternal", "world-edit.properties");
        byte[] original = null;
        var profile = new GameProfile(ID, "__worldqa");
        var world = server.getWorld(LunaDimensions.CIUDADELA);
        BlockPos pos = new BlockPos(-50000, 100, -50000);
        net.minecraft.block.BlockState previous = null;
        ServerPlayerEntity player = null;
        try {
            if (source.getEntity() != null || !LunaConfig.load().jdbcUrl.contains("/prelaunch_20261002?")) {
                source.sendError(Text.literal("Este ensayo solo se permite desde consola en staging aislado."));
                return 0;
            }
            if (world == null || server.getPlayerManager().getOpList().get(profile) != null) {
                source.sendError(Text.literal("Dimensión ausente o UUID de ensayo ya utilizado."));
                return 0;
            }
            original = Files.readAllBytes(permissions);
            Files.writeString(permissions, new String(original, java.nio.charset.StandardCharsets.UTF_8) + "\n" + ID + "="
                    + WorldEditPermissions.EDIT + "," + WorldEditPermissions.MODE + "," + WorldEditPermissions.COMMANDS + "\n");
            WorldEditPermissions.load();
            world.getChunk(pos);
            previous = world.getBlockState(pos);
            world.setBlockState(pos, Blocks.STONE.getDefaultState());
            player = new ServerPlayerEntity(server, world, profile, SyncedClientOptions.createDefault());
            player.networkHandler = new ServerPlayNetworkHandler(server, new ClientConnection(NetworkSide.SERVERBOUND),
                    player, ConnectedClientData.createDefault(profile, false));
            player.setPosition(pos.getX() + 3, pos.getY() + 2, pos.getZ() + 3);
            player.changeGameMode(GameMode.SURVIVAL);
            check("Player Survival no edita", !WorldProtectionManager.canModifyWorld(player, world));
            check("Fabric BEFORE cancela rotura normal", !PlayerBlockBreakEvents.BEFORE.invoker()
                    .beforeBlockBreak(world, player, pos, world.getBlockState(pos), null));
            var context = new ItemPlacementContext(player, Hand.MAIN_HAND, new ItemStack(Items.STONE),
                    new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false));
            check("BlockItem.place rechaza colocación directa", !((BlockItem) Items.STONE).place(context).isAccepted());
            check("World.breakBlock cancela destrucción ambiental", !world.breakBlock(pos, false, null, 0)
                    && world.getBlockState(pos).isOf(Blocks.STONE));
            world.setBlockState(pos, Blocks.CHEST.getDefaultState());
            check("Cofre administrativo cerrado", !net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker()
                    .interact(player, world, Hand.MAIN_HAND, new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false)).isAccepted()
                    && !net.pokereport.luna.world.SafeInteractionPolicy.canInteract(player, world, pos));
            world.setBlockState(pos, Blocks.OAK_DOOR.getDefaultState());
            check("Puerta conserva interacción", net.pokereport.luna.world.SafeInteractionPolicy.canInteract(player, world, pos));
            world.setBlockState(pos, Blocks.STONE.getDefaultState());
            player.setHealth(20);
            check("Daño lava cancelado", !player.damage(world.getDamageSources().lava(), 100) && player.getHealth() == 20);
            check("Daño caída cancelado", !player.damage(world.getDamageSources().fall(), 100) && player.getHealth() == 20);
            check("Daño vacío cancelado", !player.damage(world.getDamageSources().outOfWorld(), 100) && player.getHealth() == 20);
            player.getHungerManager().setFoodLevel(10);
            player.getHungerManager().setExhaustion(10);
            player.getHungerManager().update(player);
            check("Hambre estable en Ciudadela", player.getHungerManager().getFoodLevel() == 20
                    && player.getHungerManager().getExhaustion() == 0);
            var stand = new net.minecraft.entity.decoration.ArmorStandEntity(world, pos.getX(), pos.getY(), pos.getZ());
            check("Armor stand resiste daño sin permiso", !stand.damage(world.getDamageSources().playerAttack(player), 100));
            world.setBlockState(pos, Blocks.FARMLAND.getDefaultState());
            FarmlandBlock.setToDirt(player, world.getBlockState(pos), world, pos);
            check("Farmland no se convierte en tierra", world.getBlockState(pos).isOf(Blocks.FARMLAND));
            world.setBlockState(pos, Blocks.STONE.getDefaultState());
            world.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(), 4, World.ExplosionSourceType.TNT);
            check("Explosión conserva terreno", world.getBlockState(pos).isOf(Blocks.STONE));
            player.changeGameMode(GameMode.CREATIVE);
            check("Player Creative no edita", !WorldProtectionManager.canModifyWorld(player, world));
            server.getPlayerManager().getOpList().add(new OperatorEntry(profile, 2, false));
            check("OP2 Creative con nodos no edita", !WorldProtectionManager.canModifyWorld(player, world));
            server.getCommandManager().executeWithPrefix(player.getCommandSource(), "/fill -50000 100 -50000 -50000 100 -50000 dirt");
            check("OP2 no evade con /fill", world.getBlockState(pos).isOf(Blocks.STONE));
            server.getPlayerManager().getOpList().add(new OperatorEntry(profile, 4, false));
            player.changeGameMode(GameMode.SURVIVAL);
            check("OP4 autorizado Survival no edita", !WorldProtectionManager.canModifyWorld(player, world));
            player.changeGameMode(GameMode.CREATIVE);
            check("OP4 Creative sin editmode no edita", !WorldProtectionManager.canModifyWorld(player, world));
            WorldProtectionManager.setEditing(player, true);
            check("OP4 autorizado Creative EditMode edita", WorldProtectionManager.canModifyWorld(player, world));
            // CommandManager queues nested commands until this outer selftest returns.
            // The direct dispatcher runs the authorized vanilla operation synchronously,
            // so cleanup cannot race a deferred write after the assertion.
            server.getCommandManager().getDispatcher().execute("setblock -50000 100 -50000 dirt", player.getCommandSource());
            check("Edición autorizada /setblock funciona", world.getBlockState(pos).isOf(Blocks.DIRT));
            check("Hogar conserva permiso Survival", WorldProtectionManager.canModifyWorld(player, server.getOverworld()));
        } catch (Exception | LinkageError e) {
            failed++;
            LunaEternal.LOG.error("[PROTECTION-QA] FAIL excepción de ensayo", e);
        } finally {
            try {
                if (player != null) WorldProtectionManager.setEditing(player, false);
                if (previous != null) world.setBlockState(pos, previous);
                if (original != null) {
                    Files.write(permissions, original);
                    WorldEditPermissions.load();
                    server.getPlayerManager().getOpList().remove(profile);
                }
            } catch (Exception e) {
                failed++;
                LunaEternal.LOG.error("[PROTECTION-QA] FAIL limpieza", e);
            }
        }
        LunaEternal.LOG.info("[PROTECTION-QA] FINAL passed={} failed={}", passed, failed);
        source.sendFeedback(() -> Text.literal("Protección QA: " + passed + " PASS / " + failed + " FAIL"), false);
        return failed == 0 ? passed : 0;
    }
}
