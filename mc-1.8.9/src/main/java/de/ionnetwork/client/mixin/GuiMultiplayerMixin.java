package de.ionnetwork.client.mixin;

import de.ionnetwork.client.gui.IonServerEntry;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.gui.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The pinned entry can be joined, but not edited, deleted or moved. */
@Mixin(GuiMultiplayer.class)
public abstract class GuiMultiplayerMixin {

    @Shadow
    private ServerSelectionList serverListSelector;

    @Shadow
    private GuiButton btnEditServer;

    @Shadow
    private GuiButton btnDeleteServer;

    @Inject(method = "selectServer", at = @At("RETURN"))
    private void ionclient$lockButtons(int index, CallbackInfo ci) {
        GuiListExtended.IGuiListEntry entry = index < 0 ? null : serverListSelector.getListEntry(index);
        if (entry instanceof IonServerEntry) {
            btnEditServer.enabled = false;
            btnDeleteServer.enabled = false;
        }
    }

    /** canMoveUp: the entry right below the pinned one must not be moved above it either. */
    @Inject(method = "func_175392_a", at = @At("HEAD"), cancellable = true)
    private void ionclient$canMoveUp(ServerListEntryNormal entry, int index, CallbackInfoReturnable<Boolean> cir) {
        if (entry instanceof IonServerEntry || index <= 1) {
            cir.setReturnValue(false);
        }
    }

    /** canMoveDown. */
    @Inject(method = "func_175394_b", at = @At("HEAD"), cancellable = true)
    private void ionclient$canMoveDown(ServerListEntryNormal entry, int index, CallbackInfoReturnable<Boolean> cir) {
        if (entry instanceof IonServerEntry) {
            cir.setReturnValue(false);
        }
    }
}
