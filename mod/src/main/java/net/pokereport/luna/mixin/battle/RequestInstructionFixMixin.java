package net.pokereport.luna.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import com.cobblemon.mod.common.battles.interpreter.instructions.RequestInstruction;
import kotlin.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Corrige el flujo de solicitudes de turno y cambios forzados (forceSwitch) en Cobblemon.
 *
 * <p>En Cobblemon vanilla, RequestInstruction sólo asigna actor.setRequest(...) y limpia responses,
 * pero no invoca postUpdate(). Debido a que PostUpdateInstruction únicamente se encola al final
 * de los bloques 'update' y no de los bloques 'sideupdate' donde viajan los |request|,
 * tras un debilitamiento (faint) el actor nunca recibe BattleMakeChoicePacket, mustChoose
 * permanece en false, y la máquina de estados se congela.
 *
 * <p>Este Mixin se inyecta en RequestInstruction.invoke(...) y encola un dispatchGo que,
 * inmediatamente después de que Cobblemon asigne el request, comprueba si requiere forceSwitch
 * o acción inmediata y dispara actor.postUpdate(), notificando al jugador y a la IA sin
 * alterar el Mixin de RCT API que limpia switchChoices en el HEAD de este método.
 */
@Mixin(value = RequestInstruction.class, priority = 8888)
public abstract class RequestInstructionFixMixin {

    @Shadow(remap = false)
    public abstract BattleActor getBattleActor();

    @Inject(method = "invoke", at = @At("TAIL"), remap = false)
    private void pokeReport$onInvokeRequest(PokemonBattle battle, CallbackInfo ci) {
        battle.dispatchGo(() -> {
            BattleActor actor = this.getBattleActor();
            if (actor != null) {
                ShowdownActionRequest req = actor.getRequest();
                if (req != null && req.getForceSwitch() != null && req.getForceSwitch().contains(true)) {
                    actor.postUpdate();
                }
            }
            return Unit.INSTANCE;
        });
    }
}
