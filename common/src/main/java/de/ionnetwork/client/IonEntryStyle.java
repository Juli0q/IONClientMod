package de.ionnetwork.client;

/**
 * The layout and state rules of the pinned server entry, shared by every version's renderer.
 *
 * <p>Each Minecraft version draws with its own API (Tessellator in 1.8.9, GuiGraphics in 1.21),
 * but what gets drawn where, in which colour, and what the status line says, is decided here so
 * the entry looks the same everywhere.
 */
public final class IonEntryStyle {

    private IonEntryStyle() {
    }

    /** Width of the gradient stripe on the entry's left edge. */
    public static final int STRIPE_WIDTH = 2;
    /** The server icon is the vanilla 32x32. */
    public static final int ICON_SIZE = 32;
    /** Gap between the stripe and the icon, and between the icon and the text column. */
    public static final int GAP = 4;
    /** Horizontal start of the text column, relative to the entry's x. */
    public static final int TEXT_X = STRIPE_WIDTH + GAP + ICON_SIZE + GAP;
    /** Vertical offsets of the name line and the first MOTD line, relative to the entry's y. */
    public static final int LINE_1_Y = 2;
    public static final int LINE_2_Y = 12;
    /** Fill of the card: the site's page grey, letting a little of the dirt show through. */
    public static final int CARD_FILL = IonBrand.withAlpha(IonBrand.GRAYER, 205);
    /** Fill of the card while hovered or selected. */
    public static final int CARD_FILL_ACTIVE = IonBrand.withAlpha(IonBrand.GRAY, 225);

    /** The brand word of the server name, drawn letter by letter in the logo gradient. */
    public static final String BRAND_WORD = "ION";
    /** The rest of the server name, drawn in white after {@link #BRAND_WORD}. */
    public static final String BRAND_REST = " Network";

    /** Colour of letter {@code index} of {@link #BRAND_WORD}, sampled along the logo gradient. */
    public static int brandLetterColor(int index) {
        int last = BRAND_WORD.length() - 1;
        return IonBrand.gradientAt(last == 0 ? 0f : index / (float) last);
    }
    /** The pinned entry's ping bars sit where vanilla puts them: 15px from the right edge. */
    public static final int PING_ICON_RIGHT_INSET = 15;

    /** What the pinger has told us so far. */
    public enum Status {
        PINGING,
        ONLINE,
        OFFLINE
    }

    /**
     * Derives the status from the fields vanilla's pinger writes into a server entry.
     *
     * @param pingStarted whether a ping has been dispatched for this entry
     * @param pingMillis  the measured ping; {@code -2} while pending, {@code < 0} on failure
     */
    public static Status status(boolean pingStarted, long pingMillis) {
        if (!pingStarted || pingMillis == -2L) {
            return Status.PINGING;
        }
        return pingMillis < 0L ? Status.OFFLINE : Status.ONLINE;
    }

    /**
     * Vanilla's ping-bar bucket: {@code 0} is five bars, {@code 4} is one bar, {@code 5} is the
     * red cross. Only meaningful when the status is ONLINE or OFFLINE.
     */
    public static int pingBars(long pingMillis) {
        if (pingMillis < 0L) {
            return 5;
        }
        if (pingMillis < 150L) {
            return 0;
        }
        if (pingMillis < 300L) {
            return 1;
        }
        if (pingMillis < 600L) {
            return 2;
        }
        if (pingMillis < 1000L) {
            return 3;
        }
        return 4;
    }

    /**
     * Vanilla's "pinging" animation frame for the bars, cycling at the same speed as the
     * vanilla entries so the two never look out of step.
     */
    public static int pingingFrame(long nowMillis, int slotIndex) {
        int frame = (int) (nowMillis / 100L + slotIndex * 2L & 7L);
        return frame > 4 ? 8 - frame : frame;
    }

    /** Removes {@code §x} formatting codes. */
    public static String stripFormatting(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }
}
