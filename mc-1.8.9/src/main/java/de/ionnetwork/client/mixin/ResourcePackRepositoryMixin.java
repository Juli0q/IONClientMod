package de.ionnetwork.client.mixin;

import de.ionnetwork.client.textures.ModernTexturesSelection;
import net.minecraft.client.resources.ResourcePackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Carries a resource pack selection made in the Resource Packs screen over to the Modern Textures setting. */
@Mixin(ResourcePackRepository.class)
public abstract class ResourcePackRepositoryMixin {

    @Inject(method = "setRepositories", at = @At("TAIL"))
    private void ionclient$syncModernTextures(List<ResourcePackRepository.Entry> repositories, CallbackInfo ci) {
        ModernTexturesSelection.selectionChanged(repositories);
    }
}
