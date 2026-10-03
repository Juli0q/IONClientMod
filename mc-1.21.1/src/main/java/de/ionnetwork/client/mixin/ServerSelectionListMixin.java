package de.ionnetwork.client.mixin;

import de.ionnetwork.client.gui.IonServerData;
import de.ionnetwork.client.gui.IonServerEntry;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
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
    private JoinMultiplayerScreen screen;

    @Shadow
    @Final
    private List<ServerSelectionList.OnlineServerEntry> onlineServers;

    @Inject(
            method = "updateOnlineServers",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/multiplayer/ServerSelectionList;refreshEntries()V")
    )
    private void ionclient$swapInIonEntry(ServerList servers, CallbackInfo ci) {
        for (int i = 0; i < onlineServers.size(); i++) {
            ServerSelectionList.OnlineServerEntry entry = onlineServers.get(i);
            if (entry.getServerData() instanceof IonServerData && !(entry instanceof IonServerEntry)) {
                onlineServers.set(i, new IonServerEntry((ServerSelectionList) (Object) this, screen, entry.getServerData()));
            }
        }
    }
}
