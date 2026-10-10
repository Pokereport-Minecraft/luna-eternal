package net.pokereport.luna.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.ai.BattleAI;
import com.cobblemon.mod.common.battles.*;
import com.cobblemon.mod.common.battles.ai.StrongBattleAI;
import com.cobblemon.mod.common.battles.ai.RandomBattleAI;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.net.messages.client.battle.BattleHealthChangePacket;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.gym.Tipos;

/** One AI per battle. Forced switches reserve distinct, tactically suitable bench Pokemon. */
public final class TrainerBattleAI implements BattleAI {
    private final StrongBattleAI turns;
    private final RandomBattleAI fallback = new RandomBattleAI();
    public TrainerBattleAI(int skill) { turns = new StrongBattleAI(Math.max(0, Math.min(5, skill))); }

    public static ShowdownActionResponse forcedChoice(ActiveBattlePokemon active, BattleSide side,
                                                      ShowdownMoveset moves, boolean forced) {
        // Only the request flag decides which doubles slot must replace. A
        // fainted but non-required empty slot legitimately passes when no bench remains.
        if (!forced) return moves == null ? PassActionResponse.INSTANCE : null;
        BattlePokemon best = null;
        double score = -Double.MAX_VALUE;
        for (BattlePokemon p : active.getActor().getPokemonList()) {
            if (p.getHealth() <= 0 || p.isSentOut() || p.getWillBeSwitchedIn()) continue;
            var request = active.getActor().getRequest();
            if (request != null && request.getSide() != null) {
                // Showdown's roster is authoritative while switch animations lag behind.
                var row = request.getSide().getPokemon().stream()
                        .filter(entry -> entry.getUuid().equals(p.getUuid())).findFirst();
                if (row.isEmpty() || row.get().getActive()
                        || row.get().getCondition().contains("fnt")
                        || row.get().getCondition().startsWith("0/")) continue;
            }
            double candidate = matchup(p, side);
            if (candidate > score) { best = p; score = candidate; }
        }
        // Showdown permits pass only when no unreserved replacement exists
        // (e.g. two fainted doubles slots but just one available bench member).
        if (best == null) return PassActionResponse.INSTANCE;
        best.setWillBeSwitchedIn(true);
        return new SwitchActionResponse(best.getUuid());
    }

    private static double matchup(BattlePokemon p, BattleSide side) {
        String[] own = Tipos.deEspecie(p.getEffectedPokemon().getSpecies());
        double score = (double)p.getHealth() / Math.max(1, p.getMaxHealth());
        for (var enemy : side.getOppositeSide().getActivePokemon()) {
            if (!enemy.hasPokemon() || !enemy.isAlive()) continue;
            String[] target = Tipos.deEspecie(enemy.getBattlePokemon().getEffectedPokemon().getSpecies());
            double attack = 0, incoming = 0;
            for (var move : p.getMoveSet().getMoveTemplates()) {
                if (move.getPower() > 0) attack = Math.max(attack,
                        Tipos.contra(move.getElementalType().getName(), target[0], target[1]));
            }
            for (String type : target) if (type != null) incoming = Math.max(incoming, Tipos.contra(type, own[0], own[1]));
            score += attack - incoming * 0.6;
        }
        return score;
    }

    @Override public ShowdownActionResponse choose(ActiveBattlePokemon active, PokemonBattle battle,
                                                   BattleSide side, ShowdownMoveset moves, boolean forced) {
        var replacement = forcedChoice(active, side, moves, forced);
        if (replacement != null) return replacement;
        try { return turns.choose(active, battle, side, moves, false); }
        catch (RuntimeException e) {
            LunaEternal.LOG.warn("[BATTLE-AI] Tactical turn fallback battle={} slot={}: {}", battle.getBattleId(), active.getPNX(), e.toString());
            return fallback.choose(active, battle, side, moves, false);
        }
    }
    @Override public void onHealthChange(BattleHealthChangePacket packet) { turns.onHealthChange(packet); }
}
