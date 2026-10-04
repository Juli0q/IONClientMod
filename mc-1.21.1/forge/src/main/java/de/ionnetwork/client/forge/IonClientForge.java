package de.ionnetwork.client.forge;

import de.ionnetwork.client.IonClient;
import de.ionnetwork.client.gui.IonSettingsScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(IonClient.MOD_ID)
public final class IonClientForge {

    public IonClientForge(FMLJavaModLoadingContext context) {
        // Client-only: a vanilla or differently-modded server must not be marked incompatible.
        context.registerDisplayTest(IExtensionPoint.DisplayTest.IGNORE_ALL_VERSION);
        // The mod list's "Config" button.
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(IonSettingsScreen::new));
        IonClient.init("Forge", FMLPaths.CONFIGDIR.get().toFile());
    }
}
