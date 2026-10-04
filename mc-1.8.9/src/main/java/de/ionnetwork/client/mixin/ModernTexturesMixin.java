package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonClient;
import de.ionnetwork.client.textures.ModernTextures;
import de.ionnetwork.client.textures.ModernTexturesSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

/**
 * Starts preparing the modern texture pack before Minecraft lists the resource packs, so a pack
 * rebuilt during the previous session is moved into place before it is opened, and selects or
 * drops it as the ION Client setting says. That is after the options are read (they hold the
 * selected packs) and before the resource pack repository reads them.
 */
@Mixin(Minecraft.class)
public abstract class ModernTexturesMixin {

    @Shadow
    public File mcDataDir;

    @Shadow
    public GameSettings gameSettings;

    @Inject(method = "startGame", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;registerMetadataSerializers()V"))
    private void ionclient$prepareModernTextures(CallbackInfo ci) {
        if (!ModernTextures.enabled()) {
            return;
        }
        // The mod's init comes later; Forge's config folder is always <game dir>/config.
        IonClient.loadSettings(new File(mcDataDir, "config"));
        // The default pack reads vanilla textures from the classpath, so that is where 1.8.9's are.
        ModernTextures.start(mcDataDir,
                path -> DefaultResourcePack.class.getResource("/assets/minecraft/textures/" + path + ".png") != null,
                () -> {
                    // First build: select it now if the setting wants it, rather than on the next start.
                    Minecraft mc = Minecraft.getMinecraft();
                    mc.addScheduledTask(() -> ModernTexturesSelection.apply(mc));
                });
        ModernTexturesSelection.applyToOptions(gameSettings, mcDataDir);
    }
}
