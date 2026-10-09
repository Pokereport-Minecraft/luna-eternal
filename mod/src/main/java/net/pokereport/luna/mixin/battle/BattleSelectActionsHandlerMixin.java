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

/**
 * Evita el descarte silencioso de paquetes de selección de acciones enviados por jugadores.
 *
 * <p>En Cobblemon vanilla, BattleSelectActionsHandler descarta el paquete si !actor.getMustChoose().
 * Si un paquete del cliente llega en el breve lapso de transición entre un nuevo turno o tras un
 * faint antes de que la máquina de estados local active mustChoose, el paquete se perdía y el
 * jugador quedaba congelado esperando que su selección se procese.
 */
@Mixin(value = BattleSelectActionsHandler.class, priority = 8888, remap = false)
public abstract class BattleSelectActionsHandlerMixin {

    @Inject(method = "handle", at = @At("HEAD"), remap = false)
    private void pokeReport$onHandleSelectActions(BattleSelectActionsPacket packet, MinecraftServer server, ServerPlayerEntity player, CallbackInfo ci) {
        if (packet == null || player == null) return;
        PokemonBattle battle = BattleRegistry.getBattle(packet.getBattleId());
        if (battle == null) return;

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
                // Si el jugador tiene una solicitud activa pendiente y aún no tiene respuestas registradas:
                if (req != null && !req.getWait() && actor.getResponses().isEmpty() && !actor.getMustChoose()) {
                    actor.setMustChoose(true);
                }
                break;
            }
        }
    }
}
