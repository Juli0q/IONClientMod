package de.ionnetwork.client.mixin;

import de.ionnetwork.client.net.IonHelloPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the custom payload codec carry {@link IonHelloPayload}. Since 1.20.5 a payload id the codec
 * does not know falls back to {@code DiscardedPayload.codec(id, max)}, a codec that writes nothing;
 * without Fabric API or the loader's networking there is no registry to add a type to, and the list
 * the codec is built from differs per loader. The fallback is the one place every loader shares, so
 * for the hello's id it hands back the hello's codec instead.
 */
@Mixin(DiscardedPayload.class)
public abstract class DiscardedPayloadMixin {

    @Inject(method = "codec", at = @At("HEAD"), cancellable = true)
    private static <T extends FriendlyByteBuf> void ionclient$helloCodec(ResourceLocation id, int maxSize,
            CallbackInfoReturnable<StreamCodec<T, DiscardedPayload>> cir) {
        if (id.equals(IonHelloPayload.TYPE.id())) {
            @SuppressWarnings("unchecked")
            StreamCodec<T, DiscardedPayload> codec = (StreamCodec<T, DiscardedPayload>) (StreamCodec<?, ?>) IonHelloPayload.STREAM_CODEC;
            cir.setReturnValue(codec);
        }
    }
}
