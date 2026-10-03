package de.ionnetwork.client.mixin;

import de.ionnetwork.client.gui.IonServerData;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Keeps the ION server at the top of the in-memory list without ever writing it to servers.dat.
 *
 * <p>The pinned entry lives at index 0 of the same list the screen indexes into, so delete, edit
 * and reorder keep pointing at the right saved server; the operations that would touch the
 * pinned entry itself are refused here.
 */
@Mixin(ServerList.class)
public abstract class ServerListMixin {

    @Shadow
    @Final
    private List<ServerData> servers;

    @Unique
    private IonServerData ionclient$pinned;

    @Inject(method = "loadServerList", at = @At("RETURN"))
    private void ionclient$pinAfterLoad(CallbackInfo ci) {
        servers.removeIf(data -> data instanceof IonServerData);
        ionclient$pinned = new IonServerData();
        servers.add(0, ionclient$pinned);
    }

    @Inject(method = "saveServerList", at = @At("HEAD"))
    private void ionclient$unpinBeforeSave(CallbackInfo ci) {
        servers.removeIf(data -> data instanceof IonServerData);
    }

    @Inject(method = "saveServerList", at = @At("RETURN"))
    private void ionclient$repinAfterSave(CallbackInfo ci) {
        if (ionclient$pinned != null && !servers.contains(ionclient$pinned)) {
            servers.add(0, ionclient$pinned);
        }
    }

    @Inject(method = "removeServerData", at = @At("HEAD"), cancellable = true)
    private void ionclient$refuseRemove(int index, CallbackInfo ci) {
        if (ionclient$isPinned(index)) {
            ci.cancel();
        }
    }

    @Inject(method = "swapServers", at = @At("HEAD"), cancellable = true)
    private void ionclient$refuseSwap(int first, int second, CallbackInfo ci) {
        if (ionclient$isPinned(first) || ionclient$isPinned(second)) {
            ci.cancel();
        }
    }

    @Inject(method = "func_147413_a", at = @At("HEAD"), cancellable = true)
    private void ionclient$refuseReplace(int index, ServerData replacement, CallbackInfo ci) {
        if (ionclient$isPinned(index)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean ionclient$isPinned(int index) {
        return index >= 0 && index < servers.size() && servers.get(index) instanceof IonServerData;
    }
}
