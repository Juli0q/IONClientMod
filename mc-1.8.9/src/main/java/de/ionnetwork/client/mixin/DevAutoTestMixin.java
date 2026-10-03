package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ScreenShotHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

/**
 * Development self-test, inert unless {@code IONCLIENT_AUTOTEST=1} is in the environment (or
 * {@code -Dionclient.autotest=true} is passed): once the main menu is up it opens the
 * multiplayer screen, waits for the ping, saves {@code screenshots/ionclient-autotest.png} and
 * quits. Used to eyeball the pinned entry without a person at the keyboard.
 */
@Mixin(Minecraft.class)
public abstract class DevAutoTestMixin {

    @Shadow
    public GuiScreen currentScreen;

    @Shadow
    public int displayWidth;

    @Shadow
    public int displayHeight;

    @Shadow
    public File mcDataDir;

    @Shadow
    public abstract void displayGuiScreen(GuiScreen screen);

    @Shadow
    public abstract Framebuffer getFramebuffer();

    @Shadow
    public abstract void shutdown();

    @Unique
    private static final boolean ionclient$ENABLED = "1".equals(System.getenv("IONCLIENT_AUTOTEST")) || Boolean.getBoolean("ionclient.autotest");

    @Unique
    private int ionclient$ticksOnScreen;

    @Unique
    private boolean ionclient$opened;

    @Inject(method = "runTick", at = @At("TAIL"))
    private void ionclient$autoTest(CallbackInfo ci) {
        if (!ionclient$ENABLED) {
            return;
        }
        if (!ionclient$opened) {
            if (currentScreen instanceof GuiMainMenu) {
                ionclient$opened = true;
                IonClient.LOGGER.info("[autotest] opening the multiplayer screen");
                displayGuiScreen(new GuiMultiplayer(currentScreen));
            }
            return;
        }
        if (currentScreen instanceof GuiMultiplayer && ++ionclient$ticksOnScreen == 120) {
            IonClient.LOGGER.info("[autotest] saving screenshot and quitting");
            ScreenShotHelper.saveScreenshot(mcDataDir, "ionclient-autotest.png", displayWidth, displayHeight, getFramebuffer());
            shutdown();
        }
    }
}
