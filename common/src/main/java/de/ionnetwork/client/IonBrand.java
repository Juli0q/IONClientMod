package de.ionnetwork.client;

/**
 * Everything that makes the mod look and point like ION Network, in one place.
 *
 * <p>The colours mirror the website's theme tokens ({@code app.css}): the two greys the site is
 * built on, the house periwinkle and its deeper partner, and the three state colours. The logo
 * gradient is sampled from the ION wordmark (cyan at the top, blue, periwinkle, purple at the
 * bottom) so the pinned entry's accent stripe reads as the same brand as the site header.
 *
 * <p>Pure Java 8, no Minecraft imports: this file is compiled into every version and loader
 * target, so it must not know anything about rendering.
 */
public final class IonBrand {

    private IonBrand() {
    }

    // ---- identity -------------------------------------------------------------------------

    public static final String SERVER_NAME = "ION Network";
    public static final String SERVER_ADDRESS = "ion-network.de";
    public static final String WEBSITE = "https://ion-network.de";
    public static final String DISCORD_INVITE = "https://discord.gg/rNP4Qfvj9B";
    public static final String TAGLINE = "Bowbash • CrystalHunt • Bedwars • PirateCraft • IONJumps";

    // ---- website theme tokens (ARGB) ------------------------------------------------------

    /** {@code --color-ionGrayer}: the page background. */
    public static final int GRAYER = 0xFF1E212B;
    /** {@code --color-ionGray}: cards and raised surfaces. */
    public static final int GRAY = 0xFF262934;
    /** {@code --color-ionAqua}: the house periwinkle accent. */
    public static final int AQUA = 0xFF7777DF;
    /** {@code --color-ionAquaer}: the deeper periwinkle used for hover and borders. */
    public static final int AQUA_DEEP = 0xFF6565B8;
    /** {@code --color-ionGood}. */
    public static final int GOOD = 0xFF25AB90;
    /** {@code --color-ionWarn}. */
    public static final int WARN = 0xFFC88312;
    /** {@code --color-ionCritical}. */
    public static final int CRITICAL = 0xFFDE4E45;

    public static final int WHITE = 0xFFFFFFFF;
    /** Muted copy, the site's {@code text-neutral-400}. */
    public static final int TEXT_MUTED = 0xFFA3A3A3;
    /** Disabled copy, the site's {@code text-neutral-500}. */
    public static final int TEXT_DIM = 0xFF737373;
    /** The emerald pulse dot next to the live player count on the website hero. */
    public static final int ONLINE_DOT = 0xFF34D399;

    // ---- the logo gradient ----------------------------------------------------------------

    /** Colour stops of the ION wordmark, top to bottom. */
    public static final int[] LOGO_GRADIENT = {
            0xFF5CD3F0, // cyan
            0xFF4F9EF5, // blue
            0xFF7777DF, // periwinkle
            0xFF8A6BD8, // purple
    };

    /**
     * Samples the logo gradient at {@code t} in {@code [0, 1]}.
     */
    public static int gradientAt(float t) {
        if (t <= 0f) {
            return LOGO_GRADIENT[0];
        }
        if (t >= 1f) {
            return LOGO_GRADIENT[LOGO_GRADIENT.length - 1];
        }
        float scaled = t * (LOGO_GRADIENT.length - 1);
        int index = (int) scaled;
        return lerp(LOGO_GRADIENT[index], LOGO_GRADIENT[index + 1], scaled - index);
    }

    // ---- colour helpers -------------------------------------------------------------------

    /** Linear interpolation of two ARGB colours, channel by channel. */
    public static int lerp(int from, int to, float t) {
        if (t <= 0f) {
            return from;
        }
        if (t >= 1f) {
            return to;
        }
        int a = lerpChannel(from >>> 24, to >>> 24, t);
        int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerpChannel(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Replaces the alpha of an ARGB colour. {@code alpha} is {@code 0..255}. */
    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpChannel(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }
}
