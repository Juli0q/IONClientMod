package de.ionnetwork.client;

import java.util.Random;

/**
 * The ION lobby panoramas that replace the vanilla title-screen background.
 *
 * <p>Each set is six cubemap faces under
 * {@code assets/ionclient/textures/gui/title/background/<set>/panorama_0..5.png} (front, right,
 * back, left, up, down), shot in the lobby with Complementary Reimagined (+ Euphoria Patches for
 * the night ones). One set is picked per launch so the menu stays consistent within a session.
 *
 * <p>Pure Java 8, no Minecraft imports: each version maps {@link #facePath} onto its own
 * resource-location type.
 */
public final class IonPanoramas {

    private IonPanoramas() {
    }

    public static final String NAMESPACE = "ionclient";

    private static final String[] SETS = {
            "spawn_ring",
            "glass_dome",
            "night_streets",
            "night_skyline",
            "night_tree",
            "stone_hall",
    };

    /** The set shown this launch. */
    public static final String CURRENT = SETS[new Random().nextInt(SETS.length)];

    /** One face of {@link #CURRENT}, relative to {@code assets/ionclient/textures/gui/}, no extension. */
    public static String faceName(int face) {
        return "title/background/" + CURRENT + "/panorama_" + face;
    }

    /** Full resource path of one face of {@link #CURRENT} within {@link #NAMESPACE}. */
    public static String facePath(int face) {
        return "textures/gui/" + faceName(face) + ".png";
    }
}
