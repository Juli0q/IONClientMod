package de.ionnetwork.client.gui;

import de.ionnetwork.client.IonCoins;
import de.ionnetwork.client.IonCoinsApi;
import de.ionnetwork.client.IonCoinsStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** The coin balance in the main menu's top-right corner. */
public final class IonCoinsOverlay {

    private IonCoinsOverlay() {
    }

    public static void render(GuiGraphics graphics, int screenWidth, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        IonCoins.refreshIfStale(IonCoins.playerUuid(minecraft.getUser().getProfileId()),
                IonCoins.playerName(minecraft.getUser().getName()), IonCoinsApi::fetch);
        if (IonCoins.state() != IonCoins.State.READY || !IonIcon.ensureRegistered(IonIcon.COIN)) {
            return;
        }

        Font font = minecraft.font;
        String number = IonCoins.format(IonCoins.balance());
        int width = IonCoinsStyle.width(font.width(number));
        int left = screenWidth - IonCoinsStyle.MARGIN - width;
        int top = IonCoinsStyle.MARGIN;
        int bottom = top + IonCoinsStyle.HEIGHT;
        boolean hovered = mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < bottom;

        graphics.fill(left, top, left + width, bottom, hovered ? IonCoinsStyle.FILL_HOVER : IonCoinsStyle.FILL);
        int iconX = left + IonCoinsStyle.PAD;
        int iconY = top + IonCoinsStyle.PAD;
        graphics.blit(IonIcon.COIN, iconX, iconY, 0.0F, 0.0F, IonCoinsStyle.ICON_SIZE, IonCoinsStyle.ICON_SIZE,
                IonCoinsStyle.ICON_TEXTURE_SIZE, IonCoinsStyle.ICON_TEXTURE_SIZE);
        int textY = top + (IonCoinsStyle.HEIGHT - font.lineHeight) / 2 + 1;
        graphics.drawString(font, number, iconX + IonCoinsStyle.ICON_SIZE + IonCoinsStyle.GAP, textY, IonCoinsStyle.TEXT, true);

        if (hovered) {
            graphics.renderTooltip(font, Component.literal(IonCoinsStyle.TOOLTIP), mouseX, mouseY);
        }
    }
}
