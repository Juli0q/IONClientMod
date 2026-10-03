package de.ionnetwork.client.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import de.ionnetwork.client.IonClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

/**
 * Development self-test, inert unless {@code IONCLIENT_AUTOTEST=1} is in the environment (or
 * {@code -Dionclient.autotest=true} is passed): once the title screen is up it opens the
 * multiplayer screen, waits for the ping, saves {@code screenshots/ionclient-autotest.png} and
 * quits. Used to eyeball the pinned entry without a person at the keyboard.
 */
@Mixin(Minecraft.class)
public abstract class DevAutoTestMixin {

    @Shadow
    public Screen screen;

    @Shadow
    public abstract void setScreen(Screen screen);

    @Shadow
    public abstract RenderTarget getMainRenderTarget();

    @Shadow
    public abstract void stop();

    @Shadow
    public File gameDirectory;

    @Unique
    private static final boolean ionclient$ENABLED = "1".equals(System.getenv("IONCLIENT_AUTOTEST")) || Boolean.getBoolean("ionclient.autotest");

    @Unique
    private int ionclient$ticksOnScreen;

    @Unique
    private boolean ionclient$opened;

    @Inject(method = "tick", at = @At("TAIL"))
    private void ionclient$autoTest(CallbackInfo ci) {
        if (!ionclient$ENABLED) {
            return;
        }
        if (!ionclient$opened) {
            if (screen instanceof TitleScreen) {
                ionclient$opened = true;
                IonClient.LOGGER.info("[autotest] opening the multiplayer screen");
                setScreen(new JoinMultiplayerScreen(screen));
            }
            return;
        }
        if (screen instanceof JoinMultiplayerScreen && ++ionclient$ticksOnScreen == 120) {
            IonClient.LOGGER.info("[autotest] saving screenshot and quitting");
            Screenshot.grab(gameDirectory, "ionclient-autotest.png", getMainRenderTarget(), message -> IonClient.LOGGER.info("[autotest] {}", message.getString()));
            stop();
        }
    }
}
