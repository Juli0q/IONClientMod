package de.ionnetwork.client.gui;

import de.ionnetwork.client.IonClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * The mod's textures, registered straight from the jar so they do not depend on how the loader
 * exposes mod resources.
 */
public final class IonIcon {

    public static final ResourceLocation COIN = new ResourceLocation(IonClient.MOD_ID, "coin");

    private static final Set<ResourceLocation> REGISTERED = new HashSet<>();
    private static final Set<ResourceLocation> FAILED = new HashSet<>();

    private IonIcon() {
    }

    /** Registers the texture on first use. Returns false if the image could not be read. */
    public static boolean ensureRegistered(ResourceLocation location) {
        if (REGISTERED.contains(location)) {
            return true;
        }
        if (FAILED.contains(location)) {
            return false;
        }
        String path = "/assets/ionclient/textures/gui/" + location.getResourcePath() + ".png";
        try (InputStream stream = IonIcon.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("missing " + path);
            }
            BufferedImage image = ImageIO.read(stream);
            Minecraft.getMinecraft().getTextureManager().loadTexture(location, new DynamicTexture(image));
            REGISTERED.add(location);
            return true;
        } catch (IOException e) {
            IonClient.LOGGER.error("Could not load texture {}", path, e);
            FAILED.add(location);
            return false;
        }
    }
}
