package de.ionnetwork.client.mixin;

import de.ionnetwork.client.textures.ModernTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

/**
 * Starts preparing the modern texture pack before Minecraft lists the resource packs, so a pack
 * rebuilt during the previous session is moved into place before it is opened.
 */
@Mixin(Minecraft.class)
public abstract class ModernTexturesMixin {

    @Shadow
    public File mcDataDir;

    @Inject(method = "startGame", at = @At("HEAD"))
    private void ionclient$prepareModernTextures(CallbackInfo ci) {
        // The default pack reads vanilla textures from the classpath, so that is where 1.8.9's are.
        ModernTextures.start(mcDataDir,
                path -> DefaultResourcePack.class.getResource("/assets/minecraft/textures/" + path + ".png") != null);
    }
}
