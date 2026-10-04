package de.ionnetwork.client.gui;

import de.ionnetwork.client.IonEntryStyle;
import de.ionnetwork.client.IonSettingsMenu;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/** "ION" in the logo gradient followed by plain text, and the gradient line under the settings header. */
public final class IonBrandText {

    private IonBrandText() {
    }

    public static int width(FontRenderer font, String rest) {
        return font.getStringWidth(IonEntryStyle.BRAND_WORD + rest);
    }

    /** Draws {@code ION<rest>} with a shadow, the brand letters in the gradient and {@code rest} in {@code restColor}. */
    public static void draw(FontRenderer font, String rest, int x, int y, int restColor) {
        String brand = IonEntryStyle.BRAND_WORD;
        for (int i = 0; i < brand.length(); i++) {
            String letter = String.valueOf(brand.charAt(i));
            font.drawStringWithShadow(letter, x, y, IonEntryStyle.brandLetterColor(i));
            x += font.getStringWidth(letter);
        }
        font.drawStringWithShadow(rest, x, y, restColor);
    }

    /** A horizontal strip sweeping the logo gradient from {@code left} to {@code right}. */
    public static void drawGradientLine(int left, int top, int right, int bottom, int screenWidth) {
        int stops = 8;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < stops; i++) {
            int x0 = left + (right - left) * i / stops;
            int x1 = left + (right - left) * (i + 1) / stops;
            int c0 = IonSettingsMenu.separatorColor(x0, screenWidth);
            int c1 = IonSettingsMenu.separatorColor(x1, screenWidth);
            vertex(renderer, x1, top, c1);
            vertex(renderer, x0, top, c0);
            vertex(renderer, x0, bottom, c0);
            vertex(renderer, x1, bottom, c1);
        }
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    private static void vertex(WorldRenderer renderer, int x, int y, int argb) {
        renderer.pos(x, y, 0.0D).color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, argb >>> 24).endVertex();
    }
}
