package de.ionnetwork.client.textures;

import de.ionnetwork.client.IonClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The modern client jar the textures are taken from: Mojang's own download of {@link #VERSION},
 * pinned by hash so the mapping table always matches the files it reads.
 *
 * <p>The jar is looked for in this order, and only downloaded when none of them has it:
 * <ol>
 *   <li>{@code -Dionclient.modernTextures.jar=<path>};</li>
 *   <li>{@code <gameDir>/ionclient/modern-textures/client-<version>.jar}, the cache, which is also
 *       where a launcher can put it before the game starts (see the README);</li>
 *   <li>{@code <gameDir>/versions/<version>/<version>.jar}, a vanilla launcher's copy.</li>
 * </ol>
 * Every candidate is checked against the pinned size and SHA-1; a corrupt cached copy is deleted.
 */
public final class ModernTextureSource {

    public static final String VERSION = "26.3";
    public static final String URL = "https://piston-data.mojang.com/v1/objects/e877b6a07acd633fb3bb475002175cec036e7b87/client.jar";
    public static final String SHA1 = "e877b6a07acd633fb3bb475002175cec036e7b87";
    public static final long SIZE = 41483720L;

    private static final int CONNECT_TIMEOUT_MILLIS = 10000;
    private static final int READ_TIMEOUT_MILLIS = 30000;

    private ModernTextureSource() {
    }

    /** {@code <gameDir>/ionclient/modern-textures}: the jar cache and the converter's scratch space. */
    public static File cacheDir(File gameDir) {
        return new File(new File(gameDir, "ionclient"), "modern-textures");
    }

    public static File cachedJar(File gameDir) {
        return new File(cacheDir(gameDir), "client-" + VERSION + ".jar");
    }

    /** Returns a verified copy of the jar, downloading it into the cache when there is none. */
    static File resolve(File gameDir) throws IOException {
        String override = System.getProperty("ionclient.modernTextures.jar");
        if (override != null && isValid(new File(override))) {
            return new File(override);
        }
        File cached = cachedJar(gameDir);
        if (isValid(cached)) {
            return cached;
        }
        if (cached.exists() && !cached.delete()) {
            throw new IOException("cannot delete the corrupt " + cached);
        }
        File vanilla = new File(new File(new File(gameDir, "versions"), VERSION), VERSION + ".jar");
        if (isValid(vanilla)) {
            return vanilla;
        }
        download(cached);
        return cached;
    }

    static boolean isValid(File jar) throws IOException {
        if (!jar.isFile() || jar.length() != SIZE) {
            return false;
        }
        MessageDigest sha1 = sha1();
        try (InputStream in = new FileInputStream(jar)) {
            byte[] buffer = new byte[65536];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                sha1.update(buffer, 0, read);
            }
        }
        return SHA1.equals(hex(sha1.digest()));
    }

    private static void download(File target) throws IOException {
        IonClient.LOGGER.info("Downloading the Minecraft {} client jar for its textures ({} MB)", VERSION, SIZE / 1000000);
        File dir = target.getParentFile();
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("cannot create " + dir);
        }
        File part = new File(dir, target.getName() + ".part");
        HttpURLConnection connection = (HttpURLConnection) new URL(URL).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setRequestProperty("User-Agent", "IONClient/" + IonClient.VERSION);
        try {
            int status = connection.getResponseCode();
            if (status != 200) {
                throw new IOException("Mojang answered " + status + " for " + URL);
            }
            MessageDigest sha1 = sha1();
            long total = 0;
            try (InputStream in = connection.getInputStream(); OutputStream out = new FileOutputStream(part)) {
                byte[] buffer = new byte[65536];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    sha1.update(buffer, 0, read);
                    out.write(buffer, 0, read);
                    total += read;
                }
            }
            String hash = hex(sha1.digest());
            if (total != SIZE || !SHA1.equals(hash)) {
                throw new IOException("downloaded jar does not match (" + total + " bytes, sha1 " + hash + ")");
            }
            Files.move(part.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            connection.disconnect();
            if (part.exists() && !part.delete()) {
                part.deleteOnExit();
            }
        }
    }

    static MessageDigest sha1() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            out.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return out.toString();
    }
}
