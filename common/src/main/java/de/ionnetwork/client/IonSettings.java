package de.ionnetwork.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * The player's ION Client settings, kept in {@code config/ionclient.json}.
 *
 * <p>Every feature reads its setting when it runs (the menu reads the panorama each frame, the
 * server list reads the pin each time it loads), so a change applies without a restart. Until
 * {@link #load} has run, and for any key the file lacks or cannot be read, the defaults apply,
 * which are the mod's behaviour from before it had settings.
 *
 * <p>Pure Java 8, no Minecraft imports: each version passes its loader's config directory in.
 */
public final class IonSettings {

    public static final String FILE_NAME = "ionclient.json";
    /** {@link #panorama()} value: one ION set per launch, picked at random. */
    public static final String RANDOM_PANORAMA = "random";
    /** {@link #panorama()} value: Minecraft's own panorama. */
    public static final String VANILLA_PANORAMA = "vanilla";

    private static volatile File file;
    private static volatile String panorama = RANDOM_PANORAMA;
    private static volatile boolean showCoins = true;
    private static volatile boolean pinServer = true;
    private static volatile boolean matchDesktopScale = true;
    private static volatile boolean modernTextures = true;
    private static volatile String steamKeyboard = IonSteamKeyboard.AUTO;
    private static volatile int chatLift = 60;

    private IonSettings() {
    }

    /** {@link #RANDOM_PANORAMA}, {@link #VANILLA_PANORAMA} or one of {@link IonPanoramas#SETS}. */
    public static String panorama() {
        return panorama;
    }

    public static void setPanorama(String value) {
        panorama = value;
    }

    /** Whether the main menu shows the ION coin balance. */
    public static boolean showCoins() {
        return showCoins;
    }

    public static void setShowCoins(boolean value) {
        showCoins = value;
    }

    /** Whether ION Network is pinned to the top of the multiplayer list. */
    public static boolean pinServer() {
        return pinServer;
    }

    public static void setPinServer(boolean value) {
        pinServer = value;
    }

    /** 1.8.9 only: whether the GUI scale is multiplied by the desktop scale under LWJGL3. */
    public static boolean matchDesktopScale() {
        return matchDesktopScale;
    }

    public static void setMatchDesktopScale(boolean value) {
        matchDesktopScale = value;
    }

    /**
     * 1.8.9 only: whether the "ION Modern Textures" resource pack is selected. The Resource Packs
     * screen changes it too, so it always says what that screen shows.
     */
    public static boolean modernTextures() {
        return modernTextures;
    }

    public static void setModernTextures(boolean value) {
        modernTextures = value;
    }

    /** {@link IonSteamKeyboard#AUTO}, {@link IonSteamKeyboard#ON} or {@link IonSteamKeyboard#OFF}. */
    public static String steamKeyboard() {
        return steamKeyboard;
    }

    public static void setSteamKeyboard(String value) {
        steamKeyboard = value;
    }

    /**
     * How much of the screen's height, in percent, the chat moves up while Steam's keyboard is
     * docked over the bottom of the screen (see {@link IonSteamKeyboard#chatLift}).
     */
    public static int chatLift() {
        return chatLift;
    }

    public static void setChatLift(int value) {
        chatLift = value;
    }

    /**
     * Reads {@code <configDir>/ionclient.json} and remembers it for {@link #save}. A missing file
     * is not an error: the defaults stay and the first save creates it.
     */
    public static void load(File configDir) throws IOException {
        File target = new File(configDir, FILE_NAME);
        file = target;
        if (!target.isFile()) {
            return;
        }
        JsonElement parsed;
        try (Reader reader = new InputStreamReader(new FileInputStream(target), StandardCharsets.UTF_8)) {
            parsed = new JsonParser().parse(reader);
        } catch (RuntimeException e) {
            throw new IOException("cannot parse " + target, e);
        }
        if (!parsed.isJsonObject()) {
            throw new IOException(target + " is not a JSON object");
        }
        JsonObject json = parsed.getAsJsonObject();
        panorama = string(json, "panorama", panorama);
        showCoins = bool(json, "showCoins", showCoins);
        pinServer = bool(json, "pinServer", pinServer);
        matchDesktopScale = bool(json, "matchDesktopScale", matchDesktopScale);
        modernTextures = bool(json, "modernTextures", modernTextures);
        steamKeyboard = string(json, "steamKeyboard", steamKeyboard);
        chatLift = integer(json, "chatLift", chatLift);
    }

    /** Writes the settings back to the file {@link #load} read. Does nothing before that. */
    public static void save() throws IOException {
        File target = file;
        if (target == null) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("panorama", panorama);
        json.addProperty("showCoins", showCoins);
        json.addProperty("pinServer", pinServer);
        json.addProperty("matchDesktopScale", matchDesktopScale);
        json.addProperty("modernTextures", modernTextures);
        json.addProperty("steamKeyboard", steamKeyboard);
        json.addProperty("chatLift", chatLift);

        File dir = target.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("cannot create " + dir);
        }
        // Written beside the file and moved over it, so a crash mid-write never leaves half a file.
        File temp = new File(dir, FILE_NAME + ".tmp");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(temp), StandardCharsets.UTF_8)) {
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(json));
            writer.write('\n');
        }
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : fallback;
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? value.getAsInt() : fallback;
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() ? value.getAsBoolean() : fallback;
    }
}
