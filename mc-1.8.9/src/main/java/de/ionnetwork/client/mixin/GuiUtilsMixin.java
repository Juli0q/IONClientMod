package de.ionnetwork.client.mixin;

import de.ionnetwork.client.gui.IonTooltip;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Keeps every tooltip on screen. Forge routes all of them (items, chat hovers, menus) through
 * GuiUtils.drawHoveringText, which only pushes a tooltip up from the bottom edge, so tall item lore ran off
 * the top and long lines off the side. Named by string: Forge is not on this source set's classpath, and
 * the class keeps its names in production, hence no remapping.
 */
@Mixin(targets = "net.minecraftforge.fml.client.config.GuiUtils", remap = false)
public abstract class GuiUtilsMixin {

    @Inject(method = "drawHoveringText(Ljava/util/List;IIIIILnet/minecraft/client/gui/FontRenderer;)V",
            at = @At("HEAD"), cancellable = true)
    private static void ionclient$drawOnScreen(List<String> textLines, int mouseX, int mouseY, int screenWidth,
                                               int screenHeight, int maxTextWidth, FontRenderer font, CallbackInfo ci) {
        IonTooltip.draw(textLines, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth, font);
        ci.cancel();
    }
}
