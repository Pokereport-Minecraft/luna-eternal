package net.pokereport.luna.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.battles.PassActionResponse;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import com.cobblemon.mod.common.battles.ShowdownInterpreter;
import com.cobblemon.mod.common.battles.SwitchActionResponse;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.dispatch.BattleDispatch;
import com.cobblemon.mod.common.battles.dispatch.DispatchResult;
import com.cobblemon.mod.common.battles.dispatch.DispatchResultKt;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMakeChoicePacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleQueueRequestPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Red de seguridad activa en PokemonBattle.tick().
 *
 * Supervisa los combates en curso para evitar bloqueos por desincronización o paquetes perdidos:
 * 1. Si hay cambios forzados pendientes y la cola de dispatches está ociosa, garantiza que la IA
 *    genere su elección (con fallback al primer Pokémon vivo de banquillo) y que el jugador reciba
 *    el diálogo de selección.
 * 2. Si todos los combatientes con cambio forzado tienen sus respuestas listas, dispara checkForInputDispatch().
 * 3. Si todos los Pokémon de ambos bandos están debilitados y Showdown no ha finalizado, resuelve empate.
 * 4. Si un bando tiene 0 Pokémon en todo su equipo y el otro tiene vivos, declara ganador al bando con vida.
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
    public abstract void end();

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
            // 200 ticks = 10 segundos continuos sin avanzar de animación / future.
            // Los faints y recalls normales tardan entre 2.5s y 3.5s (50-70 ticks), por lo que 80 ticks generaba falsos positivos.
            if (this.pokeReport$stuckTicks >= 200) {
                net.pokereport.luna.LunaEternal.LOG.warn("[BATTLE TRACE] Batalla {} atascada en DispatchResult por {} ticks. Desatascando...",
                        this.getBattleId(), this.pokeReport$stuckTicks);
                this.pokeReport$stuckTicks = 0;
                this.setDispatchResult(DispatchResultKt.getGO());
            }
            return;
        }

        if (!this.getDispatches().isEmpty()) {
            this.pokeReport$stuckTicks++;
            if (this.pokeReport$stuckTicks >= 200) {
                net.pokereport.luna.LunaEternal.LOG.warn("[BATTLE TRACE] Batalla {} con {} dispatches atascados por {} ticks. Desatascando...",
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
        boolean algunCambioForzado = false;

        for (BattleActor actor : this.getActors()) {
            ShowdownActionRequest req = actor.getRequest();
            if (req != null && req.getForceSwitch() != null && req.getForceSwitch().contains(true)) {
                algunCambioForzado = true;

                // 1. Si es IA y no ha respondido:
                if (actor instanceof AIBattleActor ai && actor.getResponses().isEmpty()) {
                    try {
                        ai.onChoiceRequested();
                    } catch (Throwable ignored) {}

                    // Si aún no tiene respuestas, rescatar con el primer Pokémon vivo de banquillo
                    if (actor.getResponses().isEmpty()) {
                        BattlePokemon vivo = null;
                        for (BattlePokemon bp : actor.getPokemonList()) {
                            if (bp != null && bp.getHealth() > 0 && !bp.isSentOut() && !bp.getWillBeSwitchedIn()) {
                                vivo = bp;
                                break;
                            }
                        }
                        if (vivo != null) {
                            vivo.setWillBeSwitchedIn(true);
                            actor.getResponses().add(new SwitchActionResponse(vivo.getUuid()));
                            actor.setMustChoose(false);
                        } else {
                            actor.getResponses().add(PassActionResponse.INSTANCE);
                            actor.setMustChoose(false);
                        }
                    }
                }

                // 2. Si es Jugador y el flag mustChoose quedó apagado pero necesita elegir:
                if (actor instanceof PlayerBattleActor playerActor && !actor.getMustChoose() && actor.getResponses().isEmpty()) {
                    actor.setMustChoose(true);
                    actor.sendUpdate(new BattleMakeChoicePacket());
                    actor.sendUpdate(new BattleQueueRequestPacket(req));
                }
            }
        }

        // Si hay cambio forzado en curso y los actores requeridos ya tienen respuestas preparadas:
        if (algunCambioForzado) {
            boolean listosParaEnviar = true;
            for (BattleActor a : this.getActors()) {
                ShowdownActionRequest req = a.getRequest();
                if (req != null && req.getForceSwitch() != null && req.getForceSwitch().contains(true)) {
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
        this.end();
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
        this.end();
        try {
            CobblemonEvents.BATTLE_VICTORY.emit(new BattleVictoryEvent[]{
                new BattleVictoryEvent((PokemonBattle)(Object)this, winners, losers, false)
            });
        } catch (Throwable ignored) {}
        ShowdownInterpreter.INSTANCE.getLastCauser().remove(this.getBattleId());
    }
}
