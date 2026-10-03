package de.ionnetwork.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import de.ionnetwork.client.IonClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;

/**
 * The ION server icon, registered straight from the jar.
 *
 * <p>Going through the resource manager would need Fabric API on Fabric (vanilla's resource
 * manager does not look inside mod jars there), so the texture is read from the classpath and
 * handed to the texture manager directly. That works identically on every loader.
 */
public final class IonIcon {

    public static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath(IonClient.MOD_ID, "server_icon");
    private static final String PATH = "/assets/ionclient/textures/gui/server_icon.png";
    private static boolean registered;

    private IonIcon() {
    }

    /** Registers the texture on first use. Returns false if the image could not be read. */
    public static boolean ensureRegistered() {
        if (registered) {
            return true;
        }
        try (InputStream stream = IonIcon.class.getResourceAsStream(PATH)) {
            if (stream == null) {
                throw new IOException("missing " + PATH);
            }
            NativeImage image = NativeImage.read(stream);
            Minecraft.getInstance().getTextureManager().register(LOCATION, new DynamicTexture(image));
            registered = true;
        } catch (IOException e) {
            IonClient.LOGGER.error("Could not load the ION server icon", e);
        }
        return registered;
    }
}
