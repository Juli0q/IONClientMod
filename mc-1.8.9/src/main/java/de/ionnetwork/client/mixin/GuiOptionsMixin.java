package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonSettingsMenu;
import de.ionnetwork.client.gui.IonBrandButton;
import de.ionnetwork.client.gui.IonSettingsScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts an "ION Client..." button into Options, in the slot of "Broadcast Settings...": Twitch shut
 * down the API 1.8.9 streams through, so that button only ever opens an "unavailable" notice. If
 * another mod already took that slot, the button gets a row of its own above Done instead.
 */
@Mixin(GuiOptions.class)
public abstract class GuiOptionsMixin extends GuiScreen {

    @Unique
    private static final int ionclient$BROADCAST_ID = 107;
    @Unique
    private static final int ionclient$DONE_ID = 200;
    /** "ION" in ASCII; far from vanilla's ids and the usual mod ones. */
    @Unique
    private static final int ionclient$BUTTON_ID = 0x494F4E;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void ionclient$addSettingsButton(CallbackInfo ci) {
        for (int i = 0; i < buttonList.size(); i++) {
            GuiButton broadcast = buttonList.get(i);
            if (broadcast.id == ionclient$BROADCAST_ID) {
                buttonList.set(i, new IonBrandButton(ionclient$BUTTON_ID, broadcast.xPosition, broadcast.yPosition,
                        broadcast.getButtonWidth(), IonSettingsMenu.BUTTON_HEIGHT, IonSettingsMenu.BUTTON_REST));
                return;
            }
        }
        int y = height / 6 + 168;
        for (GuiButton button : buttonList) {
            if (button.id == ionclient$DONE_ID) {
                y = button.yPosition;
                button.yPosition += 24;
            }
        }
        buttonList.add(new IonBrandButton(ionclient$BUTTON_ID, width / 2 - 155, y, 310, IonSettingsMenu.BUTTON_HEIGHT,
                IonSettingsMenu.BUTTON_REST));
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void ionclient$openSettings(GuiButton button, CallbackInfo ci) {
        if (button.enabled && button.id == ionclient$BUTTON_ID) {
            mc.gameSettings.saveOptions();
            mc.displayGuiScreen(new IonSettingsScreen(this));
            ci.cancel();
        }
    }
}
