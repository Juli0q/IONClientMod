package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonSteamKeyboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Closes Steam's on-screen keyboard with the screen it was opened for (see {@link IonSteamKeyboard}). */
@Mixin(Minecraft.class)
public abstract class MinecraftScreenMixin {

    @Inject(method = "displayGuiScreen", at = @At("HEAD"))
    private void ionclient$closeSteamKeyboard(GuiScreen screen, CallbackInfo ci) {
        IonSteamKeyboard.screenChanged();
    }
}
