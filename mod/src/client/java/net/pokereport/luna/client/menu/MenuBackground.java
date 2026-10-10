package net.pokereport.luna.client.menu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.pokereport.luna.ui.PadSeason;

public final class MenuBackground {
    public static final Identifier TEXTURE = Identifier.of("lunaeternal", "textures/gui/title/background.png");
    private static final int TEX_W = 3840;
    private static final int TEX_H = 2160;
    private static final Identifier HALLOWEEN_TITLE = Identifier.of("lunaeternal", "textures/gui/title/halloween/background.png");
    private static final Identifier HALLOWEEN_LOADING = Identifier.of("lunaeternal", "textures/gui/title/halloween/loading.png");

    private MenuBackground() {}

    public static void render(DrawContext context, int width, int height) {
        boolean halloween = PadSeason.halloween();
        renderImage(context, width, height, halloween ? HALLOWEEN_TITLE : TEXTURE,
                halloween ? 1672 : TEX_W, halloween ? 941 : TEX_H);
    }

    public static void renderLoading(DrawContext context, int width, int height) {
        boolean halloween = PadSeason.halloween();
        renderImage(context, width, height, halloween ? HALLOWEEN_LOADING : TEXTURE,
                halloween ? 1672 : TEX_W, halloween ? 941 : TEX_H);
        // Keep progress, status and disconnect messages readable over the artwork.
        context.fill(0, 0, width, height, 0x38000000);
    }

    private static void renderImage(DrawContext context, int width, int height, Identifier texture, int textureWidth, int textureHeight) {
        float scale = Math.max((float) width / textureWidth, (float) height / textureHeight);
        int drawW = (int) Math.ceil(textureWidth * scale);
        int drawH = (int) Math.ceil(textureHeight * scale);
        int x = (width - drawW) / 2;
        int y = (height - drawH) / 2;
        context.drawTexture(texture, x, y, drawW, drawH, 0.0F, 0.0F, textureWidth, textureHeight, textureWidth, textureHeight);
    }

    public static void render(DrawContext context) {
        render(context, context.getScaledWindowWidth(), context.getScaledWindowHeight());
    }
}
