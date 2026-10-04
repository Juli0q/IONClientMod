package de.ionnetwork.client.mixin;

import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import de.ionnetwork.client.gui.IonCoinsOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Draws the ION coin balance on the main menu and takes the Realms button off it. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Shadow
    private RealmsNotificationsScreen realmsNotificationsScreen;

    protected TitleScreenMixin() {
        super(null);
    }

    /**
     * Drops the Realms button. A button sharing its row (the Forge/NeoForge or Mod Menu "Mods" button)
     * takes the full width; without one, the rows below move up into the gap.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void ionclient$removeRealmsButton(CallbackInfo ci) {
        // The Realms invite and news icons sit next to the Realms button, so they go with it. Every use
        // of this field is null-checked, and null keeps the icons from being drawn or fetched.
        realmsNotificationsScreen = null;
        AbstractWidget realms = null;
        for (GuiEventListener child : children()) {
            if (child instanceof AbstractWidget widget && widget.getMessage().getContents() instanceof TranslatableContents text
                    && "menu.online".equals(text.getKey())) {
                realms = widget;
            }
        }
        if (realms == null) {
            return;
        }
        removeWidget(realms);
        List<AbstractWidget> widgets = children().stream()
                .filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).toList();
        for (AbstractWidget widget : widgets) {
            if (widget.getY() == realms.getY()) {
                widget.setX(this.width / 2 - 100);
                widget.setWidth(200);
                return;
            }
        }
        for (AbstractWidget widget : widgets) {
            if (widget.getY() > realms.getY() && widget.getY() <= realms.getY() + 36) {
                widget.setY(widget.getY() - 24);
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void ionclient$renderCoins(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        IonCoinsOverlay.render(graphics, this.width, mouseX, mouseY);
    }
}
