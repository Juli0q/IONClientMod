package de.ionnetwork.client.net;

import de.ionnetwork.client.IonClientHello;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The {@link IonClientHello} as a 1.21.1 custom payload. Its bytes are produced by the shared encoder;
 * this only gives them a payload type, which {@code DiscardedPayloadMixin} makes the packet codec
 * accept. Never decoded on the client.
 */
public record IonHelloPayload(byte[] bytes) implements CustomPacketPayload {

    public static final Type<IonHelloPayload> TYPE = new Type<>(ResourceLocation.parse(IonClientHello.CHANNEL));

    public static final StreamCodec<FriendlyByteBuf, IonHelloPayload> STREAM_CODEC = CustomPacketPayload.codec(
            (payload, buf) -> buf.writeBytes(payload.bytes()),
            buf -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new IonHelloPayload(bytes);
            });

    /** The hello of this build: the 1.21.1 client has both capabilities natively, so it declares both. */
    public static IonHelloPayload of(String modVersion) {
        return new IonHelloPayload(IonClientHello.encode(modVersion,
                IonClientHello.CAPABILITY_GLOW_OUTLINES | IonClientHello.CAPABILITY_MODERN_SOUND_NAMES));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
