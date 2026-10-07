package net.pokereport.luna.world;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Server-side policy shared by map protection and player safety. */
public final class WorldProtectionManager {
    private static final Set<RegistryKey<World>> SAFE = Set.of(
            LunaDimensions.CIUDADELA, LunaDimensions.LOBBY, LunaDimensions.GIMNASIOS,
            LunaDimensions.GIMNASIO_MORTI, LunaDimensions.TORRE, LunaDimensions.ISLAS_NARANJA);
    private static final Set<UUID> EDITING = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private record SafePosition(RegistryKey<World> world, Vec3d position) {}
    private static final Map<UUID, SafePosition> LAST_SAFE = new HashMap<>();

    private WorldProtectionManager() {}

    public static boolean isProtectedDimension(World world) {
        return isProtectedDimension(world.getRegistryKey());
    }

    public static boolean isProtectedDimension(RegistryKey<World> key) {
        return SAFE.contains(key);
    }

    /** Existing superior staff level AND explicit granular grants; OP alone never suffices. */
    public static boolean hasAdministrativeEditAccess(ServerPlayerEntity player) {
        return player.hasPermissionLevel(4)
                && WorldEditPermissions.allowed(player.getUuid(), WorldEditPermissions.EDIT)
                && WorldEditPermissions.allowed(player.getUuid(), WorldEditPermissions.MODE);
    }

    public static boolean canModifyWorld(ServerPlayerEntity player, World world) {
        return !isProtectedDimension(world) || (hasAdministrativeEditAccess(player)
                && player.isCreative() && EDITING.contains(player.getUuid()));
    }

    public static boolean editing(ServerPlayerEntity player) {
        return EDITING.contains(player.getUuid()) && hasAdministrativeEditAccess(player);
    }

    public static void setEditing(ServerPlayerEntity player, boolean enabled) {
        if (enabled && !hasAdministrativeEditAccess(player)) {
            throw new IllegalStateException("El rango no permite editar mapas");
        }
        if (enabled) EDITING.add(player.getUuid());
        else EDITING.remove(player.getUuid());
    }

    private static boolean modifiesWorld(Item item) {
        return item instanceof BlockItem || item instanceof BucketItem
                || item instanceof BoatItem || item instanceof MinecartItem
                || item instanceof FlintAndSteelItem || item instanceof FireChargeItem
                || item instanceof SpawnEggItem || item instanceof EndCrystalItem
                || item instanceof BoneMealItem || item instanceof HoeItem
                || item instanceof ShovelItem || item instanceof AxeItem
                || item instanceof ShearsItem || item instanceof ArmorStandItem
                || item instanceof DecorationItem;
    }

    public static void registrar() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            WorldEditPermissions.load();
            StaffRoles.load();
            SafeInteractionPolicy.load();
            WorldEditAudit.start();
        });
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                !(player instanceof ServerPlayerEntity sp) || canModifyWorld(sp, world));
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity sp) WorldEditAudit.block(sp, pos, "BREAK", state, world.getBlockState(pos));
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity sp
                    && !canModifyWorld(sp, world) && !SafeInteractionPolicy.usesPublicBlock(sp, world, hit.getBlockPos())
                    && modifiesWorld(sp.getStackInHand(hand).getItem())) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            var stack = player.getStackInHand(hand);
            if (!world.isClient() && player instanceof ServerPlayerEntity sp
                    && !canModifyWorld(sp, world) && modifiesWorld(stack.getItem())) {
                return TypedActionResult.fail(stack);
            }
            return TypedActionResult.pass(stack);
        });
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (player instanceof ServerPlayerEntity sp && !canModifyWorld(sp, world)
                    && (entity instanceof net.minecraft.entity.decoration.AbstractDecorationEntity
                    || entity instanceof net.minecraft.entity.decoration.ArmorStandEntity)) return ActionResult.FAIL;
            return ActionResult.PASS;
        });
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (player instanceof ServerPlayerEntity sp && !canModifyWorld(sp, world)
                    && (entity instanceof net.minecraft.entity.decoration.ItemFrameEntity
                    || entity instanceof net.minecraft.entity.decoration.ArmorStandEntity)) return ActionResult.FAIL;
            return ActionResult.PASS;
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof ServerPlayerEntity player) || !isProtectedDimension(player.getWorld()));
        // Last defence if a mod invokes death directly instead of PlayerEntity.damage.
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player && isProtectedDimension(player.getWorld())) {
                player.setHealth(Math.max(1.0F, player.getMaxHealth()));
                player.extinguish();
                player.fallDistance = 0;
                return false;
            }
            return true;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            EDITING.remove(handler.player.getUuid());
            LAST_SAFE.remove(handler.player.getUuid());
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            EDITING.clear();
            LAST_SAFE.clear();
            WorldEditAudit.close();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                var world = player.getServerWorld();
                if (!isProtectedDimension(world)) {
                    LAST_SAFE.remove(player.getUuid());
                    continue;
                }
                BlockPos feet = player.getBlockPos();
                if (player.isOnGround() && world.getBlockState(feet.down()).isSolidBlock(world, feet.down())
                        && world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                        && world.getBlockState(feet.up()).getCollisionShape(world, feet.up()).isEmpty()) {
                    LAST_SAFE.put(player.getUuid(), new SafePosition(world.getRegistryKey(), player.getPos()));
                }
                if (player.getY() < world.getBottomY() + 8) {
                    SafePosition safe = LAST_SAFE.get(player.getUuid());
                    if (safe != null && safe.world().equals(world.getRegistryKey())) {
                        Traslado.ir(player, world, safe.position(), player.getYaw(), player.getPitch());
                    } else {
                        Traslado.ir(player, world, TravelService.safeSpawn(world), player.getYaw(), player.getPitch());
                    }
                    player.fallDistance = 0;
                    player.extinguish();
                }
            }
        });
    }
}
