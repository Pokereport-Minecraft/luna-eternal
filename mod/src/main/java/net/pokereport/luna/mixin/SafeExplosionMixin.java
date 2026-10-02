package net.pokereport.luna.mixin;

import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.pokereport.luna.world.WorldProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class SafeExplosionMixin {
    @Shadow @Final private World world;
    @Inject(method = "affectWorld", at = @At("HEAD"), cancellable = true)
    private void luna$keepTerrain(boolean particles, CallbackInfo ci) {
        if (!world.isClient() && WorldProtectionManager.isProtectedDimension(world)) ci.cancel();
    }
}
