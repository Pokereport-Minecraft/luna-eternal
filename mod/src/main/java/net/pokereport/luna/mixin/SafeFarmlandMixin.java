package net.pokereport.luna.mixin;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.pokereport.luna.world.WorldProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FarmlandBlock.class)
public abstract class SafeFarmlandMixin {
    @Inject(method = "setToDirt", at = @At("HEAD"), cancellable = true)
    private static void luna$keepSoil(Entity entity, BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        if (!world.isClient() && WorldProtectionManager.isProtectedDimension(world)) ci.cancel();
    }
}
