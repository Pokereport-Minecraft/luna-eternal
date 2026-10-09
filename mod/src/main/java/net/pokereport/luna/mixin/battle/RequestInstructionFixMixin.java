package net.pokereport.luna.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.interpreter.instructions.RequestInstruction;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMakeChoicePacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleQueueRequestPacket;
import kotlin.Unit;
import net.pokereport.luna.battle.BattleRequestRecovery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RequestInstruction.class, priority = 8888)
public abstract class RequestInstructionFixMixin {
    @Shadow(remap = false) public abstract BattleActor getBattleActor();

    @Inject(method = "invoke", at = @At("TAIL"), remap = false)
    private void pokeReport$onInvokeRequest(PokemonBattle battle, CallbackInfo ci) {
        battle.dispatchGo(() -> {
            // Errors occur when this closure executes in tick(), not when it is enqueued.
            try {
                if (battle.getEnded()) return Unit.INSTANCE;
                BattleActor actor = getBattleActor();
                if (actor == null) return Unit.INSTANCE;
                var request = actor.getRequest();
                if (request == null || request.getWait()) return Unit.INSTANCE;
                actor.postUpdate();
                // postUpdate may already submit and clear the request when both sides are ready.
                if (battle.getEnded() || actor.getRequest() != request) return Unit.INSTANCE;
                if (actor instanceof AIBattleActor ai) {
                    BattleRequestRecovery.choose(ai, request);
                } else if (actor instanceof PlayerBattleActor && actor.getResponses().isEmpty()
                        && !actor.getMustChoose()) {
                    actor.setMustChoose(true);
                    actor.sendUpdate(new BattleQueueRequestPacket(request));
                    actor.sendUpdate(new BattleMakeChoicePacket());
                }
            } catch (RuntimeException | LinkageError | StackOverflowError failure) {
                BattleRequestRecovery.abort(battle, failure);
            }
            return Unit.INSTANCE;
        });
    }
}
