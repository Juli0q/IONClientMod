package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonSettings;
import de.ionnetwork.client.gui.IonDisplayScale;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps the chosen GUI scale the same physical size on a scaled desktop. When the game renders at full
 * framebuffer resolution (LWJGL3 on a 1.45x Wayland output), "Large" (3) becomes 4. Auto (0) is left
 * alone: it already picks the largest scale that fits. The fit check after this read still caps the result.
 * The "Desktop Scale" setting turns it off.
 */
@Mixin(ScaledResolution.class)
public abstract class ScaledResolutionMixin {

    @Redirect(method = "<init>",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;guiScale:I"))
    private int ionclient$scaleGuiScale(GameSettings settings) {
        int guiScale = settings.guiScale;
        if (guiScale == 0 || !IonSettings.matchDesktopScale()) return guiScale;
        return Math.max(guiScale, (int) Math.round(guiScale * IonDisplayScale.get()));
    }
}
