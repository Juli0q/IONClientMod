package de.ionnetwork.client.sound;

import de.ionnetwork.client.IonClient;
import net.minecraft.util.ResourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Plays the sound event names of newer Minecraft versions on 1.8.9.
 *
 * <p>The sound packet has carried the event's name as a plain string since 1.8, and ION's servers
 * send the names of the version they run (or the top rung of a sound ladder), such as
 * {@code entity.player.levelup} or {@code block.note_block.pling}. Vanilla 1.8.9 only knows its own
 * names ({@code random.levelup}, {@code note.pling}) and drops the rest with a warning. This table,
 * {@code assets/ionclient/sounds/legacy_names.txt}, says which 1.8.9 sound each modern name is, so
 * the sound plays as it would for a 1.8 player on a Via-proxied modern server: it is generated from
 * ViaVersion's own mapping data by {@code tools/legacy_sound_names.py}.
 */
public final class LegacySoundNames {

    private static final String TABLE = "/assets/ionclient/sounds/legacy_names.txt";
    private static volatile Map<String, String> table;

    private LegacySoundNames() {
    }

    /**
     * The 1.8.9 sound a modern event name stands for, or null when it is not a modern name (or is a
     * name this version already has, which never reaches here because the registry answers first).
     */
    public static ResourceLocation legacy(ResourceLocation modern) {
        if (modern == null || !"minecraft".equals(modern.getResourceDomain())) {
            return null;
        }
        String legacy = table().get(modern.getResourcePath());
        return legacy == null ? null : new ResourceLocation(legacy);
    }

    private static Map<String, String> table() {
        Map<String, String> loaded = table;
        if (loaded == null) {
            synchronized (LegacySoundNames.class) {
                loaded = table;
                if (loaded == null) {
                    table = loaded = load();
                }
            }
        }
        return loaded;
    }

    private static Map<String, String> load() {
        Map<String, String> names = new HashMap<>();
        InputStream in = LegacySoundNames.class.getResourceAsStream(TABLE);
        if (in == null) {
            IonClient.LOGGER.warn("{} is missing from the jar; modern sound names will not play", TABLE);
            return Collections.emptyMap();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int comment = line.indexOf('#');
                if (comment >= 0) {
                    line = line.substring(0, comment);
                }
                int equals = line.indexOf('=');
                if (equals < 0) {
                    continue;
                }
                String modern = line.substring(0, equals).trim();
                String legacy = line.substring(equals + 1).trim();
                if (!modern.isEmpty() && !legacy.isEmpty()) {
                    names.put(modern, legacy);
                }
            }
        } catch (IOException e) {
            IonClient.LOGGER.warn("Could not read {}; modern sound names will not play", TABLE, e);
        }
        IonClient.LOGGER.debug("Loaded {} modern sound names", names.size());
        return Collections.unmodifiableMap(names);
    }
}
