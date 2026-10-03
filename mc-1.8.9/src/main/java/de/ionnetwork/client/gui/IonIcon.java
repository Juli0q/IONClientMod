package de.ionnetwork.client.gui;

import de.ionnetwork.client.IonClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * The ION server icon, registered straight from the jar so it does not depend on how the
 * loader exposes mod resources.
 */
public final class IonIcon {

    public static final ResourceLocation LOCATION = new ResourceLocation(IonClient.MOD_ID, "server_icon");
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
            BufferedImage image = ImageIO.read(stream);
            Minecraft.getMinecraft().getTextureManager().loadTexture(LOCATION, new DynamicTexture(image));
            registered = true;
        } catch (IOException e) {
            IonClient.LOGGER.error("Could not load the ION server icon", e);
        }
        return registered;
    }
}
