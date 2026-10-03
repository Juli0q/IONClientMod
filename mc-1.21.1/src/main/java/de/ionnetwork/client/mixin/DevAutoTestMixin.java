package de.ionnetwork.client.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import de.ionnetwork.client.IonClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Development self-test, inert unless {@code IONCLIENT_AUTOTEST=1} is in the environment (or
 * {@code -Dionclient.autotest=true} is passed): once the title screen is up it opens the
 * multiplayer screen, waits for the ping, saves {@code screenshots/ionclient-autotest.png} and
 * quits. Used to eyeball the pinned entry without a person at the keyboard.
 *
 * <p>Panorama capture, inert unless {@code IONCLIENT_PANORAMA=1} is set or a file named
 * {@code ionclient-panorama} exists in the game directory (handy under a launcher): pressing F6
 * in-world turns the camera through the six title-screen cubemap faces, starting from the current
 * view direction, and saves them as {@code panoramas/<time>/panorama_0..5.png}. Works with Iris
 * shaders, so the menu background can show the lobby shaded.
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

    @Shadow
    public LocalPlayer player;

    @Shadow
    public abstract Window getWindow();

    @Shadow
    @Final
    public Options options;

    @Unique
    private static final boolean ionclient$ENABLED = "1".equals(System.getenv("IONCLIENT_AUTOTEST")) || Boolean.getBoolean("ionclient.autotest");

    @Unique
    private int ionclient$ticksOnScreen;

    @Unique
    private int ionclient$ticksOnTitle;

    @Unique
    private boolean ionclient$opened;

    @Unique
    private static final int ionclient$PANORAMA_SIZE = 1024;

    /** Ticks to hold each face before grabbing it, so shader TAA and exposure settle. */
    @Unique
    private static final int ionclient$PANORAMA_SETTLE_TICKS = 20;

    @Unique
    private Boolean ionclient$panorama;

    @Unique
    private boolean ionclient$panoramaKeyDown;

    /** Face being captured, or -1 while idle. */
    @Unique
    private int ionclient$face = -1;

    @Unique
    private int ionclient$faceTicks;

    @Unique
    private float ionclient$baseYaw;

    @Unique
    private File ionclient$panoramaDir;

    @Unique
    private boolean ionclient$savedHideGui;

    @Unique
    private int ionclient$savedFov;

    @Unique
    private double ionclient$savedFovEffect;

    @Unique
    private boolean ionclient$savedBob;

    @Inject(method = "tick", at = @At("TAIL"))
    private void ionclient$capturePanorama(CallbackInfo ci) {
        if (ionclient$panorama == null) {
            ionclient$panorama = "1".equals(System.getenv("IONCLIENT_PANORAMA")) || new File(gameDirectory, "ionclient-panorama").exists();
        }
        if (!ionclient$panorama) {
            return;
        }
        if (ionclient$face >= 0) {
            ionclient$stepPanorama();
            return;
        }
        boolean down = InputConstants.isKeyDown(getWindow().getWindow(), GLFW.GLFW_KEY_F6);
        boolean pressed = down && !ionclient$panoramaKeyDown;
        ionclient$panoramaKeyDown = down;
        if (pressed && player != null && screen == null) {
            ionclient$startPanorama();
        }
    }

    /**
     * Captures from the normal frame rather than vanilla's offscreen panorama pass, which Sodium
     * and Iris shaders leave empty: the camera is turned through the six cube faces with a 90°
     * FOV and the centered square of each frame becomes one face.
     */
    @Unique
    private void ionclient$startPanorama() {
        ionclient$panoramaDir = new File(gameDirectory, "panoramas/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
        ionclient$panoramaDir.mkdirs();
        ionclient$savedHideGui = options.hideGui;
        ionclient$savedFov = options.fov().get();
        ionclient$savedFovEffect = options.fovEffectScale().get();
        ionclient$savedBob = options.bobView().get();
        options.hideGui = true;
        options.fov().set(90);
        options.fovEffectScale().set(0.0);
        options.bobView().set(false);
        ionclient$baseYaw = player.getYRot();
        ionclient$face = 0;
        ionclient$faceTicks = 0;
        IonClient.LOGGER.info("[panorama] capturing into {}", ionclient$panoramaDir);
        ionclient$aimAtFace();
    }

    @Unique
    private void ionclient$stepPanorama() {
        if (player == null) {
            ionclient$finishPanorama("Panorama aborted");
            return;
        }
        // Re-aim every tick so a stray mouse nudge cannot skew a face.
        ionclient$aimAtFace();
        if (++ionclient$faceTicks < ionclient$PANORAMA_SETTLE_TICKS) {
            return;
        }
        RenderTarget frame = getMainRenderTarget();
        try (NativeImage full = Screenshot.takeScreenshot(frame);
             NativeImage face = new NativeImage(ionclient$PANORAMA_SIZE, ionclient$PANORAMA_SIZE, false)) {
            // FOV is vertical, so the centered height-sized square spans exactly 90° each way.
            int side = Math.min(full.getWidth(), full.getHeight());
            full.resizeSubRectTo((full.getWidth() - side) / 2, (full.getHeight() - side) / 2, side, side, face);
            face.writeToFile(new File(ionclient$panoramaDir, "panorama_" + ionclient$face + ".png"));
        } catch (Exception e) {
            IonClient.LOGGER.error("[panorama] failed to save face {}", ionclient$face, e);
            ionclient$finishPanorama("Panorama failed, see the log");
            return;
        }
        ionclient$faceTicks = 0;
        if (++ionclient$face == 6) {
            ionclient$finishPanorama("Panorama saved to panoramas/" + ionclient$panoramaDir.getName());
        } else {
            ionclient$aimAtFace();
        }
    }

    /** Same face order as vanilla: front, right, back, left, up, down. */
    @Unique
    private void ionclient$aimAtFace() {
        float yaw = ionclient$baseYaw + switch (ionclient$face) {
            case 1 -> 90.0F;
            case 2 -> 180.0F;
            case 3 -> -90.0F;
            default -> 0.0F;
        };
        float pitch = ionclient$face == 4 ? -90.0F : ionclient$face == 5 ? 90.0F : 0.0F;
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
    }

    @Unique
    private void ionclient$finishPanorama(String message) {
        ionclient$face = -1;
        options.hideGui = ionclient$savedHideGui;
        options.fov().set(ionclient$savedFov);
        options.fovEffectScale().set(ionclient$savedFovEffect);
        options.bobView().set(ionclient$savedBob);
        IonClient.LOGGER.info("[panorama] {}", message);
        if (player != null) {
            player.setYRot(ionclient$baseYaw);
            player.setXRot(0.0F);
            player.displayClientMessage(Component.literal(message), false);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void ionclient$autoTest(CallbackInfo ci) {
        if (!ionclient$ENABLED) {
            return;
        }
        if (!ionclient$opened) {
            if (screen instanceof TitleScreen && ++ionclient$ticksOnTitle == 80) {
                IonClient.LOGGER.info("[autotest] saving title screenshot, opening the multiplayer screen");
                Screenshot.grab(gameDirectory, "ionclient-autotest-title.png", getMainRenderTarget(), message -> IonClient.LOGGER.info("[autotest] {}", message.getString()));
                ionclient$opened = true;
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
