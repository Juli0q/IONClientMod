package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonBrand;
import de.ionnetwork.client.IonSettings;
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
 * pinned entry itself are refused here. With pinning turned off in the settings the list is left
 * exactly as vanilla has it, including any ION entry the player saved themselves.
 */
@Mixin(ServerList.class)
public abstract class ServerListMixin {

    @Shadow
    @Final
    private List<ServerData> serverList;

    /** Null when this list was loaded with pinning turned off. */
    @Unique
    private IonServerData ionclient$pinned;

    @Inject(method = "load", at = @At("RETURN"))
    private void ionclient$pinAfterLoad(CallbackInfo ci) {
        ionclient$pinned = null;
        if (!IonSettings.pinServer()) {
            return;
        }
        // The pinned entry must be the only ION entry: drop the player's own copies of the
        // server (and any stray pinned entry), whichever way the address was written. The
        // next save writes the list back without them.
        serverList.removeIf(data -> data instanceof IonServerData || IonBrand.isIonAddress(data.ip));
        ionclient$pinned = new IonServerData();
        serverList.add(0, ionclient$pinned);
    }

    @Inject(method = "save", at = @At("HEAD"))
    private void ionclient$unpinBeforeSave(CallbackInfo ci) {
        if (ionclient$pinned == null) {
            return;
        }
        serverList.removeIf(data -> data instanceof IonServerData || IonBrand.isIonAddress(data.ip));
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void ionclient$repinAfterSave(CallbackInfo ci) {
        if (ionclient$pinned != null && !serverList.contains(ionclient$pinned)) {
            serverList.add(0, ionclient$pinned);
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void ionclient$refuseRemove(ServerData data, CallbackInfo ci) {
        if (data instanceof IonServerData) {
            ci.cancel();
        }
    }

    @Inject(method = "swap", at = @At("HEAD"), cancellable = true)
    private void ionclient$refuseSwap(int first, int second, CallbackInfo ci) {
        if (ionclient$isPinned(first) || ionclient$isPinned(second)) {
            ci.cancel();
        }
    }

    @Inject(method = "replace", at = @At("HEAD"), cancellable = true)
    private void ionclient$refuseReplace(int index, ServerData replacement, CallbackInfo ci) {
        if (ionclient$isPinned(index)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean ionclient$isPinned(int index) {
        return index >= 0 && index < serverList.size() && serverList.get(index) instanceof IonServerData;
    }
}
