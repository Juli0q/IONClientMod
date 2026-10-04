package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonSteamKeyboard;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opens Steam's on-screen keyboard when the player clicks into a text field (see {@link IonSteamKeyboard}). */
@Mixin(GuiTextField.class)
public abstract class GuiTextFieldMixin {

    @Shadow
    public int xPosition;
    @Shadow
    public int yPosition;
    @Shadow
    @Final
    private int width;
    @Shadow
    @Final
    private int height;
    @Shadow
    private boolean isFocused;
    @Shadow
    private boolean isEnabled;
    @Shadow
    private boolean visible;

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void ionclient$openSteamKeyboard(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        boolean inside = mouseX >= xPosition && mouseX < xPosition + width && mouseY >= yPosition && mouseY < yPosition + height;
        if (inside && isFocused && isEnabled && visible) {
            IonSteamKeyboard.open();
        }
    }
}
