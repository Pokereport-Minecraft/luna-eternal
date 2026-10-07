package net.pokereport.luna.client.mixin;

import com.cobblemon.mod.common.client.gui.PartyOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** El inicial se entrega con Oak: no anunciar el selector genérico de Cobblemon. */
@Mixin(PartyOverlay.class)
public abstract class StarterToastMixin {
    @Shadow(remap = false)
    private boolean attachedToast;

    @Inject(method = "render", at = @At("HEAD"))
    private void luna$omitirAvisoInicial(DrawContext context,
                                       RenderTickCounter tickCounter,
                                       CallbackInfo ci) {
        // Cobblemon vuelve a ponerlo en false al sincronizar/reconectar.
        // Solo evita encolar starterToast; no cambia starterSelected ni el equipo.
        attachedToast = true;
    }
}
