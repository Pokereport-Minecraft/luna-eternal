package net.pokereport.luna.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.pokereport.luna.rank.RemotePcAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep LumyMon's disabled/battle/link checks; reject unauthorized remote access first. */
@Pseudo
@Mixin(targets = "com.lumyverse.lumymon.LumyMon", remap = false)
public abstract class LumyRemotePcMixin {
    @Inject(method = "openPCStorage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void luna$checkRemotePc(ServerPlayerEntity player, CallbackInfo ci) {
        if (!RemotePcAccess.check(player)) ci.cancel();
    }
}
