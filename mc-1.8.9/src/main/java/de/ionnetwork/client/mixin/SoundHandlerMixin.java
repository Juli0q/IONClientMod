package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonClient;
import de.ionnetwork.client.sound.LegacySoundNames;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundRegistry;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Plays a sound the server names the modern way ({@code entity.player.levelup}) as the 1.8.9 sound it
 * is ({@code random.levelup}). Only names the registry does not know are looked up, so a resource
 * pack that defines the modern name itself still wins.
 */
@Mixin(SoundHandler.class)
public abstract class SoundHandlerMixin {

    @Shadow
    @Final
    private SoundRegistry sndRegistry;

    @Inject(method = "getSound", at = @At("RETURN"), cancellable = true)
    private void ionclient$playModernName(ResourceLocation location, CallbackInfoReturnable<SoundEventAccessorComposite> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }
        ResourceLocation legacy = LegacySoundNames.legacy(location);
        if (legacy == null) {
            return;
        }
        SoundEventAccessorComposite sound = sndRegistry.getObject(legacy);
        if (sound != null) {
            IonClient.LOGGER.debug("Playing {} as {}", location, legacy);
            cir.setReturnValue(sound);
        }
    }
}
