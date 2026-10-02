package net.pokereport.luna.mixin;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.pokereport.luna.world.WorldProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FireBlock.class)
public abstract class SafeFireMixin {
    @Inject(method = "scheduledTick", at = @At("HEAD"), cancellable = true)
    private void luna$noSpread(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (WorldProtectionManager.isProtectedDimension(world)) ci.cancel();
    }
}
