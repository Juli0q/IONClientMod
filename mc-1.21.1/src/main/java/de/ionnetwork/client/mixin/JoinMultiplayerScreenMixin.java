package de.ionnetwork.client.mixin;

import de.ionnetwork.client.gui.IonServerEntry;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The pinned entry can be joined, but not edited or deleted. */
@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin {

    @Shadow
    protected ServerSelectionList serverSelectionList;

    @Shadow
    private Button editButton;

    @Shadow
    private Button deleteButton;

    @Inject(method = "onSelectedChange", at = @At("RETURN"))
    private void ionclient$lockButtons(CallbackInfo ci) {
        if (serverSelectionList.getSelected() instanceof IonServerEntry) {
            editButton.active = false;
            deleteButton.active = false;
        }
    }
}
