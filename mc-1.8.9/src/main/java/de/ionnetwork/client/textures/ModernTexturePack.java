package de.ionnetwork.client.textures;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Builds a 1.8.9 resource pack from a modern client jar: block and item textures, renamed back to
 * their 1.8.9 paths by {@code assets/ionclient/modern_textures/mapping.txt}.
 *
 * <p>Pure Java (no Minecraft classes): which textures 1.8.9 has is asked through a predicate, so
 * the game answers it from its own default pack and a test can answer it from a 1.8.9 jar.
 */
public final class ModernTexturePack {

    /** Bump when the converter's output changes for the same jar and table. */
    private static final int REVISION = 2;
    private static final String MAPPING = "/assets/ionclient/modern_textures/mapping.txt";
    private static final String TEXTURES = "assets/minecraft/textures/";
    private static final Pattern FRAMES = Pattern.compile("(.*)\\{(\\d+)\\.\\.(\\d+)}");

    private ModernTexturePack() {
    }

    /** Identifies the output of this converter, table and source jar; stored as the zip's comment. */
    public static String stamp() throws IOException {
        byte[] table = mappingText().getBytes(StandardCharsets.UTF_8);
        String hash = ModernTextureSource.hex(ModernTextureSource.sha1().digest(table)).substring(0, 12);
        return "ionclient-modern-textures " + ModernTextureSource.VERSION + " r" + REVISION + " " + hash;
    }

    /**
     * Writes the pack to {@code out}.
     *
     * @param legacyExists whether 1.8.9 has a texture, given its path below textures/ without ".png"
     * @return the number of textures written
     */
    public static int build(ZipFile modern, Predicate<String> legacyExists, OutputStream out) throws IOException {
        Map<String, Rule> rules = rules(modern, legacyExists);
        int written = 0;
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.setComment(stamp());
            put(zip, "pack.mcmeta", ("{\"pack\":{\"pack_format\":1,\"description\":\"Textures from Minecraft "
                    + ModernTextureSource.VERSION + "\\n\\u00a77Converted by ION Client\"}}").getBytes(StandardCharsets.UTF_8));
            ZipEntry icon = modern.getEntry("pack.png");
            if (icon != null) {
                put(zip, "pack.png", read(modern, icon));
            }
            for (Map.Entry<String, Rule> entry : rules.entrySet()) {
                if (convert(modern, entry.getKey(), entry.getValue(), zip)) {
                    written++;
                }
            }
        }
        return written;
    }

    /** The table's rules, plus same-name rules for every 1.8.9 texture the table doesn't mention. */
    static Map<String, Rule> rules(ZipFile modern, Predicate<String> legacyExists) throws IOException {
        Map<String, Rule> rules = new LinkedHashMap<>();
        List<String> listed = new ArrayList<>();
        for (String raw : mappingText().split("\n")) {
            String line = raw.replaceFirst("#.*", "").trim();
            if (line.isEmpty()) {
                continue;
            }
            int equals = line.indexOf('=');
            String legacy = line.substring(0, equals).trim();
            String[] target = line.substring(equals + 1).trim().split("\\s+");
            listed.add(legacy);
            if (!target[0].equals("-")) {
                rules.put(legacy, new Rule(target[0], target.length == 3 && target[1].equals("tint") ? parseTint(target[2]) : null));
            }
        }
        Enumeration<? extends ZipEntry> entries = modern.entries();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            for (String[] folder : new String[][]{{"block/", "blocks/"}, {"item/", "items/"}}) {
                String prefix = TEXTURES + folder[0];
                if (name.startsWith(prefix) && name.endsWith(".png") && name.indexOf('/', prefix.length()) < 0) {
                    String base = name.substring(prefix.length(), name.length() - ".png".length());
                    String legacy = folder[1] + base;
                    if (!listed.contains(legacy) && legacyExists.test(legacy)) {
                        rules.put(legacy, new Rule(folder[0] + base, null));
                    }
                }
            }
        }
        return rules;
    }

    private static boolean convert(ZipFile modern, String legacy, Rule rule, ZipOutputStream zip) throws IOException {
        List<String> frames = rule.frames();
        BufferedImage image = frames.size() > 1 ? stitch(modern, frames) : readImage(modern, frames.get(0));
        if (image == null) {
            return false;
        }
        if (rule.tint != null) {
            tint(image, rule.tint);
        }
        // Always re-encoded: modern textures are often grey or palette PNGs, 1.8.9's are all RGBA.
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        put(zip, TEXTURES + legacy + ".png", png.toByteArray());

        // A strip taller than wide is an animation; 1.8.9 needs the .mcmeta next to it to read it as one.
        if (image.getHeight() > image.getWidth()) {
            put(zip, TEXTURES + legacy + ".png.mcmeta", animation(modern, frames.size() > 1 ? null : frames.get(0)));
        }
        return true;
    }

    /** The modern texture's animation section alone; its other sections are unknown to 1.8.9. */
    private static byte[] animation(ZipFile modern, String path) throws IOException {
        JsonElement animation = null;
        ZipEntry meta = path == null ? null : modern.getEntry(TEXTURES + path + ".png.mcmeta");
        if (meta != null) {
            try (InputStream in = modern.getInputStream(meta)) {
                JsonElement parsed = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8));
                if (parsed.isJsonObject()) {
                    animation = parsed.getAsJsonObject().get("animation");
                }
            }
        }
        JsonObject out = new JsonObject();
        out.add("animation", animation != null && animation.isJsonObject() ? animation : new JsonObject());
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static BufferedImage stitch(ZipFile modern, List<String> frames) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        for (String frame : frames) {
            BufferedImage image = readImage(modern, frame);
            if (image == null) {
                return null;
            }
            images.add(image);
        }
        int width = images.get(0).getWidth();
        int height = images.get(0).getHeight();
        BufferedImage strip = new BufferedImage(width, height * images.size(), BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < images.size(); i++) {
            BufferedImage frame = images.get(i);
            if (frame.getWidth() != width || frame.getHeight() != height) {
                return null;
            }
            strip.setRGB(0, i * height, width, height, frame.getRGB(0, 0, width, height, null, 0, width), 0, width);
        }
        return strip;
    }

    private static void tint(BufferedImage image, float[] tint) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * tint[0]));
                int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * tint[1]));
                int b = Math.min(255, Math.round((argb & 0xFF) * tint[2]));
                image.setRGB(x, y, (argb & 0xFF000000) | (r << 16) | (g << 8) | b);
            }
        }
    }

    private static BufferedImage readImage(ZipFile modern, String path) throws IOException {
        ZipEntry entry = modern.getEntry(TEXTURES + path + ".png");
        if (entry == null) {
            return null;
        }
        byte[] png = read(modern, entry);
        BufferedImage read = ImageIO.read(new ByteArrayInputStream(png));
        if (read == null) {
            return null;
        }
        BufferedImage argb = new BufferedImage(read.getWidth(), read.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Raster raster = read.getRaster();
        boolean grey = read.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_GRAY;
        if (grey) {
            // Java treats grey PNGs as linear and brightens them when converting to sRGB (1.8.9's own
            // loader does the same), so take the stored values as they are.
            int max = (1 << read.getColorModel().getComponentSize(0)) - 1;
            boolean alpha = raster.getNumBands() > 1;
            for (int y = 0; y < read.getHeight(); y++) {
                for (int x = 0; x < read.getWidth(); x++) {
                    int value = raster.getSample(x, y, 0) * 255 / max;
                    int a = alpha ? raster.getSample(x, y, 1) * 255 / max : 255;
                    argb.setRGB(x, y, (a << 24) | (value << 16) | (value << 8) | value);
                }
            }
        } else {
            // A straight copy: drawing would blend translucent pixels onto the empty canvas and round them.
            int width = read.getWidth();
            argb.setRGB(0, 0, width, read.getHeight(), read.getRGB(0, 0, width, read.getHeight(), null, 0, width), 0, width);
        }

        // Grey and RGB PNGs can mark one colour transparent (tRNS); Java 8 ignores that, so apply it here.
        int[] key = transparentKey(png);
        if (key != null && !read.getColorModel().hasAlpha() && raster.getNumBands() == key.length) {
            for (int y = 0; y < read.getHeight(); y++) {
                for (int x = 0; x < read.getWidth(); x++) {
                    boolean match = true;
                    for (int band = 0; band < key.length && match; band++) {
                        match = raster.getSample(x, y, band) == key[band];
                    }
                    if (match) {
                        argb.setRGB(x, y, argb.getRGB(x, y) & 0x00FFFFFF);
                    }
                }
            }
        }
        return argb;
    }

    /** The tRNS colour key of a grey (one sample) or RGB (three samples) PNG, or null when it has none. */
    static int[] transparentKey(byte[] png) {
        if (png.length < 33) {
            return null;
        }
        int colorType = png[25] & 0xFF;
        if (colorType != 0 && colorType != 2) {
            return null;
        }
        int offset = 8;
        while (offset + 8 <= png.length) {
            int length = ((png[offset] & 0xFF) << 24) | ((png[offset + 1] & 0xFF) << 16)
                    | ((png[offset + 2] & 0xFF) << 8) | (png[offset + 3] & 0xFF);
            String type = new String(png, offset + 4, 4, StandardCharsets.US_ASCII);
            if (type.equals("tRNS")) {
                int samples = colorType == 0 ? 1 : 3;
                if (length < samples * 2 || offset + 8 + samples * 2 > png.length) {
                    return null;
                }
                int[] key = new int[samples];
                for (int i = 0; i < samples; i++) {
                    key[i] = ((png[offset + 8 + i * 2] & 0xFF) << 8) | (png[offset + 9 + i * 2] & 0xFF);
                }
                return key;
            }
            if (type.equals("IDAT") || length < 0) {
                return null;
            }
            offset += 12 + length;
        }
        return null;
    }

    private static float[] parseTint(String value) {
        String[] parts = value.split(",");
        return new float[]{Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2])};
    }

    private static void put(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    private static byte[] read(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream in = zip.getInputStream(entry)) {
            return readAll(in);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[16384];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static String mappingText() throws IOException {
        InputStream in = ModernTexturePack.class.getResourceAsStream(MAPPING);
        if (in == null) {
            throw new IOException(MAPPING + " is missing from the jar");
        }
        try {
            return new String(readAll(in), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    /** One 1.8.9 texture's source: a modern path, or a {from..to} range of frames, and an optional tint. */
    static final class Rule {
        final String target;
        final float[] tint;

        Rule(String target, float[] tint) {
            this.target = target;
            this.tint = tint;
        }

        List<String> frames() {
            Matcher range = FRAMES.matcher(target);
            if (!range.matches()) {
                return Collections.singletonList(target);
            }
            int digits = range.group(2).length();
            List<String> frames = new ArrayList<>();
            for (int i = Integer.parseInt(range.group(2)); i <= Integer.parseInt(range.group(3)); i++) {
                frames.add(range.group(1) + String.format("%0" + digits + "d", i));
            }
            return frames;
        }
    }
}
