package de.ionnetwork.client.mixin;

import de.ionnetwork.client.gui.IonCoinsOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the ION coin balance on the main menu. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin() {
        super(null);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void ionclient$renderCoins(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        IonCoinsOverlay.render(graphics, this.width, mouseX, mouseY);
    }
}
