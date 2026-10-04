package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonPanoramas;
import de.ionnetwork.client.IonSettings;
import de.ionnetwork.client.gui.IonCoinsOverlay;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.Project;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Draws the ION coin balance on the main menu, shows the ION lobby panorama the settings ask for and
 * takes the Realms button off the menu.
 */
@Mixin(GuiMainMenu.class)
public abstract class GuiMainMenuMixin extends GuiScreen implements IonCoinsOverlay.TooltipAccess {

    @Shadow
    @Final
    private static ResourceLocation[] titlePanoramaPaths;

    @Unique
    private static final int ionclient$REALMS_ID = 14;
    /** Forge's "Mods" button, which shares the Realms row. */
    @Unique
    private static final int ionclient$MODS_ID = 6;

    @Shadow
    private int panoramaTimer;

    @Unique
    private static ResourceLocation[] ionclient$vanillaPanorama;
    /** The set the faces currently point at; null for vanilla. */
    @Unique
    private static String ionclient$shownSet;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void ionclient$useLobbyPanorama(CallbackInfo ci) {
        // Copied by hand: Mixin 0.7 cannot resolve array.clone() (it looks the array type up as a class).
        ionclient$vanillaPanorama = new ResourceLocation[titlePanoramaPaths.length];
        for (int face = 0; face < titlePanoramaPaths.length; face++) {
            ionclient$vanillaPanorama[face] = titlePanoramaPaths[face];
        }
        ionclient$showPanorama(IonPanoramas.active());
    }

    /** The panorama setting can change in the settings screen, so the faces follow it every frame. */
    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void ionclient$followPanoramaSetting(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        String set = IonPanoramas.active();
        if (set == null ? ionclient$shownSet != null : !set.equals(ionclient$shownSet)) {
            ionclient$showPanorama(set);
        }
    }

    @Unique
    private static void ionclient$showPanorama(String set) {
        for (int face = 0; face < titlePanoramaPaths.length; face++) {
            titlePanoramaPaths[face] = set == null ? ionclient$vanillaPanorama[face]
                    : new ResourceLocation(IonPanoramas.NAMESPACE, IonPanoramas.facePath(set, face));
        }
        ionclient$shownSet = set;
    }

    /**
     * Drops the Realms button. Forge's Mods button on the same row takes the full width; without one,
     * the rows below move up into the gap.
     */
    @Inject(method = "initGui", at = @At("TAIL"))
    private void ionclient$removeRealmsButton(CallbackInfo ci) {
        GuiButton realms = null;
        for (GuiButton button : buttonList) {
            if (button.id == ionclient$REALMS_ID) {
                realms = button;
            }
        }
        if (realms == null) {
            return;
        }
        buttonList.remove(realms);
        for (int i = 0; i < buttonList.size(); i++) {
            GuiButton mods = buttonList.get(i);
            if (mods.id == ionclient$MODS_ID && mods.yPosition == realms.yPosition) {
                buttonList.set(i, new GuiButton(mods.id, width / 2 - 100, mods.yPosition, 200, 20, mods.displayString));
                return;
            }
        }
        for (GuiButton button : buttonList) {
            if (button.yPosition > realms.yPosition && button.yPosition <= realms.yPosition + 36) {
                button.yPosition -= 24;
            }
        }
    }

    /** The Realms invite and news icons sit next to the Realms button, so they go with it. */
    @Inject(method = "func_183501_a", at = @At("HEAD"), cancellable = true)
    private void ionclient$hideRealmsNotifications(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    /**
     * Vanilla renders the panorama into a 256x256 corner, smears it with jittered passes and
     * rotateAndBlurSkybox, then stretches it over the screen. This draws the cube once at full
     * resolution instead, with the same view vanilla crops out of that square.
     */
    @Inject(method = "renderSkybox", at = @At("HEAD"), cancellable = true)
    private void ionclient$renderSharpSkybox(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        ci.cancel();
        // Vanilla shows 15/16 of a 120 degree square across the longer screen side.
        float longHalfTan = (float) Math.tan(Math.toRadians(60.0)) * 0.9375F;
        float aspect = (float) mc.displayWidth / Math.max(1, mc.displayHeight);
        float verticalHalfTan = aspect > 1.0F ? longHalfTan / aspect : longHalfTan;
        float fovY = (float) Math.toDegrees(2.0 * Math.atan(verticalHalfTan));

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective(fovY, aspect, 0.05F, 10.0F);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        // Vanilla's extra 90 degree roll is undone by its sideways final blit, so it is left out here.
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        float time = panoramaTimer + partialTicks;
        GlStateManager.rotate((float) Math.sin(time / 400.0F) * 25.0F + 20.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-time * 0.1F, 0.0F, 1.0F, 0.0F);
        GlStateManager.disableBlend();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        for (int face = 0; face < 6; face++) {
            GlStateManager.pushMatrix();
            if (face == 1) GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
            if (face == 2) GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            if (face == 3) GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
            if (face == 4) GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            if (face == 5) GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
            mc.getTextureManager().bindTexture(titlePanoramaPaths[face]);
            // Smooth sampling now that a face covers far more pixels than it has; clamped so the cube shows no seams.
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            renderer.pos(-1.0, -1.0, 1.0).tex(0.0, 0.0).endVertex();
            renderer.pos(1.0, -1.0, 1.0).tex(1.0, 0.0).endVertex();
            renderer.pos(1.0, 1.0, 1.0).tex(1.0, 1.0).endVertex();
            renderer.pos(-1.0, 1.0, 1.0).tex(0.0, 1.0).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
        }

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void ionclient$renderCoins(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (IonSettings.showCoins()) {
            IonCoinsOverlay.render(this, mouseX, mouseY);
        }
    }

    @Override
    public void ionclient$drawTooltip(List<String> lines, int x, int y) {
        drawHoveringText(lines, x, y);
    }
}
