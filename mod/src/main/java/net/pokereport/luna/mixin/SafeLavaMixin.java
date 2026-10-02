package net.pokereport.luna.mixin;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.pokereport.luna.world.WorldProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LavaFluid.class)
public abstract class SafeLavaMixin {
    @Inject(method = "onRandomTick", at = @At("HEAD"), cancellable = true)
    private void luna$noIgnition(World world, BlockPos pos, FluidState state, Random random, CallbackInfo ci) {
        if (!world.isClient() && WorldProtectionManager.isProtectedDimension(world)) ci.cancel();
    }
}
