package de.ionnetwork.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** A vanilla button whose label starts with "ION" in the logo gradient, e.g. the options screen's "ION Client...". */
public class IonBrandButton extends GuiButton {

    private final String rest;

    public IonBrandButton(int id, int x, int y, int width, int height, String rest) {
        super(id, x, y, width, height, "");
        this.rest = rest;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        super.drawButton(mc, mouseX, mouseY);
        // Vanilla's label colours for the plain part: grey when disabled, yellow on hover.
        int color = !enabled ? 0xA0A0A0 : hovered ? 0xFFFFA0 : 0xE0E0E0;
        int textWidth = IonBrandText.width(mc.fontRendererObj, rest);
        IonBrandText.draw(mc.fontRendererObj, rest, xPosition + (width - textWidth) / 2, yPosition + (height - 8) / 2, color);
    }
}
