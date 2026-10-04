package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonSteamKeyboard;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Screens that are only there to type in open Steam's on-screen keyboard right away (see {@link IonSteamKeyboard}). */
@Mixin({ChatScreen.class, AbstractSignEditScreen.class, BookEditScreen.class})
public abstract class TypingScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void ionclient$openSteamKeyboard(CallbackInfo ci) {
        IonSteamKeyboard.open();
    }
}
