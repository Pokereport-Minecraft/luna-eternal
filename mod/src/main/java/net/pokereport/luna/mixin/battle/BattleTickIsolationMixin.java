package net.pokereport.luna.mixin.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.pokereport.luna.battle.BattleRequestRecovery;
import org.spongepowered.asm.mixin.Mixin;

/** Native/RCT AI also runs before our request tail injection. Isolate failures per battle. */
@Mixin(value = PokemonBattle.class, remap = false)
public abstract class BattleTickIsolationMixin {
    @WrapMethod(method = "tick")
    private void pokeReport$isolateTick(Operation<Void> original) {
        net.pokereport.luna.battle.BattleFailureBoundary.run(() -> original.call(),
                failure -> BattleRequestRecovery.abort((PokemonBattle) (Object) this, failure));
    }
}
