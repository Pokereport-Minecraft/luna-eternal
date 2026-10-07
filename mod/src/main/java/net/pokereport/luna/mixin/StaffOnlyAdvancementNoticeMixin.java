package net.pokereport.luna.mixin;

import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;
import net.pokereport.luna.ui.StaffPlayerNotices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerAdvancementTracker.class)
public abstract class StaffOnlyAdvancementNoticeMixin {
    @Shadow private ServerPlayerEntity owner;
    // Yarn 1.21.1: private helper for chat announcements in grantCriterion.
    @Inject(method = "method_53637", at = @At("HEAD"))
    private void luna$hiddenAdvancementForStaff(AdvancementEntry entry, AdvancementDisplay display, CallbackInfo ci) {
        if (display.shouldAnnounceToChat()
                && !owner.getWorld().getGameRules().getBoolean(GameRules.ANNOUNCE_ADVANCEMENTS)) {
            StaffPlayerNotices.dispatch(owner.getServer().getPlayerManager(),
                    display.getFrame().getChatAnnouncementText(entry, owner), false);
        }
    }
}
