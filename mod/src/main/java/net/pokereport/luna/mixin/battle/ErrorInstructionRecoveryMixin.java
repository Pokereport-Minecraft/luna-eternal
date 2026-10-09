package net.pokereport.luna.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.interpreter.instructions.ErrorInstruction;
import kotlin.Unit;
import net.pokereport.luna.battle.BattleChoiceRecovery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ErrorInstruction.class, remap = false)
public abstract class ErrorInstructionRecoveryMixin {
    @Inject(method = "invoke$lambda$0", at = @At("HEAD"), cancellable = true)
    private static void pokeReport$retry(ErrorInstruction instruction, PokemonBattle battle,
                                        CallbackInfoReturnable<Unit> cir) {
        if (((BattleChoiceRecovery)instruction.getBattleActor()).pokeReport$recoverRejectedChoice(
                instruction.getMessage().getRawMessage())) cir.setReturnValue(Unit.INSTANCE);
    }
}
