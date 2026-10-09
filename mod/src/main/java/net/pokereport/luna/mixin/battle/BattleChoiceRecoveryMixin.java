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

/** Cobblemon clears request when writing a choice, before Showdown can reject it. */
@Mixin(value = BattleActor.class, remap = false)
public abstract class BattleChoiceRecoveryMixin implements BattleChoiceRecovery {
    @Unique private ShowdownActionRequest pokeReport$submittedRequest;

    @Override public void pokeReport$rememberSubmittedRequest() {
        pokeReport$submittedRequest = ((BattleActor)(Object)this).getRequest();
    }

    @Inject(method = "writeShowdownResponse", at = @At("HEAD"))
    private void pokeReport$rememberChoice(CallbackInfo ci) {
        pokeReport$rememberSubmittedRequest();
    }

    @Inject(method = "setActionResponses", at = @At("HEAD"))
    private void pokeReport$rejectForcedPass(List<? extends ShowdownActionResponse> responses, CallbackInfo ci) {
        BattleActor actor = (BattleActor)(Object)this;
        ShowdownActionRequest request = actor.getRequest();
        // Trainer battles are singles. Preserve doubles' legitimate empty-slot
        // passes and native ForcePass actions for bag items/captures.
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
        if (request == null) request = pokeReport$submittedRequest;
        if (request == null || request.getWait()) return false;
        // Showdown's explicit single-slot replacement error is authoritative,
        // even if the client submitted from an obsolete regular-turn request.
        if (error.contains("Can't pass: You need to switch in a Pokémon to replace")
                && actor.getActivePokemon().size() == 1) {
            request = new ShowdownActionRequest(false, null, List.of(true), true, request.getSide());
            actor.getExpectingPassActions().clear();
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
        LunaEternal.LOG.warn("[BATTLE-CHOICE] Reopened rejected choice battle={} actor={} error={}",
                actor.getBattle().getBattleId(), actor.getUuid(), error);
        return true;
    }
}
