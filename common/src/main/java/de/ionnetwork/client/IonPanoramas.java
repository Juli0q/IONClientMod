package de.ionnetwork.client;

import java.util.Random;

/**
 * The ION lobby panoramas that replace the vanilla title-screen background.
 *
 * <p>Each set is six cubemap faces under
 * {@code assets/ionclient/textures/gui/title/background/<set>/panorama_0..5.png} (front, right,
 * back, left, up, down), shot in the lobby with Complementary Reimagined (+ Euphoria Patches for
 * the night ones). By default one set is picked per launch so the menu stays consistent within a
 * session; the settings screen can pin a set or go back to the vanilla panorama instead.
 *
 * <p>Pure Java 8, no Minecraft imports: each version maps {@link #facePath} onto its own
 * resource-location type.
 */
public final class IonPanoramas {

    private IonPanoramas() {
    }

    public static final String NAMESPACE = "ionclient";

    /** Every set, in the order the settings screen cycles through them. */
    public static final String[] SETS = {
            "spawn_ring",
            "glass_dome",
            "night_streets",
            "night_skyline",
            "night_tree",
            "stone_hall",
    };

    /** The set {@link IonSettings#RANDOM_PANORAMA} resolves to this launch. */
    public static final String RANDOM = SETS[new Random().nextInt(SETS.length)];

    /**
     * The set the menu shows right now, following the setting: this launch's random pick, a
     * chosen set, or null for the vanilla panorama. An unknown name (a set removed in a later
     * version) falls back to the random pick.
     */
    public static String active() {
        String chosen = IonSettings.panorama();
        if (IonSettings.VANILLA_PANORAMA.equals(chosen)) {
            return null;
        }
        for (String set : SETS) {
            if (set.equals(chosen)) {
                return set;
            }
        }
        return RANDOM;
    }

    /** {@code night_streets} as {@code Night Streets}. */
    public static String displayName(String set) {
        StringBuilder out = new StringBuilder(set.length());
        for (String word : set.split("_")) {
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    /** One face of {@code set}, relative to {@code assets/ionclient/textures/gui/}, no extension. */
    public static String faceName(String set, int face) {
        return "title/background/" + set + "/panorama_" + face;
    }

    /** Full resource path of one face of {@code set} within {@link #NAMESPACE}. */
    public static String facePath(String set, int face) {
        return "textures/gui/" + faceName(set, face) + ".png";
    }
}
