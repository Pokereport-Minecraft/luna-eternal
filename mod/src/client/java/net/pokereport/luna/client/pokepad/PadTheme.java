package net.pokereport.luna.client.pokepad;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.pokereport.luna.ui.PadSeason;

/** Actual seasonal PNG variants; originals and interactive coordinates remain intact. */
public final class PadTheme {
    private static long nextCheck;
    private static boolean halloween;
    private PadTheme() {}

    public static boolean active() {
        long now = System.currentTimeMillis();
        if (now >= nextCheck) { halloween = PadSeason.halloween(); nextCheck = now + 1000; }
        return halloween;
    }
    public static int color(int original) {
        if (!active()) return original;
        return switch (original) {
            case 0xFFE2EBFD -> 0xFFF3E7D6;
            case 0xFFBFCBE8 -> 0xFFE0CCE8;
            case 0xFFC2CCE4 -> 0xFFE1D3E7;
            case 0xFF7C89B4, 0xFF96A1C0 -> 0xFF977BA5;
            case 0xFFFFF0DC -> 0xFFFFECD3;
            case 0xFF1B2333 -> 0xFF25182F;
            case 0xFF26314A -> 0xFF382345;
            case 0xFF141A26 -> 0xFF1B1324;
            case 0xFF0A0E16 -> 0xFF130F1C;
            case 0xFF1C2536 -> 0xFF2B1C38;
            case 0xFF35496A -> 0xFF73527D;
            case 0xFF3C4250 -> 0xFF594364;
            default -> original;
        };
    }
    public static void fill(DrawContext ctx, int x0, int y0, int x1, int y1, int color) {
        ctx.fill(x0, y0, x1, y1, color(color));
    }
    public static Identifier resolve(Identifier original) {
        if (!active() || !original.getNamespace().equals("lunaeternal")) return original;
        String path = original.getPath();
        String prefix = "textures/gui/";
        if (!path.startsWith(prefix) || path.contains("/themes/")) return original;
        String relative = path.substring(prefix.length());
        if (relative.startsWith("pokepad/")) relative = relative.substring("pokepad/".length());
        Identifier seasonal = Identifier.of("lunaeternal", "textures/gui/pokepad/themes/halloween/" + relative);
        // Supports resource-pack reloads and leaves externally supplied images untouched.
        return MinecraftClient.getInstance().getResourceManager().getResource(seasonal).isPresent()
                ? seasonal : original;
    }
    public static void texture(DrawContext ctx, Identifier tex, int x, int y, int w, int h,
                               float u, float v, int rw, int rh, int tw, int th) {
        ctx.drawTexture(resolve(tex), x, y, w, h, u, v, rw, rh, tw, th);
    }
    public static void texture(DrawContext ctx, Identifier tex, int x, int y,
                               float u, float v, int w, int h, int tw, int th) {
        texture(ctx,tex,x,y,w,h,u,v,w,h,tw,th);
    }
}
