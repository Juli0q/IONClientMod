package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonSteamKeyboard;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opens Steam's on-screen keyboard when the player clicks into a text field (see {@link IonSteamKeyboard}). */
@Mixin(EditBox.class)
public abstract class EditBoxMixin {

    @Shadow
    private boolean isEditable;

    @Shadow
    public abstract boolean isVisible();

    @Inject(method = "onClick", at = @At("HEAD"))
    private void ionclient$openSteamKeyboard(double mouseX, double mouseY, CallbackInfo ci) {
        if (isEditable && isVisible()) {
            IonSteamKeyboard.open();
        }
    }
}
