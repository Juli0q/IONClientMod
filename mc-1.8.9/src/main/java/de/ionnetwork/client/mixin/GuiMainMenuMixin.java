package de.ionnetwork.client.mixin;

import de.ionnetwork.client.gui.IonCoinsOverlay;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Draws the ION coin balance on the main menu. */
@Mixin(GuiMainMenu.class)
public abstract class GuiMainMenuMixin extends GuiScreen implements IonCoinsOverlay.TooltipAccess {

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void ionclient$renderCoins(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        IonCoinsOverlay.render(this, mouseX, mouseY);
    }

    @Override
    public void ionclient$drawTooltip(List<String> lines, int x, int y) {
        drawHoveringText(lines, x, y);
    }
}
