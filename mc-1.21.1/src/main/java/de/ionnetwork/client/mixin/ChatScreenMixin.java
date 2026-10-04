package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonSteamKeyboard;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the chat above Steam's docked on-screen keyboard: while it covers the bottom of the screen
 * (see {@link IonSteamKeyboard#chatLift}), the chat screen, which draws the messages, the input
 * and the command suggestions, is drawn that much higher and the mouse is moved down by the same
 * amount, so hovering and clicking still hit what is drawn under the pointer.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    protected ChatScreenMixin() {
        super(null);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void ionclient$liftAboveKeyboard(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, -IonSteamKeyboard.chatLift(height), 0.0F);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void ionclient$endLift(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        graphics.pose().popPose();
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int ionclient$liftMouse(int mouseY) {
        return mouseY + IonSteamKeyboard.chatLift(height);
    }

    @ModifyVariable(method = {"mouseClicked", "mouseScrolled"}, at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double ionclient$liftMouse(double mouseY) {
        return mouseY + IonSteamKeyboard.chatLift(height);
    }
}
