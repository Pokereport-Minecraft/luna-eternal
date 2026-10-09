package net.pokereport.luna.gym;

import com.cobblemon.mod.common.battles.BattleRegistry;
import com.gitlab.srcmc.rctmod.api.RCTMod;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import net.minecraft.server.MinecraftServer;
import net.pokereport.luna.LunaEternal;

/** Inspect the NPC's owner before resetting; the clicking player may be unrelated. */
public final class TrainerStateRecovery {
    private TrainerStateRecovery() {}
    private static final java.util.Set<java.util.UUID> SUSPECTS = new java.util.HashSet<>();
    private static final java.util.Set<TrainerMob> LOADED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void register() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof TrainerMob mob) LOADED.add(mob);
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof TrainerMob mob) {
                LOADED.remove(mob);
                SUSPECTS.remove(mob.getUuid());
            }
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LOADED.clear();
            SUSPECTS.clear();
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(TrainerStateRecovery::tick);
    }

    public static void tick(MinecraftServer server) {
        if (server.getTicks() % 200 != 0) return;
        var seen = new java.util.HashSet<java.util.UUID>();
        for (var mob : LOADED) {
                if (!mob.isRemoved() && mob.isInBattle()) {
                    seen.add(mob.getUuid());
                    // Two observations avoid resetting an NPC while a battle is being registered.
                    if (SUSPECTS.contains(mob.getUuid())) available(mob, server);
                }
        }
        SUSPECTS.clear();
        SUSPECTS.addAll(seen);
    }

    public static boolean available(TrainerMob mob, MinecraftServer server) {
        if (server == null) return false;
        try {
            var owners = new java.util.HashSet<java.util.UUID>();
            if (mob.getOpponent() != null) owners.add(mob.getOpponent().getUuid());
            var manager = RCTMod.getInstance().getTrainerManager();
            for (var player : server.getPlayerManager().getPlayerList()) {
                var tracked = manager.getBattle(player.getUuid());
                if (tracked.isPresent()) {
                    var match = tracked.get();
                    boolean belongs = match.getTrainerSideMobs().stream().anyMatch(m -> m.getUuid().equals(mob.getUuid()))
                            || match.getInitiatorSideMobs().stream().anyMatch(m -> m.getUuid().equals(mob.getUuid()));
                    if (belongs) owners.add(player.getUuid());
                }
            }
            for (var owner : owners) {
                var battle = BattleRegistry.getBattleByParticipatingPlayerId(owner);
                if (battle != null && !battle.getEnded()) return false;
            }
            if (mob.isInBattle() || !owners.isEmpty()) {
                mob.cancelBattle();
                for (var owner : owners) manager.removeBattle(owner);
                LunaEternal.LOG.warn("[TRAINER-RECOVERY] Cleared orphan trainer {} owners={}", mob.getUuid(), owners);
            }
            return true;
        } catch (RuntimeException | LinkageError error) {
            LunaEternal.LOG.error("[TRAINER-RECOVERY] Cannot verify trainer {}", mob.getUuid(), error);
            return false;
        }
    }
}
