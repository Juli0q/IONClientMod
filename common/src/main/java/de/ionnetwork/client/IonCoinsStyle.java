package de.ionnetwork.client;

/** Layout of the coin display in the main menu's top-right corner. */
public final class IonCoinsStyle {

    private IonCoinsStyle() {
    }

    /** The coin icon is 9x9 in the top-left of a 16x16 texture. */
    public static final int ICON_SIZE = 9;
    public static final int ICON_TEXTURE_SIZE = 16;
    /** Distance from the screen's top and right edges. */
    public static final int MARGIN = 6;
    /** Padding inside the panel. */
    public static final int PAD = 3;
    /** Gap between the icon and the number. */
    public static final int GAP = 4;
    public static final int HEIGHT = PAD + ICON_SIZE + PAD;
    public static final int FILL = IonEntryStyle.CARD_FILL;
    public static final int FILL_HOVER = IonEntryStyle.CARD_FILL_ACTIVE;
    public static final int TEXT = IonBrand.WHITE;
    public static final String TOOLTIP = "ION Coins";

    /** Total panel width for a number of the given pixel width. */
    public static int width(int numberWidth) {
        return PAD + ICON_SIZE + GAP + numberWidth + PAD;
    }
}
