package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
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
 *
 * <p>{@code IONCLIENT_AUTOTEST=glow} instead opens a flat creative world, summons a few entities
 * (one behind a wall, one dropped item), puts the player on a red team, raises the 1.9 glowing flag
 * on the server-side entities so it arrives through the metadata packet, plays a modern-named sound,
 * and saves {@code ionclient-autotest-glow.png} (third person) and
 * {@code ionclient-autotest-glow-firstperson.png}.
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

    @Shadow
    public EntityPlayerSP thePlayer;

    @Shadow
    public GameSettings gameSettings;

    @Shadow
    public abstract void launchIntegratedServer(String folderName, String worldName, WorldSettings settings);

    @Shadow
    public abstract IntegratedServer getIntegratedServer();

    @Unique
    private static final boolean ionclient$ENABLED = "1".equals(System.getenv("IONCLIENT_AUTOTEST")) || Boolean.getBoolean("ionclient.autotest");

    @Unique
    private static final boolean ionclient$GLOW_TEST = "glow".equals(System.getenv("IONCLIENT_AUTOTEST"));

    @Unique
    private int ionclient$glowStep;

    @Unique
    private int ionclient$glowTicks;

    @Unique
    private int ionclient$ticksOnScreen;

    @Unique
    private int ionclient$ticksOnTitle;

    @Unique
    private boolean ionclient$opened;

    @Inject(method = "runTick", at = @At("TAIL"))
    private void ionclient$autoTest(CallbackInfo ci) {
        if (ionclient$GLOW_TEST) {
            ionclient$glowTest();
            return;
        }
        if (!ionclient$ENABLED) {
            return;
        }
        if (!ionclient$opened) {
            if (currentScreen instanceof GuiMainMenu && ++ionclient$ticksOnTitle == 80) {
                IonClient.LOGGER.info("[autotest] saving title screenshot, opening the multiplayer screen");
                ScreenShotHelper.saveScreenshot(mcDataDir, "ionclient-autotest-title.png", displayWidth, displayHeight, getFramebuffer());
                ionclient$opened = true;
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

    @Unique
    private void ionclient$glowTest() {
        ionclient$glowTicks++;
        switch (ionclient$glowStep) {
            case 0:
                if (currentScreen instanceof GuiMainMenu && ionclient$glowTicks >= 80) {
                    IonClient.LOGGER.info("[autotest] opening a flat world for the glow test");
                    launchIntegratedServer("ionclient-autotest", "ionclient-autotest",
                            new WorldSettings(0L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands());
                    ionclient$glowStep(1);
                }
                break;
            case 1:
                if (thePlayer != null && currentScreen == null && ionclient$glowTicks >= 60) {
                    IonClient.LOGGER.info("[autotest] in the world, summoning entities");
                    thePlayer.rotationYaw = 0.0F;
                    thePlayer.rotationPitch = 8.0F;
                    gameSettings.thirdPersonView = 1;
                    for (String command : new String[]{
                            "/time set 6000", "/gamerule doDaylightCycle false", "/gamerule doMobSpawning false",
                            "/fill ~-1 ~ ~5 ~1 ~2 ~5 stone",
                            "/summon Zombie ~-2 ~ ~3 {NoAI:1b}",
                            "/summon Villager ~2 ~ ~3 {NoAI:1b}",
                            "/summon Zombie ~ ~ ~8 {NoAI:1b}",
                            "/summon Item ~ ~1 ~2 {Item:{id:\"minecraft:diamond_sword\",Count:1b},PickupDelay:32767s}",
                            "/scoreboard teams add red", "/scoreboard teams option red color red", "/scoreboard teams join red @p",
                            "/scoreboard teams add blue", "/scoreboard teams option blue color blue",
                            "/scoreboard teams join blue @e[type=Villager]"}) {
                        thePlayer.sendChatMessage(command);
                    }
                    ionclient$glowStep(2);
                }
                break;
            case 2:
                if (ionclient$glowTicks >= 40) {
                    IonClient.LOGGER.info("[autotest] raising the glowing flag on the server's entities");
                    IntegratedServer server = getIntegratedServer();
                    for (WorldServer world : server.worldServers) {
                        for (Entity entity : world.loadedEntityList) {
                            byte flags = entity.getDataWatcher().getWatchableObjectByte(0);
                            entity.getDataWatcher().updateObject(0, (byte) (flags | 1 << 6));
                        }
                    }
                    thePlayer.sendChatMessage("/playsound entity.player.levelup @p");
                    ionclient$glowStep(3);
                }
                break;
            case 3:
                if (ionclient$glowTicks >= 40) {
                    thePlayer.rotationYaw = 0.0F;
                    thePlayer.rotationPitch = 8.0F;
                    ionclient$shoot("ionclient-autotest-glow.png");
                    gameSettings.thirdPersonView = 0;
                    ionclient$glowStep(4);
                }
                break;
            case 4:
                if (ionclient$glowTicks >= 10) {
                    ionclient$shoot("ionclient-autotest-glow-firstperson.png");
                    IonClient.LOGGER.info("[autotest] done, quitting");
                    shutdown();
                }
                break;
            default:
                break;
        }
    }

    @Unique
    private void ionclient$glowStep(int step) {
        ionclient$glowStep = step;
        ionclient$glowTicks = 0;
    }

    @Unique
    private void ionclient$shoot(String name) {
        IonClient.LOGGER.info("[autotest] saving {}", name);
        ScreenShotHelper.saveScreenshot(mcDataDir, name, displayWidth, displayHeight, getFramebuffer());
    }
}
