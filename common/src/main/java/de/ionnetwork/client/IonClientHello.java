package de.ionnetwork.client;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * The hello the client sends the server right after joining: "this is ION Client, version X, and
 * here is what it can do beyond its Minecraft version".
 *
 * <p>Why the server needs it: ION's servers decide per player what to send (the 1.9 glowing flag,
 * modern sound names) from the client's protocol version, which says "1.8" for the 1.8.9 mod even
 * though the mod renders both. The hello lets them treat an ION Client 1.8.9 like a modern client
 * for exactly the features it has. The 1.21.1 mod sends it too, so the server knows an ION Client
 * is present whatever the version.
 *
 * <p>It is a plugin message on {@link #CHANNEL}, which is sent after every Join Game (so every
 * backend behind the proxy gets one, since the proxy only re-sends vanilla's brand and settings):
 * <pre>
 *   VarInt  format version ({@link #FORMAT_VERSION})
 *   String  mod version, as a VarInt-prefixed UTF-8 string ("1.1.0")
 *   VarInt  capability bits ({@link #CAPABILITY_GLOW_OUTLINES} | ...)
 * </pre>
 * The channel name is a valid 1.13 identifier and 15 characters, within 1.8's 20-character limit,
 * so it reaches the server unchanged through Velocity and ViaVersion whatever the client and server
 * versions. The server side of this contract is {@code IonClientContract} in IONPlugins'
 * {@code core/utilities}; the two are kept in step by hand.
 *
 * <p>Pure Java 8, no Minecraft imports: each version wraps the bytes in its own packet.
 */
public final class IonClientHello {

    public static final String CHANNEL = "ionclient:hello";
    public static final int FORMAT_VERSION = 1;

    /** Renders the 1.9 glowing flag (bit 6 of the entity flags) and the team colour of the outline. */
    public static final int CAPABILITY_GLOW_OUTLINES = 1;
    /** Plays sound events named the modern way ({@code entity.player.levelup}). */
    public static final int CAPABILITY_MODERN_SOUND_NAMES = 1 << 1;

    private IonClientHello() {
    }

    /** The payload for a client of {@code modVersion} with {@code capabilities}. */
    public static byte[] encode(String modVersion, int capabilities) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeVarInt(out, FORMAT_VERSION);
        byte[] version = modVersion.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, version.length);
        out.write(version, 0, version.length);
        writeVarInt(out, capabilities);
        return out.toByteArray();
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        while ((value & ~0x7F) != 0) {
            out.write(value & 0x7F | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }
}
