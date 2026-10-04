package de.ionnetwork.client.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a hovering tooltip that always stays on screen. It looks like Forge's, but one too wide or too tall
 * for the screen is drawn smaller until it fits, and the box is pushed back inside every edge. Lines only
 * wrap when fitting the width would take the tooltip below half size.
 */
public final class IonTooltip {

    /** Room kept between the text and the screen edge: the 4 px border plus a 2 px gap. */
    private static final int INSET = 6;
    /** The smallest a tooltip shrinks to fit the width; anything wider wraps at this size. */
    private static final float MIN_WIDTH_SCALE = 0.5F;
    private static final float Z = 300.0F;

    private IonTooltip() {
    }

    public static void draw(List<String> text, int mouseX, int mouseY, int screenWidth, int screenHeight,
                            int maxTextWidth, FontRenderer font) {
        if (text.isEmpty()) return;

        List<String> lines = text;
        int[] titleLines = {1};
        int widest = widest(lines, font);
        if (maxTextWidth > 0 && widest > maxTextWidth) {
            lines = wrap(text, maxTextWidth, font, titleLines);
            widest = widest(lines, font);
        }

        // Too wide: shrink it, and wrap only what still does not fit at the smallest size.
        float scale = Math.min(1.0F, screenWidth / (float) (widest + 2 * INSET));
        if (scale < MIN_WIDTH_SCALE) {
            scale = MIN_WIDTH_SCALE;
            int wrapWidth = (int) (screenWidth / scale) - 2 * INSET;
            if (maxTextWidth > 0) wrapWidth = Math.min(wrapWidth, maxTextWidth);
            lines = wrap(text, wrapWidth, font, titleLines);
            widest = widest(lines, font);
        }

        int height = 8;
        if (lines.size() > 1) height += (lines.size() - 1) * 10;
        if (lines.size() > titleLines[0]) height += 2;

        // Too tall: shrink it further. Placement below happens in the scaled coordinates.
        scale = Math.min(scale, screenHeight / (float) (height + 2 * INSET));
        int width = (int) (screenWidth / scale);
        int bottom = (int) (screenHeight / scale);
        int mx = (int) (mouseX / scale);
        int my = (int) (mouseY / scale);

        int x = mx + 12;
        if (x + widest > width - INSET) x = mx - 16 - widest;
        x = Math.max(INSET, Math.min(x, width - INSET - widest));
        int y = my - 12;
        y = Math.max(INSET, Math.min(y, bottom - INSET - height));

        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0F);

        int background = 0xF0100010;
        int borderTop = 0x505000FF;
        int borderBottom = (borderTop & 0xFEFEFE) >> 1 | borderTop & 0xFF000000;
        gradient(x - 3, y - 4, x + widest + 3, y - 3, background, background);
        gradient(x - 3, y + height + 3, x + widest + 3, y + height + 4, background, background);
        gradient(x - 3, y - 3, x + widest + 3, y + height + 3, background, background);
        gradient(x - 4, y - 3, x - 3, y + height + 3, background, background);
        gradient(x + widest + 3, y - 3, x + widest + 4, y + height + 3, background, background);
        gradient(x - 3, y - 3 + 1, x - 3 + 1, y + height + 3 - 1, borderTop, borderBottom);
        gradient(x + widest + 2, y - 3 + 1, x + widest + 3, y + height + 3 - 1, borderTop, borderBottom);
        gradient(x - 3, y - 3, x + widest + 3, y - 3 + 1, borderTop, borderTop);
        gradient(x - 3, y + height + 2, x + widest + 3, y + height + 3, borderBottom, borderBottom);

        for (int i = 0; i < lines.size(); i++) {
            font.drawStringWithShadow(lines.get(i), x, y, -1);
            if (i + 1 == titleLines[0]) y += 2;
            y += 10;
        }

        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableRescaleNormal();
    }

    /** Wraps every line to the width; titleLines[0] receives how many lines the first one became. */
    private static List<String> wrap(List<String> text, int width, FontRenderer font, int[] titleLines) {
        List<String> lines = new ArrayList<String>();
        for (int i = 0; i < text.size(); i++) {
            List<String> wrapped = font.listFormattedStringToWidth(text.get(i), Math.max(width, 1));
            if (i == 0) titleLines[0] = wrapped.size();
            lines.addAll(wrapped);
        }
        return lines;
    }

    private static int widest(List<String> lines, FontRenderer font) {
        int widest = 0;
        for (String line : lines) {
            widest = Math.max(widest, font.getStringWidth(line));
        }
        return widest;
    }

    /** Gui.drawGradientRect at the tooltip's z level, as Forge's GuiUtils draws it. */
    private static void gradient(int left, int top, int right, int bottom, int startColor, int endColor) {
        float startAlpha = (startColor >> 24 & 255) / 255.0F;
        float startRed = (startColor >> 16 & 255) / 255.0F;
        float startGreen = (startColor >> 8 & 255) / 255.0F;
        float startBlue = (startColor & 255) / 255.0F;
        float endAlpha = (endColor >> 24 & 255) / 255.0F;
        float endRed = (endColor >> 16 & 255) / 255.0F;
        float endGreen = (endColor >> 8 & 255) / 255.0F;
        float endBlue = (endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        renderer.pos(right, top, Z).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        renderer.pos(left, top, Z).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        renderer.pos(left, bottom, Z).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        renderer.pos(right, bottom, Z).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
}
