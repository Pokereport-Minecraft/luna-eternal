package net.pokereport.luna.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.ShowdownInterpreter;
import com.cobblemon.mod.common.battles.SwitchActionResponse;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.dispatch.BattleDispatch;
import com.cobblemon.mod.common.battles.dispatch.DispatchResult;
import com.cobblemon.mod.common.battles.dispatch.DispatchResultKt;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMakeChoicePacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleQueueRequestPacket;
import com.gitlab.srcmc.rctmod.api.RCTMod;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import net.minecraft.text.Text;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.battle.TrainerBattleAI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Red de seguridad activa en PokemonBattle.
 *
 * Supervisa los combates en curso para evitar bloqueos por desincronización o paquetes perdidos:
 * 1. Garantiza que la IA y el jugador tengan sus opciones activas en cada turno (tanto cambios forzados como turnos regulares).
 * 2. Si un actor IA se atasca, rescata su elección y limpia el conjunto de solicitudes de RCT.
 * 3. Si un combate termina (por victoria, derrota, huida, empate o forzado), limpia de forma determinista el estado de TrainerMob y RCT.
 * 4. Resuelve empates y victorias estancadas si Showdown deja de responder.
 */
@Mixin(value = PokemonBattle.class, priority = 9999, remap = false)
public abstract class PokemonBattleSafetyMixin {

    @Shadow(remap = false)
    public abstract boolean getStarted();

    @Shadow(remap = false)
    public abstract boolean getEnded();

    @Shadow(remap = false)
    public abstract ConcurrentLinkedDeque<BattleDispatch> getDispatches();

    @Shadow(remap = false)
    public abstract DispatchResult getDispatchResult();

    @Shadow(remap = false)
    public abstract Iterable<BattleActor> getActors();

    @Shadow(remap = false)
    public abstract void checkForInputDispatch();

    @Shadow(remap = false)
    public abstract boolean checkForfeit();

    @Shadow(remap = false)
    public abstract boolean checkFleeAttempt();

    @Shadow(remap = false)
    public abstract List<?> getCaptureActions();

    @Shadow(remap = false)
    public abstract void setWinners(List<? extends BattleActor> winners);

    @Shadow(remap = false)
    public abstract void setLosers(List<? extends BattleActor> losers);

    @Shadow(remap = false)
    public abstract UUID getBattleId();

    @Shadow(remap = false)
    public abstract void setDispatchResult(DispatchResult dispatchResult);

    @Shadow(remap = false)
    public abstract void broadcastChatMessage(Text text);

    @Unique
    private int pokeReport$safetyTicks = 0;

    @Unique
    private int pokeReport$deadStallTicks = 0;

    @Unique
    private int pokeReport$stuckTicks = 0;

    /**
     * Limpieza completa de entrenadores y estado de batalla al finalizar cualquier combate.
     */
    @Inject(method = "end", at = @At("HEAD"), remap = false)
    private void pokeReport$onBattleEndCleanTrainers(CallbackInfo ci) {
        PokemonBattle battle = (PokemonBattle) (Object) this;
        try {
            for (BattleActor actor : battle.getActors()) {
                for (UUID pUuid : actor.getPlayerUUIDs()) {
                    try {
                        var bOpt = RCTMod.getInstance().getTrainerManager().getBattle(pUuid);
                        if (bOpt.isPresent()) {
                            var tb = bOpt.get();
                            for (TrainerMob mob : tb.getTrainerSideMobs()) {
                                if (mob != null) mob.cancelBattle();
                            }
                            for (TrainerMob mob : tb.getInitiatorSideMobs()) {
                                if (mob != null) mob.cancelBattle();
                            }
                        }
                    } catch (Throwable ignored) {}
                    try {
                        RCTMod.getInstance().getTrainerManager().removeBattle(pUuid);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            LunaEternal.LOG.warn("[BATTLE-SAFETY] Error limpiando estado de entrenadores en end(): {}", t.getMessage());
        }
    }

    /**
     * Reemplazo robusto de checkForInputDispatch con prioridad 9999.
     *
     * <p>Anula el deadlock introducido por Radical Cobblemon Trainers (RCT API)
     * en PokemonBattleMixin (prioridad 7777), donde !a.getMustChoose() bloqueaba
     * indefinidamente a actores que ya tenían sus respuestas preparadas en getResponses().
     */
    @Inject(method = "checkForInputDispatch", at = @At("HEAD"), cancellable = true, remap = false)
    private void pokeReport$checkForInputDispatch(CallbackInfo ci) {
        if (this.checkForfeit()) {
            ci.cancel();
            return;
        }
        if (this.checkFleeAttempt()) {
            ci.cancel();
            return;
        }

        List<BattleActor> livingActors = com.google.common.collect.Streams.stream(this.getActors())
                .filter(a -> a.getPokemonList().stream().anyMatch(p -> p.getHealth() > 0))
                .toList();

        if (livingActors.isEmpty()) {
            ci.cancel();
            return;
        }

        // Un actor está listo para input si:
        // 1. Ya tiene respuestas preparadas (!responses.isEmpty()) -> LISTO (sin importar mustChoose).
        // 2. O no debe elegir (!mustChoose) Y su solicitud es nula o de espera (req == null || req.getWait()).
        boolean readyToInput = livingActors.stream().allMatch(a -> {
            if (!a.getResponses().isEmpty()) {
                return true;
            }
            ShowdownActionRequest req = a.getRequest();
            return !a.getMustChoose() && (req == null || req.getWait());
        });

        if (readyToInput && this.getCaptureActions().isEmpty()) {
            livingActors.forEach(a -> {
                if (!a.getResponses().isEmpty()) {
                    a.setMustChoose(false);
                    a.writeShowdownResponse();
                } else {
                    a.setRequest(null);
                }
            });
        }
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void pokeReport$onTickSafety(CallbackInfo ci) {
        if (!this.getStarted() || this.getEnded()) {
            return;
        }

        DispatchResult dr = this.getDispatchResult();
        if (dr != null && !dr.canProceed()) {
            this.pokeReport$stuckTicks++;
            if (this.pokeReport$stuckTicks >= 200) {
                LunaEternal.LOG.warn("[BATTLE TRACE] Batalla {} atascada en DispatchResult por {} ticks. Desatascando...",
                        this.getBattleId(), this.pokeReport$stuckTicks);
                this.pokeReport$stuckTicks = 0;
                this.setDispatchResult(DispatchResultKt.getGO());
            }
            return;
        }

        if (!this.getDispatches().isEmpty()) {
            this.pokeReport$stuckTicks++;
            if (this.pokeReport$stuckTicks >= 200) {
                LunaEternal.LOG.warn("[BATTLE TRACE] Batalla {} con {} dispatches atascados por {} ticks. Desatascando...",
                        this.getBattleId(), this.getDispatches().size(), this.pokeReport$stuckTicks);
                this.pokeReport$stuckTicks = 0;
                this.setDispatchResult(DispatchResultKt.getGO());
                this.getDispatches().poll();
            }
            return;
        }

        this.pokeReport$stuckTicks = 0;
        this.pokeReport$safetyTicks++;

        // Ejecutar revisión cada 10 ticks (0.5 s) en estado ocioso
        if (this.pokeReport$safetyTicks >= 10) {
            this.pokeReport$safetyTicks = 0;
            this.pokeReport$revisarCombate();
        }
    }

    @Unique
    private void pokeReport$revisarCombate() {
        boolean algunaSolicitudPendiente = false;

        for (BattleActor actor : this.getActors()) {
            ShowdownActionRequest req = actor.getRequest();
            if (req != null && !req.getWait()) {
                algunaSolicitudPendiente = true;

                // 1. Si es IA y no ha respondido:
                if (actor instanceof AIBattleActor ai && actor.getResponses().isEmpty()) {
                    try {
                        net.pokereport.luna.battle.BattleRequestRecovery.choose(ai, req);
                    } catch (RuntimeException | LinkageError | StackOverflowError failure) {
                        net.pokereport.luna.battle.BattleRequestRecovery.abort((PokemonBattle) (Object) this, failure);
                        return;
                    }
                }

                // 2. Si es Jugador y el flag mustChoose quedó apagado pero necesita elegir:
                if (actor instanceof PlayerBattleActor && !actor.getMustChoose() && actor.getResponses().isEmpty()) {
                    actor.setMustChoose(true);
                    actor.sendUpdate(new BattleMakeChoicePacket());
                    actor.sendUpdate(new BattleQueueRequestPacket(req));
                }
            }
        }

        // Si hay solicitudes activas y todos los actores con solicitud ya tienen respuestas preparadas:
        if (algunaSolicitudPendiente) {
            boolean listosParaEnviar = true;
            for (BattleActor a : this.getActors()) {
                ShowdownActionRequest req = a.getRequest();
                if (req != null && !req.getWait()) {
                    if (a.getResponses().isEmpty()) {
                        listosParaEnviar = false;
                        break;
                    }
                }
            }
            if (listosParaEnviar) {
                this.checkForInputDispatch();
            }
        }

        // Detección de fin de combate si Showdown quedó estancado
        int actoresConVida = 0;
        int totalActores = 0;
        BattleActor ultimoConVida = null;

        for (BattleActor actor : this.getActors()) {
            totalActores++;
            boolean tieneVida = false;
            for (BattlePokemon bp : actor.getPokemonList()) {
                if (bp != null && bp.getHealth() > 0) {
                    tieneVida = true;
                    break;
                }
            }
            if (tieneVida) {
                actoresConVida++;
                ultimoConVida = actor;
            }
        }

        // Caso A: Ambos bandos perdieron todos sus Pokémon
        if (actoresConVida == 0 && totalActores > 0) {
            this.pokeReport$deadStallTicks++;
            if (this.pokeReport$deadStallTicks >= 4) { // ~2 segundos en estancamiento total
                this.pokeReport$resolverEmpate();
            }
        } else if (actoresConVida == 1 && totalActores > 1 && ultimoConVida != null) {
            // Caso B: Exactamente un bando tiene vida y el resto no tiene Pokémon
            this.pokeReport$deadStallTicks++;
            if (this.pokeReport$deadStallTicks >= 6) { // ~3 segundos en espera
                this.pokeReport$resolverVictoria(ultimoConVida);
            }
        } else {
            this.pokeReport$deadStallTicks = 0;
        }
    }

    @Unique
    private void pokeReport$resolverEmpate() {
        if (this.getEnded()) return;
        List<BattleActor> allActors = new ArrayList<>();
        for (BattleActor a : this.getActors()) {
            allActors.add(a);
        }

        this.broadcastChatMessage(Text.literal("§e¡El combate ha terminado en empate!"));
        this.setWinners(Collections.emptyList());
        this.setLosers(allActors);
        ((PokemonBattle) (Object) this).end();
        try {
            CobblemonEvents.BATTLE_VICTORY.emit(new BattleVictoryEvent[]{
                new BattleVictoryEvent((PokemonBattle)(Object)this, Collections.emptyList(), allActors, false)
            });
        } catch (Throwable ignored) {}
        ShowdownInterpreter.INSTANCE.getLastCauser().remove(this.getBattleId());
    }

    @Unique
    private void pokeReport$resolverVictoria(BattleActor ganador) {
        if (this.getEnded()) return;
        List<BattleActor> winners = List.of(ganador);
        List<BattleActor> losers = new ArrayList<>();
        for (BattleActor a : this.getActors()) {
            if (!a.equals(ganador)) losers.add(a);
        }

        this.broadcastChatMessage(Text.literal("§6¡" + ganador.getName().getString() + " ha ganado el combate!"));
        this.setWinners(winners);
        this.setLosers(losers);
        ((PokemonBattle) (Object) this).end();
        try {
            CobblemonEvents.BATTLE_VICTORY.emit(new BattleVictoryEvent[]{
                new BattleVictoryEvent((PokemonBattle)(Object)this, winners, losers, false)
            });
        } catch (Throwable ignored) {}
        ShowdownInterpreter.INSTANCE.getLastCauser().remove(this.getBattleId());
    }
}
