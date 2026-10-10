package net.pokereport.luna.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.ReconfiguringScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.text.Text;
import net.pokereport.luna.client.menu.MenuBackground;
import org.spongepowered.asm.mixin.Mixin;

/** These screens inherit renderBackground; add an override rather than an invalid injector. */
@Mixin({DisconnectedScreen.class, ProgressScreen.class, ReconfiguringScreen.class, LevelLoadingScreen.class})
public abstract class TransitionScreenMixin extends Screen {
    protected TransitionScreenMixin(Text title) { super(title); }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        MenuBackground.renderLoading(context, this.width, this.height);
    }
}
