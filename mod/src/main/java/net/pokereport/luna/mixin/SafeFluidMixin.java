package net.pokereport.luna.mixin;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.pokereport.luna.world.WorldProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FlowableFluid.class)
public abstract class SafeFluidMixin {
    @Inject(method = "onScheduledTick", at = @At("HEAD"), cancellable = true)
    private void luna$noFlow(World world, BlockPos pos, FluidState state, CallbackInfo ci) {
        if (!world.isClient() && WorldProtectionManager.isProtectedDimension(world)) ci.cancel();
    }
}
