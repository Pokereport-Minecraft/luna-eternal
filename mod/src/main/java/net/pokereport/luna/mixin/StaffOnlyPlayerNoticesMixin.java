package net.pokereport.luna.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import net.pokereport.luna.ui.StaffPlayerNotices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public abstract class StaffOnlyPlayerNoticesMixin {
    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), cancellable = true)
    private void luna$eventsOnlyStaff(Text message, boolean overlay, CallbackInfo ci) {
        if (StaffPlayerNotices.isPlayerEvent(message)) {
            StaffPlayerNotices.dispatch((PlayerManager)(Object)this, message, overlay);
            ci.cancel();
        }
    }
    @Inject(method = {"sendToTeam", "sendToOtherTeams"}, at = @At("HEAD"), cancellable = true)
    private void luna$teamEventsOnlyStaff(PlayerEntity source, Text message, CallbackInfo ci) {
        if (StaffPlayerNotices.isPlayerEvent(message)) {
            StaffPlayerNotices.dispatch((PlayerManager)(Object)this, message, false);
            ci.cancel();
        }
    }
}
