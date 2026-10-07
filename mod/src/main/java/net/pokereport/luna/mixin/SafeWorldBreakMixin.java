package net.pokereport.luna.mixin;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.pokereport.luna.world.WorldProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(World.class)
public abstract class SafeWorldBreakMixin {
    @Inject(method = "breakBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;I)Z", at = @At("HEAD"), cancellable = true)
    private void luna$noEnvironmentalBreak(BlockPos pos, boolean drops, Entity entity, int depth, CallbackInfoReturnable<Boolean> cir) {
        World world = (World) (Object) this;
        if (!world.isClient() && WorldProtectionManager.isProtectedDimension(world)
                && !(entity instanceof ServerPlayerEntity player && WorldProtectionManager.canModifyWorld(player, world))) {
            cir.setReturnValue(false);
        }
    }
}
