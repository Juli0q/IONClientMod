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
 * Swaps the vanilla title-screen panorama for this launch's ION lobby panorama. The faces are
 * registered through {@link IonIcon} (straight from the jar) rather than the resource manager,
 * which cannot see mod assets on Fabric without Fabric API.
 */
@Mixin(CubeMap.class)
public abstract class CubeMapMixin {

    @Unique
    private static final ResourceLocation ionclient$VANILLA_FRONT = ResourceLocation.withDefaultNamespace("textures/gui/title/background/panorama_0.png");

    @Shadow
    @Final
    private ResourceLocation[] images;

    @Unique
    private boolean ionclient$lobby;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ionclient$useLobbyPanorama(ResourceLocation base, CallbackInfo ci) {
        // Only the menu panorama; leave any other cube map (e.g. from another mod) alone.
        if (!ionclient$VANILLA_FRONT.equals(images[0])) {
            return;
        }
        ionclient$lobby = true;
        for (int face = 0; face < images.length; face++) {
            images[face] = ResourceLocation.fromNamespaceAndPath(IonClient.MOD_ID, IonPanoramas.faceName(face));
        }
    }

    @Inject(method = "preload", at = @At("HEAD"), cancellable = true)
    private void ionclient$preloadLobbyPanorama(TextureManager textureManager, Executor executor, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        if (ionclient$lobby) {
            ionclient$registerFaces();
            cir.setReturnValue(CompletableFuture.completedFuture(null));
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void ionclient$ensureLobbyPanorama(Minecraft minecraft, float pitch, float yaw, float alpha, CallbackInfo ci) {
        if (ionclient$lobby) {
            ionclient$registerFaces();
        }
    }

    @Unique
    private void ionclient$registerFaces() {
        for (ResourceLocation face : images) {
            IonIcon.ensureRegistered(face);
        }
    }
}
