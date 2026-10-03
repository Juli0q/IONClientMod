package de.ionnetwork.client.forge;

import de.ionnetwork.client.IonClient;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(IonClient.MOD_ID)
public final class IonClientForge {

    public IonClientForge(FMLJavaModLoadingContext context) {
        // Client-only: a vanilla or differently-modded server must not be marked incompatible.
        context.registerDisplayTest(IExtensionPoint.DisplayTest.IGNORE_ALL_VERSION);
        IonClient.init("Forge");
    }
}
