package de.ionnetwork.client.neoforge;

import de.ionnetwork.client.IonClient;
import de.ionnetwork.client.gui.IonSettingsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = IonClient.MOD_ID, dist = Dist.CLIENT)
public final class IonClientNeoForge {

    public IonClientNeoForge(ModContainer container) {
        IonClient.init("NeoForge", FMLPaths.CONFIGDIR.get().toFile());
        // The mod list's "Config" button.
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new IonSettingsScreen(parent));
    }
}
