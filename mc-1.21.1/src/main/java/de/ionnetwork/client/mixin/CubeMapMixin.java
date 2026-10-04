package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonClient;
import de.ionnetwork.client.IonPanoramas;
import de.ionnetwork.client.gui.IonIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Swaps the vanilla title-screen panorama for the ION lobby panorama the settings ask for, and back
 * to vanilla when they ask for that. The setting is followed every frame, so a change in the
 * settings screen shows right away behind its blur. The ION faces are registered through
 * {@link IonIcon} (straight from the jar) rather than the resource manager, which cannot see mod
 * assets on Fabric without Fabric API.
 */
@Mixin(CubeMap.class)
public abstract class CubeMapMixin {

    @Unique
    private static final ResourceLocation ionclient$VANILLA_FRONT = ResourceLocation.withDefaultNamespace("textures/gui/title/background/panorama_0.png");

    @Shadow
    @Final
    private ResourceLocation[] images;

    /** Vanilla's faces; null for any cube map other than the menu panorama. */
    @Unique
    private ResourceLocation[] ionclient$vanilla;
    /** The ION set the faces point at right now; null while they are vanilla's. */
    @Unique
    private String ionclient$shownSet;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ionclient$rememberVanillaFaces(ResourceLocation base, CallbackInfo ci) {
        // Only the menu panorama; leave any other cube map (e.g. from another mod) alone.
        if (ionclient$VANILLA_FRONT.equals(images[0])) {
            ionclient$vanilla = images.clone();
        }
    }

    @Inject(method = "preload", at = @At("HEAD"), cancellable = true)
    private void ionclient$preloadLobbyPanorama(TextureManager textureManager, Executor executor, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        if (ionclient$followSetting()) {
            cir.setReturnValue(CompletableFuture.completedFuture(null));
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void ionclient$ensureLobbyPanorama(Minecraft minecraft, float pitch, float yaw, float alpha, CallbackInfo ci) {
        ionclient$followSetting();
    }

    /** Points the faces at the set the settings ask for; returns whether that is an ION set (now registered). */
    @Unique
    private boolean ionclient$followSetting() {
        if (ionclient$vanilla == null) {
            return false;
        }
        String set = IonPanoramas.active();
        if (set == null ? ionclient$shownSet != null : !set.equals(ionclient$shownSet)) {
            for (int face = 0; face < images.length; face++) {
                images[face] = set == null ? ionclient$vanilla[face]
                        : ResourceLocation.fromNamespaceAndPath(IonClient.MOD_ID, IonPanoramas.faceName(set, face));
            }
            ionclient$shownSet = set;
        }
        if (set == null) {
            return false;
        }
        for (ResourceLocation face : images) {
            IonIcon.ensureRegistered(face);
        }
        return true;
    }
}
