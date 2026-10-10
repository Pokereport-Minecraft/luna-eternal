package net.pokereport.luna.client.pokepad;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.List;

/** One font for drawing AND measurement. No enlarged shadow or four-copy outline. */
public final class PadTypography {
    public static final Identifier FONT = Identifier.of("lunaeternal", "pokepad");
    private PadTypography() {}

    /** Saturated section accents need a darker variant over the light screen. */
    public static int onLight(int color) {
        return (color & 0xFF000000) | (Math.round(((color >> 16) & 255) * 0.52f) << 16)
                | (Math.round(((color >> 8) & 255) * 0.52f) << 8)
                | Math.round((color & 255) * 0.52f);
    }

    public static Text styled(Text text) {
        return text.copy().fillStyle(Style.EMPTY.withFont(FONT));
    }
    private static OrderedText styled(OrderedText text) {
        return visitor -> text.accept((index, style, codepoint) -> visitor.accept(index,
                style.getFont().equals(Style.DEFAULT_FONT_ID) ? style.withFont(FONT) : style, codepoint));
    }
    public static int width(TextRenderer renderer, Text text) { return renderer.getWidth(styled(text)); }
    public static int width(TextRenderer renderer, String text) { return width(renderer, Text.literal(text)); }
    public static int width(TextRenderer renderer, OrderedText text) { return renderer.getWidth(styled(text)); }

    public static int draw(DrawContext ctx, TextRenderer renderer, Text text, int x, int y, int color, boolean shadow) {
        return ctx.drawText(renderer, styled(text), x, y, color, false);
    }
    public static int draw(DrawContext ctx, TextRenderer renderer, String text, int x, int y, int color, boolean shadow) {
        return draw(ctx, renderer, Text.literal(text), x, y, color, false);
    }
    public static int draw(DrawContext ctx, TextRenderer renderer, OrderedText text, int x, int y, int color, boolean shadow) {
        return ctx.drawText(renderer, styled(text), x, y, color, false);
    }
    public static int drawShadow(DrawContext ctx, TextRenderer renderer, Text text, int x, int y, int color) {
        return draw(ctx, renderer, text, x, y, color, false);
    }
    public static int drawShadow(DrawContext ctx, TextRenderer renderer, String text, int x, int y, int color) {
        return draw(ctx, renderer, text, x, y, color, false);
    }
    public static int drawShadow(DrawContext ctx, TextRenderer renderer, OrderedText text, int x, int y, int color) {
        return draw(ctx, renderer, text, x, y, color, false);
    }
    public static void centered(DrawContext ctx, TextRenderer renderer, Text text, int x, int y, int color) {
        draw(ctx, renderer, text, x - width(renderer, text) / 2, y, color, false);
    }
    public static void centered(DrawContext ctx, TextRenderer renderer, String text, int x, int y, int color) {
        centered(ctx, renderer, Text.literal(text), x, y, color);
    }
    public static void tooltip(DrawContext ctx, TextRenderer renderer, Text text, int x, int y) {
        ctx.drawTooltip(renderer, styled(text), x, y);
    }
    public static void tooltip(DrawContext ctx, TextRenderer renderer, List<Text> lines, int x, int y) {
        ctx.drawTooltip(renderer, lines.stream().map(PadTypography::styled).toList(), x, y);
    }
}
