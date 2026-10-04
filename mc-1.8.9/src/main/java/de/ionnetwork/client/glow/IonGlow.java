package de.ionnetwork.client.glow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;

/**
 * The glowing effect of Minecraft 1.9 and later, read from the protocol as a 1.9 client reads it.
 *
 * <p>ION's servers mark entities as glowing the way the current protocol does: bit 6 of the entity
 * flags byte (the one that carries on fire, sneaking, sprinting and invisible). That byte reaches a
 * 1.8 client untouched, whether it comes from a modern server through ViaVersion (ViaRewind passes
 * the byte through) or from a 1.8 server that sets the bit itself; vanilla 1.8.9 just never looks at
 * the bit. The outline is drawn with the machinery 1.8.9 already has for spectator outlines (the
 * entity outline framebuffer and shader), extended by {@code RenderGlobalMixin} to glowing entities
 * and coloured the 1.9 way: the team's colour, which the Teams packet has carried since 1.8, over the
 * prefix colour that 1.8.9 used, over white.
 *
 * <p>The GL calls live in this plain class rather than in a mixin: on the LWJGL3 runtime the
 * coremod rewrites LWJGL2-only signatures such as {@code glTexEnv(int, int, FloatBuffer)} while
 * classes load, and merged mixin code never passes through it.
 */
public final class IonGlow {

    /** Bit 6 of the entity flags byte: "glowing" since 1.9. */
    private static final int GLOWING_FLAG = 6;
    private static final int WHITE = 0xFFFFFF;
    /** {@code -Dionclient.glow=false} turns the outlines off, should they ever misbehave on a GPU. */
    private static final boolean ENABLED = !"false".equalsIgnoreCase(System.getProperty("ionclient.glow"));
    /** Development aid: {@code -Dionclient.glowtest=true} makes every entity but the camera's glow. */
    private static final boolean TEST_ALL = Boolean.getBoolean("ionclient.glowtest");

    private static final FloatBuffer OUTLINE_COLOR = BufferUtils.createFloatBuffer(4);

    private IonGlow() {
    }

    /** Whether the server flagged the entity as glowing. */
    public static boolean isGlowing(Entity entity) {
        if (!ENABLED || entity == null || entity.isDead) {
            return false;
        }
        if (TEST_ALL) {
            return entity != Minecraft.getMinecraft().getRenderViewEntity();
        }
        DataWatcher watcher = entity.getDataWatcher();
        if (watcher == null) {
            return false;
        }
        try {
            return (watcher.getWatchableObjectByte(0) & 1 << GLOWING_FLAG) != 0;
        } catch (RuntimeException e) {
            // A modded entity that never registered the flags byte.
            return false;
        }
    }

    /**
     * The outline colour as 1.9 picks it: the team's colour, else white. 1.8.9's spectator outlines
     * read the colour from the team prefix instead, so that stays as the fallback for teams that only
     * set a coloured prefix.
     */
    public static int outlineColor(Entity entity) {
        Team team = entity instanceof EntityLivingBase ? ((EntityLivingBase) entity).getTeam() : null;
        if (!(team instanceof ScorePlayerTeam)) {
            return WHITE;
        }
        ScorePlayerTeam scoreTeam = (ScorePlayerTeam) team;
        EnumChatFormatting color = scoreTeam.getChatFormat();
        if (color != null && color.isColor()) {
            return colorCode(color.toString().charAt(1));
        }
        String prefix = FontRenderer.getFormatFromString(scoreTeam.getColorPrefix());
        return prefix.length() >= 2 ? colorCode(prefix.charAt(1)) : WHITE;
    }

    private static int colorCode(char code) {
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        return font == null ? WHITE : font.getColorCode(code);
    }

    /**
     * Makes every textured draw on the default texture unit come out in one flat colour, keeping the
     * texture's alpha (so a sword or an arrow keeps its shape). This is what 1.9's
     * {@code GlStateManager.enableOutlineMode} does; it colours the renderers that
     * {@code RendererLivingEntity}'s team-colour path does not cover, such as dropped items,
     * arrows, boats, minecarts and item frames.
     */
    public static void enableOutlineMode(int rgb) {
        OUTLINE_COLOR.put(0, (rgb >> 16 & 0xFF) / 255.0F);
        OUTLINE_COLOR.put(1, (rgb >> 8 & 0xFF) / 255.0F);
        OUTLINE_COLOR.put(2, (rgb & 0xFF) / 255.0F);
        OUTLINE_COLOR.put(3, 1.0F);
        GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, OUTLINE_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL11.GL_REPLACE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL13.GL_CONSTANT);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_ALPHA, GL11.GL_REPLACE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_ALPHA, GL11.GL_TEXTURE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_ALPHA, GL11.GL_SRC_ALPHA);
    }

    /** Back to the texture environment Minecraft assumes everywhere. */
    public static void disableOutlineMode() {
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
    }
}
