package net.pokereport.luna.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import net.pokereport.luna.ui.StaffPlayerNotices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Evita que el anuncio vanilla de entrada se envíe a jugadores normales. */
@Mixin(PlayerManager.class)
public abstract class StaffOnlyJoinMessageMixin {

    @Redirect(
            method = "onPlayerConnect",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V",
                    ordinal = 0))
    private void luna$joinSoloStaff(PlayerManager manager, Text mensaje, boolean overlay) {
        StaffPlayerNotices.dispatch(manager, mensaje, overlay);
    }
}
