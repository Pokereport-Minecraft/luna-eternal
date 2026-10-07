package net.pokereport.luna.mixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pokereport.luna.world.WorldProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin({ArmorStandEntity.class, BlockAttachedEntity.class, ItemFrameEntity.class})
public abstract class SafeDecorationMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void luna$protectDecorations(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (!entity.getWorld().isClient() && WorldProtectionManager.isProtectedDimension(entity.getWorld())
                && !(source.getAttacker() instanceof ServerPlayerEntity player
                && WorldProtectionManager.canModifyWorld(player, entity.getWorld()))) cir.setReturnValue(false);
    }
}
