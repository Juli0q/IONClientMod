package de.ionnetwork.client.gui;

import de.ionnetwork.client.IonClient;
import de.ionnetwork.client.IonSettings;
import de.ionnetwork.client.IonSettingsMenu;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ION Client's settings, opened from the "ION Client..." button in Options (or Forge's mod list).
 * Laid out by {@link IonSettingsMenu}; saved when the screen closes, like vanilla's options.
 */
public class IonSettingsScreen extends GuiScreen {

    private static final int DONE_ID = 200;

    private final GuiScreen parent;
    private final List<IonSettingsMenu.Item> items = new ArrayList<>();
    private IonSettingsMenu.Layout layout;
    /** The link waiting on vanilla's "open this link?" prompt. */
    private String pendingLink;

    public IonSettingsScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        items.clear();
        layout = IonSettingsMenu.layout(IonSettingsMenu.sections(IonDisplayScale.get()), width, height);
        for (IonSettingsMenu.Slot slot : layout.slots) {
            GuiButton button;
            if (slot.item instanceof IonSettingsMenu.Choice) {
                IonSettingsMenu.Choice choice = (IonSettingsMenu.Choice) slot.item;
                button = new GuiButton(items.size(), slot.x, slot.y, slot.width, IonSettingsMenu.BUTTON_HEIGHT, choice.message());
                button.enabled = choice.enabled();
            } else {
                button = new GuiButton(items.size(), slot.x, slot.y, slot.width, IonSettingsMenu.BUTTON_HEIGHT, slot.item.label);
            }
            items.add(slot.item);
            buttonList.add(button);
        }
        buttonList.add(new GuiButton(DONE_ID, width / 2 - IonSettingsMenu.DONE_WIDTH / 2, IonSettingsMenu.doneY(height),
                IonSettingsMenu.DONE_WIDTH, IonSettingsMenu.BUTTON_HEIGHT, I18n.format("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) {
            return;
        }
        if (button.id == DONE_ID) {
            mc.displayGuiScreen(parent);
            return;
        }
        IonSettingsMenu.Item item = items.get(button.id);
        if (item instanceof IonSettingsMenu.Choice) {
            IonSettingsMenu.Choice choice = (IonSettingsMenu.Choice) item;
            choice.cycle(isShiftKeyDown());
            button.displayString = choice.message();
            if (choice.resizesScreen) {
                // The GUI scale changed: lay the screen out again at its new scaled size.
                ScaledResolution resolution = new ScaledResolution(mc);
                setWorldAndResolution(mc, resolution.getScaledWidth(), resolution.getScaledHeight());
            }
        } else if (item instanceof IonSettingsMenu.Link) {
            pendingLink = ((IonSettingsMenu.Link) item).url;
            mc.displayGuiScreen(new GuiConfirmOpenLink(this, pendingLink, button.id, false));
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (result && pendingLink != null) {
            openLink(pendingLink);
        }
        pendingLink = null;
        mc.displayGuiScreen(this);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        try {
            IonSettings.save();
        } catch (IOException e) {
            IonClient.LOGGER.warn("Could not save the ION Client settings", e);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int bandTop = IonSettingsMenu.HEADER_HEIGHT;
        int bandBottom = height - IonSettingsMenu.FOOTER_HEIGHT;
        drawRect(0, bandTop, width, bandBottom, IonSettingsMenu.BAND_FILL);
        IonBrandText.drawGradientLine(0, bandTop - IonSettingsMenu.SEPARATOR_HEIGHT, width, bandTop, width);
        drawRect(0, bandBottom, width, bandBottom + 1, IonSettingsMenu.FOOTER_LINE_DARK);
        drawRect(0, bandBottom + 1, width, bandBottom + 2, IonSettingsMenu.FOOTER_LINE_LIGHT);

        int titleWidth = IonBrandText.width(fontRendererObj, IonSettingsMenu.TITLE_REST);
        IonBrandText.draw(fontRendererObj, IonSettingsMenu.TITLE_REST, (width - titleWidth) / 2,
                (IonSettingsMenu.HEADER_HEIGHT - fontRendererObj.FONT_HEIGHT) / 2, 0xFFFFFF);
        for (IonSettingsMenu.Heading heading : layout.headings) {
            fontRendererObj.drawStringWithShadow(heading.text, heading.x, heading.y, IonSettingsMenu.HEADING_COLOR);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        for (GuiButton button : buttonList) {
            if (button.id != DONE_ID && button.isMouseOver()) {
                drawHoveringText(fontRendererObj.listFormattedStringToWidth(items.get(button.id).tooltip(), IonSettingsMenu.TOOLTIP_WIDTH),
                        mouseX, mouseY);
            }
        }
    }

    /** Opens a web page in the system browser: xdg-open on Linux (AWT often cannot there), else AWT like vanilla. */
    private static void openLink(String url) {
        try {
            if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux")) {
                new ProcessBuilder("xdg-open", url).start();
                return;
            }
            Class<?> desktop = Class.forName("java.awt.Desktop");
            Object instance = desktop.getMethod("getDesktop").invoke(null);
            desktop.getMethod("browse", URI.class).invoke(instance, new URI(url));
        } catch (Throwable t) {
            IonClient.LOGGER.error("Could not open {}", url, t);
        }
    }
}
