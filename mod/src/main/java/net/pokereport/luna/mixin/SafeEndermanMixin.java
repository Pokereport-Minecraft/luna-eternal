package net.pokereport.luna.mixin;
import net.minecraft.entity.mob.EndermanEntity;
import net.pokereport.luna.world.WorldProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets = "net.minecraft.entity.mob.EndermanEntity$PickUpBlockGoal")
public abstract class SafeEndermanMixin {
    @Shadow @Final private EndermanEntity enderman;
    @Inject(method = "canStart", at = @At("HEAD"), cancellable = true)
    private void luna$keepMap(CallbackInfoReturnable<Boolean> cir) {
        if (WorldProtectionManager.isProtectedDimension(enderman.getWorld())) cir.setReturnValue(false);
    }
}
