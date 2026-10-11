package net.pokereport.luna.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.dispatch.BattleDispatch;
import com.cobblemon.mod.common.battles.dispatch.DispatchResult;
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
        if (!net.pokereport.luna.battle.BattleInputCoordinator.manages(battle)) return;
        try {
            for (BattleActor actor : battle.getActors()) {
                for (UUID pUuid : actor.getPlayerUUIDs()) {
                    var current = com.cobblemon.mod.common.battles.BattleRegistry.getBattleByParticipatingPlayerId(pUuid);
                    if (current != null && current != battle && !current.getEnded()) continue;
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
        if (!net.pokereport.luna.battle.BattleInputCoordinator.manages((PokemonBattle)(Object)this)) return;
        if (this.checkForfeit()) {
            ci.cancel();
            return;
        }
        if (this.checkFleeAttempt()) {
            ci.cancel();
            return;
        }

        net.pokereport.luna.battle.BattleInputCoordinator.dispatch((PokemonBattle)(Object)this);
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void pokeReport$onTickSafety(CallbackInfo ci) {
        if (!this.getStarted() || this.getEnded()
                || !net.pokereport.luna.battle.BattleInputCoordinator.manages((PokemonBattle)(Object)this)) {
            return;
        }

        DispatchResult dr = this.getDispatchResult();
        boolean blocked = dr != null && !dr.canProceed();
        boolean waitingForPlayer = com.google.common.collect.Streams.stream(this.getActors()).anyMatch(a -> {
            var request = a.getRequest();
            return a instanceof PlayerBattleActor && request != null && !request.getWait()
                    && a.getResponses().isEmpty() && a.getMustChoose()
                    && ((net.pokereport.luna.battle.BattleChoiceRecovery)a).pokeReport$isRequestReady();
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
        if (blocked || !this.getDispatches().isEmpty()) return;
        net.pokereport.luna.battle.BattleInputCoordinator.onIdle((PokemonBattle)(Object)this);
    }
}
