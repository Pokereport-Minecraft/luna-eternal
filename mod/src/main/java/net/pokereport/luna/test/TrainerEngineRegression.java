package net.pokereport.luna.test;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.gitlab.srcmc.rctapi.api.battle.BattleRules;
import com.gitlab.srcmc.rctapi.api.models.BagItemModel;
import com.gitlab.srcmc.rctapi.api.models.PokemonModel;
import com.gitlab.srcmc.rctapi.api.models.TrainerModel;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC;
import com.gitlab.srcmc.rctapi.api.util.JTO;
import com.gitlab.srcmc.rctmod.ModCommon;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.pokereport.luna.battle.TrainerBattleAI;
import net.pokereport.luna.gym.*;
import java.util.*;

/** Full RCT + Cobblemon + Showdown, using temporary entities in isolated staging. */
public final class TrainerEngineRegression {
    private static boolean running;
    private static int finishedLanes;
    private static int totalPassed;
    private TrainerEngineRegression() {}

    public static int run(ServerCommandSource source) {
        try {
            var config = net.pokereport.luna.LunaConfig.load();
            if (source.getEntity() != null || !source.hasPermissionLevel(4)
                    || !config.jdbcUrl.contains("/prelaunch_20261002?")) {
                source.sendError(Text.literal("Trainer engine QA requires isolated staging console."));
                return 0;
            }
            if (running) { source.sendError(Text.literal("Trainer engine QA already running.")); return 0; }
            running = true;
            finishedLanes = 0; totalPassed = 0;
            for (int lane = 0; lane < 3; lane++) next(source, lane, 0);
            return 1;
        } catch (java.io.IOException ex) {
            source.sendError(Text.literal("Cannot verify staging: " + ex)); return 0;
        }
    }

    private static void next(ServerCommandSource source, int index, int passed) {
        if (index >= Gimnasio.TODOS.size()) {
            totalPassed += passed;
            if (++finishedLanes == 3) {
                running = false;
                net.pokereport.luna.LunaEternal.LOG.info("[TRAINER-ENGINE-QA] COMPLETE {}/{}", totalPassed, Gimnasio.TODOS.size());
                source.sendFeedback(() -> Text.literal("[TRAINER-ENGINE-QA] COMPLETE " + totalPassed + "/" + Gimnasio.TODOS.size()), false);
            }
            return;
        }
        var gym = Gimnasio.TODOS.get(index);
        var registry = ModCommon.RCT.getTrainerRegistry();
        var entities = new ArrayList<ArmorStandEntity>();
        var ids = new ArrayList<String>();
        try {
            var party = new ArrayList<com.cobblemon.mod.common.pokemon.Pokemon>();
            for (var species : List.of("blastoise", "arcanine", "venusaur", "gengar", "snorlax", "dragonite")) {
                party.add(PokemonProperties.Companion.parse(species + " level=" + gym.nivel()).create());
            }
            var leaderModels = Adaptador.prepararEquipo(gym, party);
            var challengerModels = Adaptador.prepararRivalMixto(
                    List.of("blastoise", "arcanine", "venusaur", "gengar", "snorlax", "dragonite"), party, gym.nivel());
            TrainerNPC leader = npc(source, gym.id(), leaderModels, gym.pericia(), index % 3, entities, ids);
            TrainerNPC challenger = npc(source, "challenger", challengerModels, 4, index % 3, entities, ids);
            var format = Gimnasio.esCombateDoble(gym)
                    ? com.gitlab.srcmc.rctapi.api.battle.BattleFormat.GEN_9_DOUBLES
                    : com.gitlab.srcmc.rctapi.api.battle.BattleFormat.GEN_9_SINGLES;
            var battleId = startNpcFixture(challenger, leader, format);
            if (battleId == null) throw new IllegalStateException("RCT startBattle failed");
            var battle = BattleRegistry.getBattle(battleId);
            net.pokereport.luna.LunaEternal.LOG.info("[TRAINER-ENGINE-QA] START {} {}", gym.id(), battleId);
            source.sendFeedback(() -> Text.literal("[TRAINER-ENGINE-QA] START " + gym.id() + " " + battleId), false);
            observe(source, battle, index, passed, 0, entities, ids);
        } catch (RuntimeException | LinkageError ex) {
            net.pokereport.luna.LunaEternal.LOG.error("[TRAINER-ENGINE-QA] START FAIL {}", gym.id(), ex);
            cleanup(entities, ids);
            Programador.en(20, () -> next(source, index + 3, passed));
        }
    }

    /**
     * RCT explicitly requires a human as primary actor. For this console-only
     * fixture remove ONLY that error, retaining all entity/team/format checks.
     * Restore the validator synchronously before returning to the server tick.
     * This exercises real RCT actors/animations but does not claim human GUI QA.
     */
    private static UUID startNpcFixture(TrainerNPC challenger, TrainerNPC leader,
                com.gitlab.srcmc.rctapi.api.battle.BattleFormat format) {
        var manager = ModCommon.RCT.getBattleManager();
        try {
            var field = manager.getClass().getDeclaredField("validator");
            field.setAccessible(true);
            var original = field.get(manager);
            field.set(manager, new com.gitlab.srcmc.rctapi.api.battle.BattleContextValidator() {
                @Override public com.cobblemon.mod.common.battles.ErroredBattleStart validate(
                        com.cobblemon.mod.common.battles.ErroredBattleStart errors,
                        com.gitlab.srcmc.rctapi.api.battle.BattleContext context) {
                    var result = super.validate(errors, context);
                    var primary = context.getBattleSide1().getActors()[0];
                    String exempt = "Primary actor '" + primary.getName().getString() + "' is not a player";
                    result.getParticipantErrors().get(primary).removeIf(e ->
                            e.getMessageFor(challenger.getEntity()).getString().equals(exempt));
                    for (var error : result.getErrors()) {
                        net.pokereport.luna.LunaEternal.LOG.error("[TRAINER-ENGINE-QA] validation {}",
                                error.getMessageFor(challenger.getEntity()).getString());
                    }
                    return result;
                }
            });
            try {
                return manager.startBattle(List.of(challenger), List.of(leader), format,
                        new BattleRules.Builder().withMaxItemUses(0).build());
            } finally { field.set(manager, original); }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("RCT fixture API changed", ex);
        }
    }

    private static TrainerNPC npc(ServerCommandSource source, String name, List<PokemonModel> models, int skill, int lane,
                                  List<ArmorStandEntity> entities, List<String> ids) {
        var world = source.getServer().getOverworld();
        var spawn = world.getSpawnPos();
        double x = spawn.getX() + lane * 4 + (entities.isEmpty() ? 3 : -3);
        var entity = new ArmorStandEntity(world, x, spawn.getY() + 2, spawn.getZ());
        entity.setNoGravity(true); entity.setInvulnerable(true);
        world.getChunk(entity.getBlockPos());
        world.spawnEntity(entity); entities.add(entity);
        String id = "luna_regression_" + UUID.randomUUID(); ids.add(id);
        var model = new TrainerModel(com.gitlab.srcmc.rctapi.api.util.Text.literal("QA " + name),
                JTO.of(() -> new TrainerBattleAI(Math.max(4, skill))), List.<BagItemModel>of(), models);
        var npc = ModCommon.RCT.getTrainerRegistry().registerNPC(id, model);
        npc.setEntity(entity);
        return npc;
    }

    private static void observe(ServerCommandSource source, PokemonBattle battle, int index, int passed, int seconds,
                                List<ArmorStandEntity> entities, List<String> ids) {
        Programador.en(20, () -> {
            if (!battle.getEnded() && seconds < 600) {
                if (seconds % 30 == 0) {
                    var states = new ArrayList<String>();
                    for (var actor : battle.getActors()) {
                        var req = actor.getRequest();
                        states.add(actor.getShowdownId() + ":req=" + (req == null ? "null" : req.getWait() ? "wait" : req.getForceSwitch())
                                + ",choose=" + actor.getMustChoose() + ",responses=" + actor.getResponses().size());
                    }
                    net.pokereport.luna.LunaEternal.LOG.info("[TRAINER-ENGINE-QA] {} t={} seconds={} queue={} barrier={} actors={}",
                            Gimnasio.TODOS.get(index).id(), battle.getTurn(), seconds, battle.getDispatches().size(),
                            battle.getDispatchResult().getClass().getSimpleName(), states);
                }
                observe(source, battle, index, passed, seconds + 1, entities, ids);
                return;
            }
            boolean success = battle.getEnded() && !battle.getWinners().isEmpty() && battle.getTurn() > 1;
            net.pokereport.luna.LunaEternal.LOG.info("[TRAINER-ENGINE-QA] {} {} turns={} id={}",
                    Gimnasio.TODOS.get(index).id(), success ? "PASS" : "FAIL", battle.getTurn(), battle.getBattleId());
            battle.saveBattleLog();
            if (!battle.getEnded()) battle.end();
            source.sendFeedback(() -> Text.literal("[TRAINER-ENGINE-QA] " + Gimnasio.TODOS.get(index).id()
                    + " " + (success ? "PASS" : "FAIL") + " turns=" + battle.getTurn() + " id=" + battle.getBattleId()), false);
            // RCT has a delayed end handler; allow it to recall clones before unregistering.
            Programador.en(80, () -> {
                cleanup(entities, ids);
                next(source, index + 3, passed + (success ? 1 : 0));
            });
        });
    }

    private static void cleanup(List<ArmorStandEntity> entities, List<String> ids) {
        for (var entity : entities) entity.discard();
        for (var id : ids) ModCommon.RCT.getTrainerRegistry().unregisterById(id);
    }
}
