package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonSteamKeyboard;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiEditSign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Screens that are only there to type in open Steam's on-screen keyboard right away (see {@link IonSteamKeyboard}). */
@Mixin({GuiChat.class, GuiEditSign.class})
public abstract class GuiTypingScreenMixin {

    @Inject(method = "initGui", at = @At("TAIL"))
    private void ionclient$openSteamKeyboard(CallbackInfo ci) {
        IonSteamKeyboard.open();
    }
}
