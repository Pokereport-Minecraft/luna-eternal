package net.pokereport.luna.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import com.cobblemon.mod.common.net.messages.server.battle.BattleSelectActionsPacket;
import com.cobblemon.mod.common.net.serverhandling.battle.BattleSelectActionsHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Accept one choice only after the request's update and animations are complete. */
@Mixin(value = BattleSelectActionsHandler.class, priority = 8888, remap = false)
public abstract class BattleSelectActionsHandlerMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void pokeReport$onHandleSelectActions(BattleSelectActionsPacket packet, MinecraftServer server, ServerPlayerEntity player, CallbackInfo ci) {
        if (packet == null || player == null) return;
        PokemonBattle battle = BattleRegistry.getBattle(packet.getBattleId());
        if (battle == null) return;
        if (!net.pokereport.luna.battle.BattleInputCoordinator.manages(battle)) return;

        for (BattleActor actor : battle.getActors()) {
            boolean matches = false;
            for (UUID u : actor.getPlayerUUIDs()) {
                if (u.equals(player.getUuid())) {
                    matches = true;
                    break;
                }
            }
            if (matches) {
                ShowdownActionRequest req = actor.getRequest();
                if (battle.getEnded() || req == null || req.getWait() || !actor.getResponses().isEmpty()) {
                    ci.cancel();
                } else if (!net.pokereport.luna.battle.BattleInputCoordinator.idle(battle)
                        || !((net.pokereport.luna.battle.BattleChoiceRecovery)actor).pokeReport$isRequestReady()) {
                    // Do not turn a stale GUI selection into a new turn. Reopen
                    // from the current request once the animation queue drains.
                    actor.setMustChoose(false);
                    ci.cancel();
                }
                break;
            }
        }
    }
}
