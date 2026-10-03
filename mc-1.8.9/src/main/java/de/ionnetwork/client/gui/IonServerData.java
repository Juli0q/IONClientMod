package de.ionnetwork.client.gui;

import de.ionnetwork.client.IonBrand;
import net.minecraft.client.multiplayer.ServerData;

/**
 * The pinned ION Network server.
 *
 * <p>A subclass rather than a flag so every place that handles the list can tell it apart with a
 * plain {@code instanceof}: {@link net.minecraft.client.multiplayer.ServerList} refuses to save,
 * delete or move it, and {@link net.minecraft.client.gui.ServerSelectionList} renders it with
 * {@link IonServerEntry}. A fresh instance is created each time the list is loaded, so the ping
 * state resets exactly like vanilla's entries.
 */
public final class IonServerData extends ServerData {

    /**
     * The last favicon the server sent. Vanilla keeps icons in servers.dat, which the pinned
     * entry never reaches, so it is remembered here to avoid a placeholder on every screen open.
     */
    private static String lastIcon;

    public IonServerData() {
        super(IonBrand.SERVER_NAME, IonBrand.SERVER_ADDRESS, false);
        setResourceMode(ServerResourceMode.PROMPT);
        super.setBase64EncodedIconData(lastIcon);
    }

    @Override
    public void setBase64EncodedIconData(String icon) {
        super.setBase64EncodedIconData(icon);
        lastIcon = icon;
    }
}
