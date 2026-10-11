package net.pokereport.luna.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMadeInvalidChoicePacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMakeChoicePacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleQueueRequestPacket;
import com.gitlab.srcmc.rctapi.api.battle.BattleState;

/**
 * Showdown sends side requests BEFORE the update containing moves/faints/switches.
 * A request's arrival is therefore not permission to advance the battle. Only a
 * drained animation queue may choose or submit; Showdown owns turns and outcomes.
 */
public final class BattleInputCoordinator {
    private BattleInputCoordinator() {}

    public static boolean manages(PokemonBattle battle) {
        if (BattleState.findFirst(battle) != null) return true;
        for (var actor : battle.getActors()) {
            if (actor instanceof AIBattleActor ai && ai.getBattleAI() instanceof TrainerBattleAI) return true;
        }
        return false;
    }

    public static boolean idle(PokemonBattle battle) {
        return !battle.getEnded() && battle.getDispatches().isEmpty()
                && (battle.getDispatchResult() == null || battle.getDispatchResult().canProceed());
    }

    public static void onIdle(PokemonBattle battle) {
        if (!battle.getStarted() || !idle(battle)) return;
        for (var actor : battle.getActors()) {
            var request = actor.getRequest();
            if (request == null) continue;
            if (!((BattleChoiceRecovery)actor).pokeReport$isRequestReady()) continue;
            if (request.getWait()) {
                // The other side is replacing. This actor has NO action in that phase.
                actor.getResponses().clear();
                actor.setMustChoose(false);
            } else if (actor instanceof AIBattleActor ai && actor.getResponses().isEmpty()) {
                BattleRequestRecovery.choose(ai, request);
            } else if (actor instanceof PlayerBattleActor && actor.getResponses().isEmpty()
                    && !actor.getMustChoose()) {
                actor.sendUpdate(new BattleQueueRequestPacket(request));
                actor.sendUpdate(new BattleMadeInvalidChoicePacket());
                actor.setMustChoose(true);
                actor.sendUpdate(new BattleMakeChoicePacket());
            }
        }
        battle.checkForInputDispatch();
    }

    public static void dispatch(PokemonBattle battle) {
        if (!idle(battle) || !battle.getCaptureActions().isEmpty()) return;
        boolean hasChoice = false;
        for (var actor : battle.getActors()) {
            var request = actor.getRequest();
            if (request == null) continue;
            if (!((BattleChoiceRecovery)actor).pokeReport$isRequestReady()) return;
            if (request.getWait()) continue;
            int expected = Math.max(request.getActive() == null ? 0 : request.getActive().size(),
                    request.getForceSwitch().size());
            if (expected == 0 || actor.getResponses().size() != expected) return;
            hasChoice = true;
        }
        if (!hasChoice) return;
        for (var actor : battle.getActors()) {
            var request = actor.getRequest();
            if (request == null || request.getWait()) continue;
            actor.setMustChoose(false);
            actor.writeShowdownResponse();
        }
    }
}
