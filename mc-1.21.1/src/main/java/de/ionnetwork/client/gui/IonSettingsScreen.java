package de.ionnetwork.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import de.ionnetwork.client.IonClient;
import de.ionnetwork.client.IonSettings;
import de.ionnetwork.client.IonSettingsMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ION Client's settings, opened from the "ION Client..." button in Options (or the loader's mod
 * list). Laid out by {@link IonSettingsMenu}; saved when the screen closes, like vanilla's options.
 */
public class IonSettingsScreen extends Screen {

    // AbstractSelectionList keeps these private; the band is drawn like one of its lists.
    private static final ResourceLocation LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png");
    private static final ResourceLocation INWORLD_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");

    private final Screen parent;
    private IonSettingsMenu.Layout layout;

    public IonSettingsScreen(Screen parent) {
        super(IonBrandText.of(IonSettingsMenu.TITLE_REST));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout = IonSettingsMenu.layout(IonSettingsMenu.sections(0), width, height);
        for (IonSettingsMenu.Slot slot : layout.slots) {
            if (slot.item instanceof IonSettingsMenu.Choice) {
                addRenderableWidget(cycleButton((IonSettingsMenu.Choice) slot.item, slot));
            } else if (slot.item instanceof IonSettingsMenu.Link) {
                IonSettingsMenu.Link link = (IonSettingsMenu.Link) slot.item;
                addRenderableWidget(Button.builder(Component.literal(link.label), ConfirmLinkScreen.confirmLink(this, link.url, true))
                        .tooltip(Tooltip.create(Component.literal(link.tooltip())))
                        .bounds(slot.x, slot.y, slot.width, IonSettingsMenu.BUTTON_HEIGHT)
                        .build());
            }
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - IonSettingsMenu.DONE_WIDTH / 2, IonSettingsMenu.doneY(height), IonSettingsMenu.DONE_WIDTH,
                        IonSettingsMenu.BUTTON_HEIGHT)
                .build());
    }

    private static CycleButton<Integer> cycleButton(IonSettingsMenu.Choice choice, IonSettingsMenu.Slot slot) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < choice.values().size(); i++) {
            indices.add(i);
        }
        Tooltip tooltip = Tooltip.create(Component.literal(choice.tooltip()));
        CycleButton<Integer> button = CycleButton.<Integer>builder(index -> Component.literal(choice.values().get(index)))
                .withValues(indices)
                .withInitialValue(choice.selected())
                .withTooltip(index -> tooltip)
                .create(slot.x, slot.y, slot.width, IonSettingsMenu.BUTTON_HEIGHT, Component.literal(choice.label),
                        (cycle, index) -> choice.select(index));
        button.active = choice.enabled();
        return button;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        try {
            IonSettings.save();
        } catch (IOException e) {
            IonClient.LOGGER.warn("Could not save the ION Client settings", e);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        boolean inWorld = minecraft.level != null;
        int bandTop = IonSettingsMenu.HEADER_HEIGHT;
        int bandBottom = height - IonSettingsMenu.FOOTER_HEIGHT;
        Screen.renderMenuBackgroundTexture(graphics, inWorld ? INWORLD_LIST_BACKGROUND : LIST_BACKGROUND, 0, bandTop, 0.0F, 0.0F,
                width, bandBottom - bandTop);
        RenderSystem.enableBlend();
        graphics.blit(inWorld ? Screen.INWORLD_FOOTER_SEPARATOR : Screen.FOOTER_SEPARATOR, 0, bandBottom, 0.0F, 0.0F, width, 2, 32, 2);
        RenderSystem.disableBlend();
        // In place of vanilla's grey header separator: the logo gradient, swept across the screen.
        for (int x = 0; x < width; x++) {
            graphics.fill(x, bandTop - IonSettingsMenu.SEPARATOR_HEIGHT, x + 1, bandTop, IonSettingsMenu.separatorColor(x, width));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, (IonSettingsMenu.HEADER_HEIGHT - font.lineHeight) / 2, 0xFFFFFF);
        for (IonSettingsMenu.Heading heading : layout.headings) {
            graphics.drawString(font, heading.text, heading.x, heading.y, IonSettingsMenu.HEADING_COLOR, true);
        }
    }
}
