package de.ionnetwork.client.textures;

import de.ionnetwork.client.IonClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Predicate;
import java.util.zip.ZipFile;

/**
 * Keeps {@code resourcepacks/ION Modern Textures.zip} built from the modern client jar, so the
 * player can turn it on in the vanilla Resource Packs screen.
 *
 * <p>Started at the very beginning of {@code Minecraft.startGame}; the work runs on a background
 * thread and is skipped when the pack on disk already carries this build's stamp. A pack that is
 * in use cannot be replaced on Windows, so a rebuilt one waits in the cache folder and is moved
 * into place on the next start, before Minecraft opens it. {@code -Dionclient.modernTextures=false}
 * turns all of this off. Whether the pack is selected is {@link ModernTexturesSelection}'s part.
 */
public final class ModernTextures {

    public static final String PACK_NAME = "ION Modern Textures.zip";

    private ModernTextures() {
    }

    /** Whether this run builds the pack (and so offers the setting for it). */
    public static boolean enabled() {
        return !"false".equalsIgnoreCase(System.getProperty("ionclient.modernTextures"));
    }

    /** The pack's file in the game directory's resource pack folder. */
    public static File packFile(File gameDir) {
        return new File(new File(gameDir, "resourcepacks"), PACK_NAME);
    }

    /**
     * @param onBuilt run on the worker thread after a newly built pack was moved into place
     */
    public static void start(File gameDir, Predicate<String> legacyExists, Runnable onBuilt) {
        if (!enabled()) {
            return;
        }
        File pack = packFile(gameDir);
        File pending = new File(ModernTextureSource.cacheDir(gameDir), "pending.zip");
        if (pending.isFile()) {
            try {
                install(pending, pack);
            } catch (IOException e) {
                IonClient.LOGGER.warn("Could not install the rebuilt modern texture pack", e);
            }
        }

        Thread worker = new Thread(() -> {
            try {
                if (update(gameDir, pack, pending, legacyExists)) {
                    onBuilt.run();
                }
            } catch (Throwable t) {
                IonClient.LOGGER.warn("Could not prepare the modern texture pack", t);
            }
        }, "ION modern textures");
        worker.setDaemon(true);
        worker.setPriority(Thread.MIN_PRIORITY);
        worker.start();
    }

    /** Returns whether a newly built pack is now in place. */
    private static boolean update(File gameDir, File pack, File pending, Predicate<String> legacyExists) throws IOException {
        String stamp = ModernTexturePack.stamp();
        if (stamp.equals(stampOf(pack))) {
            return false;
        }
        long started = System.currentTimeMillis();
        File jar = ModernTextureSource.resolve(gameDir);
        File building = new File(ModernTextureSource.cacheDir(gameDir), "building.zip");
        int textures;
        try (ZipFile modern = new ZipFile(jar); OutputStream out = new FileOutputStream(building)) {
            textures = ModernTexturePack.build(modern, legacyExists, out);
        }
        try {
            install(building, pack);
        } catch (IOException inUse) {
            Files.move(building.toPath(), pending.toPath(), StandardCopyOption.REPLACE_EXISTING);
            IonClient.LOGGER.info("The modern texture pack is in use; the rebuilt one is installed on the next start");
            return false;
        }
        IonClient.LOGGER.info("Built {} from Minecraft {} ({} textures, {} ms)", PACK_NAME, ModernTextureSource.VERSION,
                textures, System.currentTimeMillis() - started);
        return true;
    }

    private static void install(File built, File pack) throws IOException {
        File dir = pack.getParentFile();
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("cannot create " + dir);
        }
        Files.move(built.toPath(), pack.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /** The stamp {@link ModernTexturePack} wrote into the pack, or null when there is no readable pack. */
    private static String stampOf(File pack) {
        if (!pack.isFile()) {
            return null;
        }
        try (ZipFile zip = new ZipFile(pack)) {
            return zip.getComment();
        } catch (IOException e) {
            return null;
        }
    }
}
