package net.pokereport.luna.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.dispatch.BattleDispatch;
import com.cobblemon.mod.common.battles.dispatch.DispatchResult;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMakeChoicePacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleQueueRequestPacket;
import com.gitlab.srcmc.rctmod.api.RCTMod;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import net.pokereport.luna.LunaEternal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Red de seguridad activa en PokemonBattle.
 *
 * Supervisa los combates en curso para evitar bloqueos por desincronización o paquetes perdidos:
 * 1. Garantiza que la IA y el jugador tengan sus opciones activas en cada turno (tanto cambios forzados como turnos regulares).
 * 2. Recupera decisiones de IA cuando la cola está ociosa.
 * 3. Si un combate termina (por victoria, derrota, huida, empate o forzado), limpia de forma determinista el estado de TrainerMob y RCT.
 * 4. Observa la cola sin alterar sus barreras; un fallo se cierra sin inventar un ganador.
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
    public abstract UUID getBattleId();

    @Unique
    private int pokeReport$safetyTicks = 0;

    @Unique
    private final net.pokereport.luna.battle.BattleQueueWatchdog pokeReport$queueWatchdog =
            new net.pokereport.luna.battle.BattleQueueWatchdog();

    @Shadow(remap = false)
    public abstract int getTurn();

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
        boolean blocked = dr != null && !dr.canProceed();
        boolean waitingForPlayer = com.google.common.collect.Streams.stream(this.getActors()).anyMatch(a -> {
            var request = a.getRequest();
            return a instanceof PlayerBattleActor && request != null && !request.getWait()
                    && a.getResponses().isEmpty();
        });
        var observation = pokeReport$queueWatchdog.observe(dr, this.getDispatches().peek(),
                getTurn(), blocked || !this.getDispatches().isEmpty(), waitingForPlayer);
        if (observation == net.pokereport.luna.battle.BattleQueueWatchdog.Result.WARN) {
            LunaEternal.LOG.warn("[BATTLE-QUEUE] battle={} turn={} barrier={} queued={}; preserving protocol order",
                    getBattleId(), getTurn(), dr == null ? "null" : dr.getClass().getSimpleName(), getDispatches().size());
        } else if (observation == net.pokereport.luna.battle.BattleQueueWatchdog.Result.ABORT) {
            net.pokereport.luna.battle.BattleRequestRecovery.abort((PokemonBattle)(Object)this,
                    new IllegalStateException("Battle queue made no progress for 1200 ticks"));
            return;
        }
        // A blocked dispatch owns this sequence. Never choose, release or discard through it.
        if (blocked || !this.getDispatches().isEmpty()) return;
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

        // Only Showdown can declare a winner; local HP is not an outcome protocol.
    }
}
