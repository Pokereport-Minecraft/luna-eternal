package net.pokereport.luna.mixin;

import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pokereport.luna.world.WorldProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public abstract class SafeHungerMixin {
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void luna$safeFood(PlayerEntity player, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity sp && WorldProtectionManager.isProtectedDimension(sp.getWorld())) {
            HungerManager hunger = (HungerManager) (Object) this;
            if (hunger.getFoodLevel() != 20) hunger.setFoodLevel(20);
            if (hunger.getSaturationLevel() < 5) hunger.setSaturationLevel(5);
            if (hunger.getExhaustion() != 0) hunger.setExhaustion(0);
            ci.cancel();
        }
    }
}
