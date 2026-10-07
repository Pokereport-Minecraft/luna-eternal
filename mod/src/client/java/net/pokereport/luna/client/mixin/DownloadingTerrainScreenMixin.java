package net.pokereport.luna.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.pokereport.luna.client.menu.MenuBackground;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DownloadingTerrainScreen.class)
public abstract class DownloadingTerrainScreenMixin extends Screen {
    protected DownloadingTerrainScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void pokereport$customTerrainBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MenuBackground.render(context, this.width, this.height);
        ci.cancel();
    }
}
