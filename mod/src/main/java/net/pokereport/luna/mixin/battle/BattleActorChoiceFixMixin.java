package net.pokereport.luna.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.PassActionResponse;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.battle.TrainerBattleAI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * Corrige el conflicto de sincronización entre Cobblemon y Radical Cobblemon Trainers (RCT).
 *
 * <p>Causas raíz resueltas:
 * 1. RCT AIBattleActorMixin.proxy$onChoiceRequested omite la elección si el request está en su HashSet 'requests'.
 *    Limpiamos dicho conjunto por reflexión para evitar el salto de elección.
 * 2. Si la IA falla por cualquier RuntimeException en choose(), se rescata directamente mediante req.iterate()
 *    con fallback a TrainerBattleAI.forcedChoice() o primer movimiento legal.
 * 3. Si el actor ya tiene respuestas preparadas (!responses.isEmpty()), cancela setMustChoose(true)
 *    para permitir la entrada inmediata en PokemonBattle.checkForInputDispatch().
 */
@Mixin(value = BattleActor.class, priority = 8888)
public abstract class BattleActorChoiceFixMixin {

    @Shadow(remap = false)
    public abstract List<ShowdownActionResponse> getResponses();

    @Shadow(remap = false)
    public abstract ShowdownActionRequest getRequest();

    @Shadow(remap = false)
    public abstract void setActionResponses(List<? extends ShowdownActionResponse> responses);

    @Inject(method = "setMustChoose", at = @At("HEAD"), cancellable = true, remap = false)
    private void pokeReport$onSetMustChoose(boolean value, CallbackInfo ci) {
        if (value) {
            // Caso 1: Si ya existen respuestas preparadas, jamás debe quedar en mustChoose=true.
            if (!this.getResponses().isEmpty()) {
                ci.cancel();
                return;
            }

            // Caso 2: Si es una IA y se le pide elegir pero aún no tiene respuestas:
            BattleActor self = (BattleActor) (Object) this;
            if (self instanceof AIBattleActor ai) {
                ShowdownActionRequest req = this.getRequest();
                if (req == null || req.getWait()) {
                    ci.cancel();
                    return;
                }

                // 2a. Desbloquear el set 'requests' de RCT AIBattleActorMixin
                try {
                    Field f = ai.getClass().getDeclaredField("requests");
                    f.setAccessible(true);
                    Object s = f.get(ai);
                    if (s instanceof Set<?> set) {
                        set.clear();
                    }
                } catch (Throwable ignored) {}

                // 2b. Invocar la elección normal de la IA
                try {
                    ai.onChoiceRequested();
                } catch (Throwable t) {
                    LunaEternal.LOG.warn("[BATTLE-CHOICE-FIX] Excepción en onChoiceRequested para IA {}: {}",
                            ai.getUuid(), t.getMessage());
                }

                // 2c. Si aún no tiene respuestas, rescatar directamente con iterate y fallback táctico
                if (this.getResponses().isEmpty()) {
                    try {
                        List<ShowdownActionResponse> choices = req.iterate(ai.getActivePokemon(), (active, moves, forced) -> {
                            try {
                                return ai.getBattleAI().choose(active, ai.getBattle(), ai.getSide(), moves, forced);
                            } catch (Throwable t2) {
                                return TrainerBattleAI.forcedChoice(active, ai.getSide(), moves, forced);
                            }
                        });
                        if (choices != null && !choices.isEmpty()) {
                            this.setActionResponses(choices);
                        }
                    } catch (Throwable t3) {
                        LunaEternal.LOG.warn("[BATTLE-CHOICE-FIX] Falló iterate en rescate de IA: {}", t3.getMessage());
                    }
                }

                // 2d. Fallback de última instancia si persiste vacío: forzar Pass para no congelar Showdown
                if (this.getResponses().isEmpty()) {
                    for (int i = 0; i < ai.getActivePokemon().size(); i++) {
                        this.getResponses().add(PassActionResponse.INSTANCE);
                    }
                }

                for (BattlePokemon bp : self.getPokemonList()) {
                    if (bp != null && !bp.isSentOut()) {
                        bp.setWillBeSwitchedIn(false);
                    }
                }

                if (!this.getResponses().isEmpty()) {
                    ci.cancel();
                }
            }
        }
    }
}
