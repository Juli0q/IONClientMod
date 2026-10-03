package de.ionnetwork.client.fabric;

import de.ionnetwork.client.IonClient;
import net.fabricmc.api.ClientModInitializer;

public final class IonClientFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        IonClient.init("Fabric");
    }
}
