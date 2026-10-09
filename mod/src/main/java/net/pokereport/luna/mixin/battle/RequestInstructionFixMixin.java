package net.pokereport.luna.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.PassActionResponse;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.interpreter.instructions.RequestInstruction;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMakeChoicePacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleQueueRequestPacket;
import kotlin.Unit;
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
 * Corrige la desincronización entre RequestInstruction y PostUpdateInstruction en Cobblemon.
 *
 * <p>En Cobblemon vanilla, RequestInstruction sólo asigna actor.setRequest(...) y limpia responses,
 * pero delega la activación de elección exclusivamente a PostUpdateInstruction. Dado que los |request|
 * viajan frecuentemente en bloques 'sideupdate' que no contienen PostUpdateInstruction, tanto los cambios
 * forzados tras un KO como los turnos regulares pueden quedarse sin notificar al jugador ni a la IA.
 *
 * <p>Este Mixin garantiza que para cualquier solicitud activa (!req.getWait()):
 * 1. Se invoque actor.postUpdate().
 * 2. Si el actor es IA, se desbloquee el set 'requests' de RCT y se genere su selección inmediatamente.
 * 3. Si el actor es un jugador, se asegure que reciba BattleMakeChoicePacket y BattleQueueRequestPacket.
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
                if (req != null && !req.getWait()) {
                    actor.postUpdate();

                    if (actor instanceof AIBattleActor ai && actor.getResponses().isEmpty()) {
                        try {
                            Field f = ai.getClass().getDeclaredField("requests");
                            f.setAccessible(true);
                            Object s = f.get(ai);
                            if (s instanceof Set<?> set) {
                                set.clear();
                            }
                        } catch (Throwable ignored) {}

                        try {
                            ai.onChoiceRequested();
                        } catch (Throwable t) {
                            LunaEternal.LOG.warn("[REQUEST-FIX] Error en onChoiceRequested de IA: {}", t.getMessage());
                        }

                        if (actor.getResponses().isEmpty()) {
                            try {
                                List<ShowdownActionResponse> choices = req.iterate(ai.getActivePokemon(), (active, moves, forced) -> {
                                    try {
                                        return ai.getBattleAI().choose(active, ai.getBattle(), ai.getSide(), moves, forced);
                                    } catch (Throwable t2) {
                                        return TrainerBattleAI.forcedChoice(active, ai.getSide(), moves, forced);
                                    }
                                });
                                if (choices != null && !choices.isEmpty()) {
                                    actor.setActionResponses(choices);
                                }
                            } catch (Throwable t3) {
                                LunaEternal.LOG.warn("[REQUEST-FIX] Falló iterate en RequestInstruction: {}", t3.getMessage());
                            }
                        }

                        if (actor.getResponses().isEmpty()) {
                            for (int i = 0; i < ai.getActivePokemon().size(); i++) {
                                actor.getResponses().add(PassActionResponse.INSTANCE);
                            }
                        }

                        for (BattlePokemon bp : actor.getPokemonList()) {
                            if (bp != null && !bp.isSentOut()) {
                                bp.setWillBeSwitchedIn(false);
                            }
                        }
                        actor.setMustChoose(false);
                    } else if (actor instanceof PlayerBattleActor && actor.getResponses().isEmpty() && !actor.getMustChoose()) {
                        actor.setMustChoose(true);
                        actor.sendUpdate(new BattleQueueRequestPacket(req));
                        actor.sendUpdate(new BattleMakeChoicePacket());
                    }
                }
            }
            return Unit.INSTANCE;
        });
    }
}
