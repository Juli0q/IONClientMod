package de.ionnetwork.client.mixin;

import de.ionnetwork.client.IonClient;
import de.ionnetwork.client.IonClientHello;
import de.ionnetwork.client.net.IonHelloPayload;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the server this is ION Client once per join, right after vanilla sends its brand (see
 * {@link IonClientHello}). The login packet arrives again on every server switch behind the proxy,
 * so every backend hears it.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Shadow
    public abstract void send(Packet<?> packet);

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void ionclient$sendHello(ClientboundLoginPacket packet, CallbackInfo ci) {
        send(new ServerboundCustomPayloadPacket(IonHelloPayload.of(IonClient.VERSION)));
    }
}
