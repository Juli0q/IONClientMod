package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonClient;
import de.ionnetwork.client.IonClientHello;
import io.netty.buffer.Unpooled;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S01PacketJoinGame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the server this is ION Client once per join, right after vanilla sends its brand (see
 * {@link IonClientHello}). Join Game arrives again on every server switch behind the proxy, so every
 * backend hears it. The 1.8.9 mod announces the two things it renders that vanilla 1.8 does not.
 */
@Mixin(NetHandlerPlayClient.class)
public abstract class NetHandlerPlayClientMixin {

    @Shadow
    public abstract NetworkManager getNetworkManager();

    @Inject(method = "handleJoinGame", at = @At("TAIL"))
    private void ionclient$sendHello(S01PacketJoinGame packet, CallbackInfo ci) {
        byte[] payload = IonClientHello.encode(IonClient.VERSION,
                IonClientHello.CAPABILITY_GLOW_OUTLINES | IonClientHello.CAPABILITY_MODERN_SOUND_NAMES);
        getNetworkManager().sendPacket(new C17PacketCustomPayload(IonClientHello.CHANNEL,
                new PacketBuffer(Unpooled.wrappedBuffer(payload))));
    }
}
