package net.pokereport.luna.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.*;
import com.cobblemon.mod.common.battles.ai.StrongBattleAI;
import net.pokereport.luna.battle.TrainerBattleAI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Also protects trainers supplied by external datapacks, without changing their teams. */
@Mixin(value = StrongBattleAI.class, remap = false)
public abstract class StrongBattleAIReplacementMixin {
    @Inject(method = "choose", at = @At("HEAD"), cancellable = true)
    private void pokeReport$safeReplacement(ActiveBattlePokemon active, PokemonBattle battle,
            BattleSide side, ShowdownMoveset moves, boolean forced, CallbackInfoReturnable<ShowdownActionResponse> cir) {
        if (active == null || (moves == null && !forced)) {
            cir.setReturnValue(PassActionResponse.INSTANCE);
            return;
        }
        var response = TrainerBattleAI.forcedChoice(active, side, moves, forced);
        if (response != null) cir.setReturnValue(response);
    }
}
