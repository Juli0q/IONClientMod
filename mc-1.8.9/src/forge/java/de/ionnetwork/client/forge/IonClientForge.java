package de.ionnetwork.client.forge;

import de.ionnetwork.client.IonClient;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = IonClient.MOD_ID, name = "ION Client", version = IonClient.VERSION, useMetadata = true, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]",
        guiFactory = "de.ionnetwork.client.forge.IonGuiFactory")
public class IonClientForge {

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        IonClient.init("Forge", Loader.instance().getConfigDir());
    }
}
