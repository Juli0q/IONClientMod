package de.ionnetwork.client.mixin;

import de.ionnetwork.client.glow.IonGlow;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two details of the outline pass brought up to 1.9: the colour comes from the team colour (over the
 * team prefix that 1.8.9 reads, see {@link IonGlow#outlineColor}), and a glowing entity keeps its
 * outline while invisible.
 */
@Mixin(RendererLivingEntity.class)
public abstract class RendererLivingEntityMixin {

    @Shadow
    protected boolean renderOutlines;

    /** Vanilla has just set the prefix colour and turned texturing off; the colour is all that changes. */
    @Inject(method = "setScoreTeamColor", at = @At("RETURN"))
    private void ionclient$useTeamColor(EntityLivingBase entity, CallbackInfoReturnable<Boolean> cir) {
        int rgb = IonGlow.outlineColor(entity);
        GlStateManager.color((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, 1.0F);
    }

    @Redirect(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInvisible()Z"))
    private boolean ionclient$outlineInvisible(EntityLivingBase entity) {
        return entity.isInvisible() && !(renderOutlines && IonGlow.isGlowing(entity));
    }
}
