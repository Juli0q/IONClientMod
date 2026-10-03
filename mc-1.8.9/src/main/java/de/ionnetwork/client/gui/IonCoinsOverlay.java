package de.ionnetwork.client.gui;

import de.ionnetwork.client.IonCoins;
import de.ionnetwork.client.IonCoinsApi;
import de.ionnetwork.client.IonCoinsStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

import java.util.Collections;

/** The coin balance in the main menu's top-right corner. */
public final class IonCoinsOverlay {

    private IonCoinsOverlay() {
    }

    public static void render(GuiScreen screen, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getMinecraft();
        IonCoins.refreshIfStale(IonCoins.playerUuid(mc.getSession().getProfile().getId()),
                IonCoins.playerName(mc.getSession().getUsername()), IonCoinsApi::fetch);
        if (IonCoins.state() != IonCoins.State.READY || !IonIcon.ensureRegistered(IonIcon.COIN)) {
            return;
        }

        FontRenderer font = mc.fontRendererObj;
        String number = IonCoins.format(IonCoins.balance());
        int width = IonCoinsStyle.width(font.getStringWidth(number));
        int left = screen.width - IonCoinsStyle.MARGIN - width;
        int top = IonCoinsStyle.MARGIN;
        int bottom = top + IonCoinsStyle.HEIGHT;
        boolean hovered = mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < bottom;

        Gui.drawRect(left, top, left + width, bottom, hovered ? IonCoinsStyle.FILL_HOVER : IonCoinsStyle.FILL);
        int iconX = left + IonCoinsStyle.PAD;
        int iconY = top + IonCoinsStyle.PAD;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(IonIcon.COIN);
        GlStateManager.enableBlend();
        Gui.drawModalRectWithCustomSizedTexture(iconX, iconY, 0.0F, 0.0F, IonCoinsStyle.ICON_SIZE, IonCoinsStyle.ICON_SIZE,
                IonCoinsStyle.ICON_TEXTURE_SIZE, IonCoinsStyle.ICON_TEXTURE_SIZE);
        GlStateManager.disableBlend();
        int textY = top + (IonCoinsStyle.HEIGHT - font.FONT_HEIGHT) / 2 + 1;
        font.drawStringWithShadow(number, iconX + IonCoinsStyle.ICON_SIZE + IonCoinsStyle.GAP, textY, IonCoinsStyle.TEXT);

        if (hovered) {
            ((TooltipAccess) screen).ionclient$drawTooltip(Collections.singletonList(IonCoinsStyle.TOOLTIP), mouseX, mouseY);
        }
    }

    /** Implemented by the main-menu mixin to expose GuiScreen's protected tooltip drawing. */
    public interface TooltipAccess {
        void ionclient$drawTooltip(java.util.List<String> lines, int x, int y);
    }
}
