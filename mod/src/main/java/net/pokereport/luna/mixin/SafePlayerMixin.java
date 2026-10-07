package net.pokereport.luna.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pokereport.luna.world.WorldProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class SafePlayerMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void luna$safeDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity player
                && WorldProtectionManager.isProtectedDimension(player.getWorld())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void luna$safeExhaustion(float amount, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player
                && WorldProtectionManager.isProtectedDimension(player.getWorld())) ci.cancel();
    }
}
