package net.pokereport.luna.mixin;

import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.TeleportTarget;
import net.pokereport.luna.world.RestrictedDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class RestrictedDimensionsMixin {
    @Inject(method = "teleportTo", at = @At("HEAD"), cancellable = true)
    private void luna$portal(TeleportTarget target, CallbackInfoReturnable<Entity> cir) {
        var player = (ServerPlayerEntity)(Object)this;
        if (RestrictedDimensions.denied(player, target.world())) {
            RestrictedDimensions.notifyDenied(player);
            cir.setReturnValue(null);
        }
    }
    @Inject(method = "teleport(Lnet/minecraft/server/world/ServerWorld;DDDLjava/util/Set;FF)Z",
            at = @At("HEAD"), cancellable = true)
    private void luna$command(ServerWorld world, double x, double y, double z,
            Set<PositionFlag> flags, float yaw, float pitch, CallbackInfoReturnable<Boolean> cir) {
        var player = (ServerPlayerEntity)(Object)this;
        if (RestrictedDimensions.denied(player, world)) {
            RestrictedDimensions.notifyDenied(player);
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "teleport(Lnet/minecraft/server/world/ServerWorld;DDDFF)V",
            at = @At("HEAD"), cancellable = true)
    private void luna$direct(ServerWorld world, double x, double y, double z,
            float yaw, float pitch, CallbackInfo ci) {
        var player = (ServerPlayerEntity)(Object)this;
        if (RestrictedDimensions.denied(player, world)) {
            RestrictedDimensions.notifyDenied(player);
            ci.cancel();
        }
    }
}
