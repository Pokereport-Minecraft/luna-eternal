package net.pokereport.luna.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.PassActionResponse;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.exception.IllegalActionChoiceException;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMakeChoicePacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMadeInvalidChoicePacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleQueueRequestPacket;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.battle.BattleChoiceRecovery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/** Cobblemon clears request when writing a choice, before Showdown can reject it. */
@Mixin(value = BattleActor.class, remap = false)
public abstract class BattleChoiceRecoveryMixin implements BattleChoiceRecovery {
    @Unique private ShowdownActionRequest pokeReport$submittedRequest;
    @Unique private int pokeReport$aiRejectedChoices;
    @Unique private ShowdownActionRequest pokeReport$readyRequest;

    @Inject(method = "postUpdate", at = @At("RETURN"))
    private void pokeReport$activateAfterUpdate(CallbackInfo ci) {
        // PostUpdateInstruction is queued AFTER moves, faint and switch animations.
        // Merely receiving a sideupdate request is not enough (Graal is asynchronous).
        pokeReport$readyRequest = ((BattleActor)(Object)this).getRequest();
    }

    @Override public boolean pokeReport$isRequestReady() {
        var request = ((BattleActor)(Object)this).getRequest();
        return request != null && request == pokeReport$readyRequest;
    }

    @WrapMethod(method = "setActionResponses")
    private void pokeReport$atomicChoices(List<? extends ShowdownActionResponse> responses, Operation<Void> original) {
        BattleActor actor = (BattleActor)(Object)this;
        var previousResponses = new java.util.ArrayList<>(actor.getResponses());
        var expectedPasses = new java.util.ArrayList<>(actor.getExpectingPassActions());
        try { original.call(responses); }
        catch (IllegalActionChoiceException rejected) {
            // Native validation appends slot 1 before validating slot 2. A
            // rejected double selection must not retain half of the old turn.
            actor.getResponses().clear(); actor.getResponses().addAll(previousResponses);
            actor.getExpectingPassActions().clear(); actor.getExpectingPassActions().addAll(expectedPasses);
            throw rejected;
        }
    }

    @Override public void pokeReport$rememberSubmittedRequest() {
        var request = ((BattleActor)(Object)this).getRequest();
        if (request != pokeReport$submittedRequest) pokeReport$aiRejectedChoices = 0;
        pokeReport$submittedRequest = request;
    }

    @Inject(method = "writeShowdownResponse", at = @At("HEAD"))
    private void pokeReport$rememberChoice(CallbackInfo ci) {
        pokeReport$rememberSubmittedRequest();
    }

    @Inject(method = "setActionResponses", at = @At("HEAD"))
    private void pokeReport$rejectForcedPass(List<? extends ShowdownActionResponse> responses, CallbackInfo ci) {
        BattleActor actor = (BattleActor)(Object)this;
        ShowdownActionRequest request = actor.getRequest();
        if (request != null && !request.getWait()) {
            int expected = Math.max(request.getActive() == null ? 0 : request.getActive().size(),
                    request.getForceSwitch().size());
            if (responses.size() != expected || responses.stream().anyMatch(java.util.Objects::isNull)) {
                throw new IllegalActionChoiceException(actor, "Selecciona una acción por cada posición activa.");
            }
            var switches = new java.util.HashSet<java.util.UUID>();
            for (var response : responses) {
                if (response instanceof com.cobblemon.mod.common.battles.SwitchActionResponse change) {
                    var id = change.getNewPokemonId();
                    if (!switches.add(id) || actor.getPokemonList().stream().noneMatch(p -> p.getUuid().equals(id))) {
                        throw new IllegalActionChoiceException(actor, "Selecciona Pokémon distintos de tu equipo.");
                    }
                }
            }
        }
        // Preserve doubles' legitimate empty-slot passes and native ForcePass
        // actions for bag items/captures. Giovanni and Luana use doubles.
        if (request != null && request.getForceSwitch().equals(List.of(true))
                && responses.stream().anyMatch(r -> r instanceof PassActionResponse)
                && actor.getPokemonList().stream().anyMatch(p -> p.getHealth() > 0 && !p.isSentOut())) {
            throw new IllegalActionChoiceException(actor, "Elige un Pokémon para reemplazar al debilitado.");
        }
    }

    @Override public boolean pokeReport$recoverRejectedChoice(String error) {
        if (error == null || !(error.startsWith("|error|[Invalid choice]")
                || error.startsWith("|error|[Unavailable choice]"))
                || error.contains("It's not your turn") || error.contains("Team Preview")) return false;
        BattleActor actor = (BattleActor)(Object)this;
        if (!actor.isInitialized() || actor.getBattle().getEnded()) return false;
        ShowdownActionRequest request = actor.getRequest();
        // An error for a previous submission must never reopen or mutate a
        // newer request. In particular, never fabricate forceSwitch from text.
        if (request != null && request != pokeReport$submittedRequest) return true;
        if (request == null) request = pokeReport$submittedRequest;
        if (request == null || request.getWait()) return false;
        if (actor instanceof AIBattleActor && ++pokeReport$aiRejectedChoices > 2) {
            net.pokereport.luna.battle.BattleRequestRecovery.abort(actor.getBattle(),
                    new IllegalStateException("AI repeatedly rejected for the same request: " + error));
            return true;
        }
        actor.getResponses().clear();
        for (var pokemon : actor.getPokemonList()) {
            if (!pokemon.isSentOut()) pokemon.setWillBeSwitchedIn(false);
        }
        actor.setRequest(request);
        // Queue the authoritative request before asking the client to choose.
        actor.sendUpdate(new BattleQueueRequestPacket(request));
        actor.setMustChoose(true);
        if (!(actor instanceof AIBattleActor)) {
            // The native invalid-choice packet also removes the obsolete GUI
            // selection widget; MakeChoice alone only toggles mustChoose.
            actor.sendUpdate(new BattleMadeInvalidChoicePacket());
            actor.sendUpdate(new BattleMakeChoicePacket());
        }
        LunaEternal.LOG.warn("[BATTLE-CHOICE] Reopened rejected choice battle={} turn={} actor={} type={} error={}",
                actor.getBattle().getBattleId(), actor.getBattle().getTurn(), actor.getUuid(),
                actor.getClass().getSimpleName(), error);
        return true;
    }
}
