package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonSettingsMenu;
import de.ionnetwork.client.gui.IonBrandText;
import de.ionnetwork.client.gui.IonSettingsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Adds an "ION Client..." button to Options, as a full-width row under vanilla's grid of screen
 * buttons: the grid is wrapped in a column together with the button before it becomes the
 * screen's contents, so the layout and widget registration that follow pick both up.
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

    /** The grid's two 150px buttons and the 8px between them (each cell pads 4px on both sides). */
    @Unique
    private static final int ionclient$ROW_WIDTH = 2 * IonSettingsMenu.BUTTON_WIDTH + 8;

    protected OptionsScreenMixin() {
        super(null);
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToContents(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private LayoutElement ionclient$addSettingsButton(LayoutElement grid) {
        LinearLayout column = LinearLayout.vertical();
        column.defaultCellSetting().alignHorizontallyCenter();
        column.addChild(grid);
        column.addChild(Button.builder(IonBrandText.of(IonSettingsMenu.BUTTON_REST), button -> minecraft.setScreen(new IonSettingsScreen(this)))
                .width(ionclient$ROW_WIDTH)
                .build());
        return column;
    }
}
