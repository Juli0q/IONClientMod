package de.ionnetwork.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import de.ionnetwork.client.IonClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * The mod's textures, registered straight from the jar.
 *
 * <p>Going through the resource manager would need Fabric API on Fabric (vanilla's resource
 * manager does not look inside mod jars there), so each texture is read from the classpath and
 * handed to the texture manager directly. That works identically on every loader.
 */
public final class IonIcon {

    public static final ResourceLocation COIN = ResourceLocation.fromNamespaceAndPath(IonClient.MOD_ID, "coin");

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
        String path = "/assets/ionclient/textures/gui/" + location.getPath() + ".png";
        try (InputStream stream = IonIcon.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("missing " + path);
            }
            NativeImage image = NativeImage.read(stream);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            REGISTERED.add(location);
            return true;
        } catch (IOException e) {
            IonClient.LOGGER.error("Could not load texture {}", path, e);
            FAILED.add(location);
            return false;
        }
    }
}
