package de.ionnetwork.client.mixin;

import de.ionnetwork.client.gui.IonServerData;
import de.ionnetwork.client.gui.IonServerEntry;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.gui.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Renders the pinned server with {@link IonServerEntry} instead of the vanilla row. */
@Mixin(ServerSelectionList.class)
public abstract class ServerSelectionListMixin {

    @Shadow
    @Final
    private GuiMultiplayer owner;

    @Shadow
    @Final
    private List<ServerListEntryNormal> serverListInternet;

    @Inject(method = "func_148195_a", at = @At("RETURN"))
    private void ionclient$swapInIonEntry(ServerList serverList, CallbackInfo ci) {
        for (int i = 0; i < serverListInternet.size(); i++) {
            ServerListEntryNormal entry = serverListInternet.get(i);
            if (entry.getServerData() instanceof IonServerData && !(entry instanceof IonServerEntry)) {
                serverListInternet.set(i, new IonServerEntry(owner, entry.getServerData()));
            }
        }
    }
}
