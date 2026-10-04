package de.ionnetwork.client.mixin;

import de.ionnetwork.client.glow.IonGlow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the outline of every glowing entity ({@link IonGlow}), the way 1.9 does: through walls, in
 * the team colour, composited over the world after it is rendered.
 *
 * <p>1.8.9 already has the whole pipeline for its spectator outlines (hold the key as a spectator
 * and every player is outlined): the "entityOutlines" pass in {@code renderEntities} renders the
 * entities flat into a framebuffer, {@code entity_outline.json} turns that into an outline, and
 * {@code renderEntityOutlineFramebuffer} blends it over the frame. The pass only runs when
 * {@code isRenderEntityOutlines()} says so and only takes players, so the call that gates it is
 * replaced by a pass of our own that takes the spectator's players and the glowing entities, and
 * the compositing is told to run whenever that pass drew something.
 */
@Mixin(RenderGlobal.class)
public abstract class RenderGlobalMixin {

    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    @Final
    private RenderManager renderManager;

    @Shadow
    private WorldClient theWorld;

    @Shadow
    private Framebuffer entityOutlineFramebuffer;

    @Shadow
    private ShaderGroup entityOutlineShader;

    /** Whether this frame's pass drew anything, so {@code renderEntityOutlineFramebuffer} composites it. */
    @Unique
    private boolean ionclient$outlinesDrawn;

    /** Forge calls {@code renderEntities} once more per frame for its translucent pass; the outlines are done by then. */
    @Unique
    private boolean ionclient$passDone;

    @Redirect(method = "renderEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;isRenderEntityOutlines()Z"))
    private boolean ionclient$outlinePass(RenderGlobal self, Entity renderViewEntity, ICamera camera, float partialTicks) {
        if (entityOutlineFramebuffer == null || entityOutlineShader == null || mc.thePlayer == null || ionclient$passDone) {
            return false;
        }
        ionclient$passDone = true;
        boolean spectatorOutlines = mc.thePlayer.isSpectator() && mc.gameSettings.keyBindSpectatorOutlines.isKeyDown();

        Entity view = mc.getRenderViewEntity();
        boolean viewHidden = mc.gameSettings.thirdPersonView == 0
                && !(view instanceof EntityLivingBase && ((EntityLivingBase) view).isPlayerSleeping());
        double x = renderViewEntity.prevPosX + (renderViewEntity.posX - renderViewEntity.prevPosX) * partialTicks;
        double y = renderViewEntity.prevPosY + (renderViewEntity.posY - renderViewEntity.prevPosY) * partialTicks;
        double z = renderViewEntity.prevPosZ + (renderViewEntity.posZ - renderViewEntity.prevPosZ) * partialTicks;

        List<Entity> outlined = new ArrayList<>();
        for (Entity entity : theWorld.getLoadedEntityList()) {
            if (!(spectatorOutlines && entity instanceof EntityPlayer) && !IonGlow.isGlowing(entity)) {
                continue;
            }
            if (entity == view && viewHidden || !entity.isInRangeToRender3d(x, y, z)) {
                continue;
            }
            if (entity.ignoreFrustumCheck || camera.isBoundingBoxInFrustum(entity.getEntityBoundingBox())
                    || entity.riddenByEntity == mc.thePlayer) {
                outlined.add(entity);
            }
        }

        // Cleared every frame, as 1.9 does, so a spectator never composites last frame's glow.
        entityOutlineFramebuffer.framebufferClear();
        ionclient$outlinesDrawn = !outlined.isEmpty();
        if (outlined.isEmpty()) {
            mc.getFramebuffer().bindFramebuffer(false);
            return false;
        }

        // From here on this is vanilla's pass, plus the flat colour for renderers that do not
        // honour the outline flag themselves.
        theWorld.theProfiler.endStartSection("entityOutlines");
        GlStateManager.depthFunc(519);
        GlStateManager.disableFog();
        entityOutlineFramebuffer.bindFramebuffer(false);
        RenderHelper.disableStandardItemLighting();
        mc.entityRenderer.disableLightmap();
        renderManager.setRenderOutlines(true);
        for (Entity entity : outlined) {
            IonGlow.enableOutlineMode(IonGlow.outlineColor(entity));
            renderManager.renderEntitySimple(entity, partialTicks);
        }
        IonGlow.disableOutlineMode();
        renderManager.setRenderOutlines(false);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.depthMask(false);
        entityOutlineShader.loadShaderGroup(partialTicks);
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        mc.getFramebuffer().bindFramebuffer(false);
        GlStateManager.enableFog();
        GlStateManager.enableBlend();
        GlStateManager.enableColorMaterial();
        GlStateManager.depthFunc(515);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        mc.entityRenderer.enableLightmap();
        return false;
    }

    /** The only caller left is {@code renderEntityOutlineFramebuffer}: composite whenever the pass drew. */
    @Inject(method = "isRenderEntityOutlines", at = @At("RETURN"), cancellable = true)
    private void ionclient$compositeGlow(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && ionclient$outlinesDrawn) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "renderEntityOutlineFramebuffer", at = @At("TAIL"))
    private void ionclient$endFrame(CallbackInfo ci) {
        ionclient$outlinesDrawn = false;
        ionclient$passDone = false;
    }
}
