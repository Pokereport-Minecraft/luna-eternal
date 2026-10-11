package net.pokereport.luna.test;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.*;
import com.cobblemon.mod.common.battles.actor.TrainerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import kotlin.Unit;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.pokereport.luna.battle.TrainerBattleAI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Native Cobblemon fixtures: no players, registry entries, entities, rewards or SQL writes. */
public final class BattleTurnRegression {
    private BattleTurnRegression() {}

    public static int run(ServerCommandSource source) {
        try {
            var config = net.pokereport.luna.LunaConfig.load();
            if (source.getEntity() != null || !source.hasPermissionLevel(4)
                    || !config.jdbcUrl.contains("/prelaunch_20261002?")) {
                source.sendError(Text.literal("Battle regression is restricted to isolated staging console."));
                return 0;
            }
            int passed = 0;
            var actor = actor("arcanine", "magmar");
            var other = actor("aerodactyl", "donphan");
            var battle = fixture(actor, other);
            AtomicInteger executions = new AtomicInteger();
            boolean[] released = {false};
            var barrier = (com.cobblemon.mod.common.battles.dispatch.DispatchResult) () -> released[0];
            battle.setStarted(true);
            battle.setDispatchResult(barrier);
            battle.dispatchGo(() -> { executions.incrementAndGet(); return Unit.INSTANCE; });
            for (int tick = 0; tick < 250; tick++) battle.tick();
            boolean preserved = executions.get() == 0 && battle.getDispatchResult() == barrier
                    && battle.getDispatches().size() == 1;
            report(source, "native_wait_barrier_250_ticks", preserved);
            if (preserved) passed++;
            released[0] = true;
            battle.tick();
            boolean once = executions.get() == 1 && battle.getDispatches().isEmpty();
            report(source, "native_barrier_release_executes_once", once);
            if (once) passed++;
            battle.setStarted(false);

            var active = new ActiveBattlePokemon(actor, null);
            actor.getActivePokemon().clear();
            actor.getActivePokemon().add(active);
            // Local animation has not installed Arcanine yet; Showdown already has.
            var side = new ShowdownSide();
            side.setName(actor.getUuid()); side.setId("p1");
            var rows = new ArrayList<ShowdownPokemon>();
            for (int i = 0; i < actor.getPokemonList().size(); i++) {
                var p = actor.getPokemonList().get(i);
                var row = new ShowdownPokemon();
                row.setIdent("p1: " + p.getUuid());
                row.setDetails(p.getOriginalPokemon().getSpecies().getName() + ", " + p.getUuid());
                row.setCondition("100/100"); row.setActive(i == 0); rows.add(row);
            }
            side.setPokemon(rows);
            actor.setRequest(new ShowdownActionRequest(false, null, List.of(true), true, side));
            var selected = TrainerBattleAI.forcedChoice(active, actor.getSide(), null, true);
            boolean legal = selected instanceof SwitchActionResponse s
                    && s.getNewPokemonId().equals(actor.getPokemonList().get(1).getUuid());
            report(source, "showdown_active_excluded_during_animation", legal);
            if (legal) passed++;
            var doublesActor = actor("arcanine", "magmar", "ninetales", "rapidash");
            var doublesBattle = fixture(doublesActor, actor("blastoise", "lapras"));
            doublesActor.getActivePokemon().clear();
            doublesActor.getActivePokemon().add(new ActiveBattlePokemon(doublesActor, doublesActor.getPokemonList().get(0)));
            doublesActor.getActivePokemon().add(new ActiveBattlePokemon(doublesActor, doublesActor.getPokemonList().get(1)));
            var doublesSide = new ShowdownSide();
            doublesSide.setName(doublesActor.getUuid()); doublesSide.setId("p1");
            var doublesRows = new ArrayList<ShowdownPokemon>();
            for (int i = 0; i < doublesActor.getPokemonList().size(); i++) {
                var pokemon = doublesActor.getPokemonList().get(i);
                var row = new ShowdownPokemon();
                row.setIdent("p1: " + pokemon.getUuid());
                row.setDetails(pokemon.getOriginalPokemon().getSpecies().getName() + ", " + pokemon.getUuid());
                row.setCondition("100/100"); row.setActive(i < 2); doublesRows.add(row);
            }
            doublesSide.setPokemon(doublesRows);
            doublesActor.setRequest(new ShowdownActionRequest(false, null, List.of(true, true), true, doublesSide));
            var lifecycle = (net.pokereport.luna.battle.BattleChoiceRecovery)doublesActor;
            boolean beforeUpdate = !lifecycle.pokeReport$isRequestReady();
            doublesActor.postUpdate();
            boolean afterUpdate = lifecycle.pokeReport$isRequestReady();
            var processedRequest = doublesActor.getRequest();
            doublesActor.setRequest(new ShowdownActionRequest(false, null, List.of(true, true), true, null));
            boolean newRequestWaits = !lifecycle.pokeReport$isRequestReady();
            doublesActor.setRequest(processedRequest);
            report(source, "request_requires_its_own_completed_update", beforeUpdate && afterUpdate && newRequestWaits);
            if (beforeUpdate && afterUpdate && newRequestWaits) passed++;
            doublesActor.setRequest(new ShowdownActionRequest(false, null, List.of(), false, null));
            doublesActor.postUpdate();
            boolean regularReady = lifecycle.pokeReport$isRequestReady();
            report(source, "normal_request_activates_at_early_postUpdate_return", regularReady);
            if (regularReady) passed++;
            doublesActor.setRequest(processedRequest);
            doublesBattle.setDispatchResult(() -> false);
            doublesActor.onChoiceRequested();
            boolean deferred = doublesActor.getResponses().isEmpty();
            report(source, "native_ai_defers_choice_during_animation", deferred);
            if (deferred) passed++;
            boolean rejected = false;
            try {
                doublesActor.setActionResponses(List.of(
                        new SwitchActionResponse(doublesActor.getPokemonList().get(2).getUuid()),
                        new SwitchActionResponse(doublesActor.getPokemonList().get(0).getUuid())));
            } catch (com.cobblemon.mod.common.exception.IllegalActionChoiceException expected) { rejected = true; }
            boolean atomic = rejected && doublesActor.getResponses().isEmpty();
            report(source, "native_double_choice_rejection_is_atomic", atomic);
            if (atomic) passed++;
            int[] adaptation = {0, 0};
            TrainerAdaptationSelfTest.run((name, success) -> {
                adaptation[success ? 0 : 1]++;
                if (!success) report(source, name, false);
            });
            source.sendFeedback(() -> Text.literal("[TRAINER-ADAPTATION] PASS=" + adaptation[0]
                    + " FAIL=" + adaptation[1]), false);
            source.sendFeedback(() -> Text.literal("[BATTLE-REGRESSION] fixtures complete; no production players touched."), false);
            return passed == 7 && adaptation[1] == 0 ? 1 : 0;
        } catch (RuntimeException | java.io.IOException failure) {
            net.pokereport.luna.LunaEternal.LOG.error("[BATTLE-REGRESSION] fixture failed", failure);
            source.sendError(Text.literal("[BATTLE-REGRESSION] ERROR " + failure));
            return 0;
        }
    }

    private static void report(ServerCommandSource source, String name, boolean success) {
        source.sendFeedback(() -> Text.literal("[BATTLE-REGRESSION] " + name + " " + (success ? "PASS" : "FAIL")), false);
    }

    private static TrainerBattleActor actor(String... species) {
        var team = new ArrayList<BattlePokemon>();
        for (String name : species) {
            var p = PokemonProperties.Companion.parse(name + " level=42", " ", "=").create();
            team.add(new BattlePokemon(p, p, entity -> Unit.INSTANCE));
        }
        return new TrainerBattleActor("Blaine regression", UUID.randomUUID(), team, new TrainerBattleAI(4));
    }

    private static PokemonBattle fixture(TrainerBattleActor a, TrainerBattleActor b) {
        var side1 = new BattleSide(a); var side2 = new BattleSide(b);
        var battle = new PokemonBattle(com.gitlab.srcmc.rctapi.api.battle.BattleFormat.GEN_9_SINGLES.getCobblemonBattleFormat(), side1, side2);
        side1.setBattle(battle); side2.setBattle(battle);
        a.setBattle(battle); b.setBattle(battle); a.setShowdownId("p1"); b.setShowdownId("p2");
        for (var actor : List.of(a, b)) for (var p : actor.getPokemonList()) p.setActor(actor);
        return battle;
    }
}
