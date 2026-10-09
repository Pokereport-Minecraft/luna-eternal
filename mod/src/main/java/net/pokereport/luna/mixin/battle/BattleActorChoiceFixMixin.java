package net.pokereport.luna.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Never request choices inside this setter: onChoiceRequested calls the setter itself. */
@Mixin(value = BattleActor.class, priority = 8888)
public abstract class BattleActorChoiceFixMixin {
    @Shadow(remap = false) private boolean mustChoose;
    @Shadow(remap = false) public abstract List<ShowdownActionResponse> getResponses();

    @Inject(method = "setMustChoose", at = @At("HEAD"), cancellable = true, remap = false)
    private void pokeReport$onSetMustChoose(boolean value, CallbackInfo ci) {
        if (value && (Object) this instanceof AIBattleActor && !getResponses().isEmpty()) {
            mustChoose = false;
            ci.cancel();
        }
    }
}
