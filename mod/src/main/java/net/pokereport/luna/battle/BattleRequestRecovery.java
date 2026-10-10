package net.pokereport.luna.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import net.pokereport.luna.LunaEternal;

/** Recover a complete request; never fabricate passes for live attacks or required switches. */
public final class BattleRequestRecovery {
    private BattleRequestRecovery() {}

    public static void choose(AIBattleActor actor, ShowdownActionRequest request) {
        if (!actor.getResponses().isEmpty() || request == null || request.getWait()
                || actor.getRequest() != request) return;
        var choices = request.iterate(actor.getActivePokemon(), (active, moves, forced) -> {
            try {
                return actor.getBattleAI().choose(active, actor.getBattle(), actor.getSide(), moves, forced);
            } catch (RuntimeException ex) {
                var response = TrainerBattleAI.forcedChoice(active, actor.getSide(), moves, forced);
                if (response == null) throw ex;
                return response;
            }
        });
        int expected = Math.max(request.getActive() == null ? 0 : request.getActive().size(), request.getForceSwitch().size());
        if (choices == null || choices.size() != expected
                || choices.isEmpty() || choices.contains(null)) {
            throw new IllegalStateException("Incomplete AI request response");
        }
        actor.setActionResponses(choices);
        for (var pokemon : actor.getPokemonList()) pokemon.setWillBeSwitchedIn(false);
        actor.setMustChoose(false);
    }

    public static void abort(PokemonBattle battle, Throwable cause) {
        LunaEternal.LOG.error("[BATTLE-ISOLATION] Closing failed battle {} without victory", battle.getBattleId(), cause);
        try {
            try { battle.saveBattleLog(); }
            catch (RuntimeException logging) { LunaEternal.LOG.warn("[BATTLE-ISOLATION] Could not save protocol for {}", battle.getBattleId(), logging); }
            if (!battle.getEnded()) battle.end();
            for (var player : battle.getPlayers()) {
                var assignment = net.pokereport.luna.gym.Ranuras.asignacionDe(player.getUuid());
                if (assignment != null) {
                    player.sendMessage(net.minecraft.text.Text.literal("§cEl combate se cerró por un fallo técnico; no se otorga progreso."), false);
                    net.pokereport.luna.gym.Programador.en(20, () ->
                            net.pokereport.luna.gym.Combate.devolver(player, assignment.gymId()));
                }
            }
        } catch (RuntimeException | LinkageError | StackOverflowError cleanup) {
            LunaEternal.LOG.error("[BATTLE-ISOLATION] Cleanup failed for {}", battle.getBattleId(), cleanup);
        }
    }
}
