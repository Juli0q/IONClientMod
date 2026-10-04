package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonSteamKeyboard;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the chat input above Steam's docked on-screen keyboard: while it covers the bottom of the
 * screen (see {@link IonSteamKeyboard#chatLift}), the chat screen is drawn that much higher and the
 * mouse is moved down by the same amount, so hovering and clicking still hit what is drawn under
 * the pointer. The messages above it are drawn by the HUD and moved in {@code IonClientForge}.
 */
@Mixin(GuiChat.class)
public abstract class GuiChatMixin extends GuiScreen {

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void ionclient$liftAboveKeyboard(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, -IonSteamKeyboard.chatLift(height), 0.0F);
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void ionclient$endLift(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        GlStateManager.popMatrix();
    }

    @ModifyVariable(method = {"drawScreen", "mouseClicked"}, at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int ionclient$liftMouse(int mouseY) {
        return mouseY + IonSteamKeyboard.chatLift(height);
    }

    /** The message under the pointer is looked up from the raw mouse position, in window pixels from the bottom. */
    @ModifyArg(method = {"drawScreen", "mouseClicked"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiNewChat;getChatComponent(II)Lnet/minecraft/util/IChatComponent;"), index = 1)
    private int ionclient$liftRawMouse(int rawY) {
        return rawY - IonSteamKeyboard.chatLift(height) * new ScaledResolution(mc).getScaleFactor();
    }
}
