package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonSteamKeyboard;
import net.minecraft.client.gui.GuiScreenBook;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A book and quill opens Steam's on-screen keyboard; a written book, which only reads, does not. */
@Mixin(GuiScreenBook.class)
public abstract class GuiScreenBookMixin {

    @Shadow
    @Final
    private boolean bookIsUnsigned;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void ionclient$openSteamKeyboard(CallbackInfo ci) {
        if (bookIsUnsigned) {
            IonSteamKeyboard.open();
        }
    }
}
