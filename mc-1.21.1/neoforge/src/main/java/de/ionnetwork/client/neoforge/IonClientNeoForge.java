package de.ionnetwork.client.neoforge;

import de.ionnetwork.client.IonClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = IonClient.MOD_ID, dist = Dist.CLIENT)
public final class IonClientNeoForge {

    public IonClientNeoForge() {
        IonClient.init("NeoForge");
    }
}
