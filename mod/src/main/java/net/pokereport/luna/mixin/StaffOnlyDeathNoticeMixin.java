package net.pokereport.luna.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;
import net.pokereport.luna.ui.StaffPlayerNotices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Staff still receives deaths when the world's rule or team hides vanilla announcements. */
@Mixin(ServerPlayerEntity.class)
public abstract class StaffOnlyDeathNoticeMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void luna$hiddenDeathForStaff(DamageSource source, CallbackInfo ci) {
        var player = (ServerPlayerEntity)(Object)this;
        var team = player.getScoreboardTeam();
        if (!player.getWorld().getGameRules().getBoolean(GameRules.SHOW_DEATH_MESSAGES)
                || (team != null && team.getDeathMessageVisibilityRule() == AbstractTeam.VisibilityRule.NEVER)) {
            StaffPlayerNotices.dispatch(player.getServer().getPlayerManager(), player.getDamageTracker().getDeathMessage(), false);
        }
    }
}
