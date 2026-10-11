package net.pokereport.luna.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import net.pokereport.luna.battle.BattleInputCoordinator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** RCT's intrinsic and native packets may request the same choice repeatedly. */
@Mixin(value = AIBattleActor.class, priority = 9000, remap = false)
public abstract class TrainerChoiceSchedulingMixin {
    @Inject(method = "onChoiceRequested", at = @At("HEAD"), cancellable = true)
    private void pokeReport$deferChoice(CallbackInfo ci) {
        var actor = (AIBattleActor)(Object)this;
        if (actor.isInitialized() && BattleInputCoordinator.manages(actor.getBattle())) ci.cancel();
    }
}
