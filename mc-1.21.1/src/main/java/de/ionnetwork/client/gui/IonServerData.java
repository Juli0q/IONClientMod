package de.ionnetwork.client.gui;

import de.ionnetwork.client.IonBrand;
import net.minecraft.client.multiplayer.ServerData;

/**
 * The pinned ION Network server.
 *
 * <p>A subclass rather than a flag so every place that handles the list can tell it apart with a
 * plain {@code instanceof}: {@link net.minecraft.client.multiplayer.ServerList} refuses to save,
 * delete or move it, and the selection list renders it with {@link IonServerEntry}. A fresh
 * instance is created each time the list is loaded, so the ping state resets exactly like
 * vanilla's entries.
 */
public final class IonServerData extends ServerData {

    public IonServerData() {
        super(IonBrand.SERVER_NAME, IonBrand.SERVER_ADDRESS, ServerData.Type.OTHER);
        setResourcePackStatus(ServerData.ServerPackStatus.PROMPT);
    }
}
