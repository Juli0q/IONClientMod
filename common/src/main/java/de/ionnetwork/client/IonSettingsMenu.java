package de.ionnetwork.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * What the ION Client settings screen shows and where, shared by every version's screen.
 *
 * <p>Each version draws with its own widgets (GuiButton in 1.8.9, CycleButton in 1.21), but the
 * sections, the options, their wording and the layout are decided here, so the screen reads the
 * same everywhere. The look follows vanilla's options screens: a header with the title, a darker
 * band holding two columns of 150px buttons, a footer with Done. The ION accents are the title's
 * "ION" in the logo gradient and the gradient line under the header.
 */
public final class IonSettingsMenu {

    private IonSettingsMenu() {
    }

    /** The title is {@link IonEntryStyle#BRAND_WORD} in the logo gradient, then this in white. */
    public static final String TITLE_REST = " Client";
    /** The options-screen button that opens this screen, drawn the same way as the title. */
    public static final String BUTTON_REST = " Client...";

    public static final int HEADER_HEIGHT = 33;
    public static final int FOOTER_HEIGHT = 33;
    /** The gradient line at the bottom of the header (where vanilla draws its separator). */
    public static final int SEPARATOR_HEIGHT = 2;
    public static final int BUTTON_WIDTH = 150;
    public static final int BUTTON_HEIGHT = 20;
    public static final int COLUMN_GAP = 10;
    public static final int ROW_HEIGHT = BUTTON_HEIGHT + 4;
    /** A section heading's line, including the gap to its buttons. */
    public static final int HEADING_HEIGHT = 13;
    public static final int SECTION_GAP = 6;
    public static final int DONE_WIDTH = 200;
    /** The widest a tooltip wraps to. */
    public static final int TOOLTIP_WIDTH = 200;
    public static final int HEADING_COLOR = IonBrand.TEXT_MUTED;
    /**
     * Where a version has no texture for them (1.8.9), the band and the footer separator are
     * filled in the colours of 1.21's {@code menu_list_background} and {@code footer_separator}.
     */
    public static final int BAND_FILL = 0x70000000;
    public static final int FOOTER_LINE_DARK = 0xBF000000;
    public static final int FOOTER_LINE_LIGHT = 0x33FFFFFF;

    // ---- options ----------------------------------------------------------------------------

    /** A labelled group of options. */
    public static final class Section {
        public final String title;
        public final List<Item> items;

        Section(String title, Item... items) {
            this.title = title;
            this.items = Collections.unmodifiableList(Arrays.asList(items));
        }
    }

    /** One button. */
    public abstract static class Item {
        public final String label;

        Item(String label) {
            this.label = label;
        }

        public abstract String tooltip();
    }

    /** A setting that cycles through fixed values: "Label: Value". */
    public abstract static class Choice extends Item {
        private final String tooltip;
        private final boolean enabled;
        /** Whether changing it changes the screen's scaled size, so the screen must lay out again. */
        public final boolean resizesScreen;

        Choice(String label, String tooltip, boolean enabled, boolean resizesScreen) {
            super(label);
            this.tooltip = tooltip;
            this.enabled = enabled;
            this.resizesScreen = resizesScreen;
        }

        /** The display text of every value, in cycling order. */
        public abstract List<String> values();

        public abstract int selected();

        /** Applies value {@code index}; saving is left to the screen closing, as in vanilla. */
        public abstract void select(int index);

        public boolean enabled() {
            return enabled;
        }

        @Override
        public String tooltip() {
            return tooltip;
        }

        /** Vanilla's wording: {@code Label: Value}. */
        public String message() {
            return label + ": " + values().get(selected());
        }

        /** Moves to the next value, or the previous one (shift-click, as vanilla). */
        public void cycle(boolean backwards) {
            int count = values().size();
            select(Math.floorMod(selected() + (backwards ? -1 : 1), count));
        }
    }

    /** A button that opens a web page, after vanilla's "open this link?" prompt. */
    public static final class Link extends Item {
        public final String url;

        Link(String label, String url) {
            super(label);
            this.url = url;
        }

        @Override
        public String tooltip() {
            return url.replaceFirst("^https?://", "");
        }
    }

    private static final String ON = "ON";
    private static final String OFF = "OFF";
    private static final List<String> ON_OFF = Collections.unmodifiableList(Arrays.asList(ON, OFF));

    /**
     * The screen's sections.
     *
     * @param desktopScale the desktop scale factor when the version supports matching it (1.8.9),
     *                     or {@code 0} to leave the option out; {@code 1} shows it disabled, since
     *                     only the LWJGL3 runtime reports a scaled desktop
     */
    public static List<Section> sections(double desktopScale) {
        List<Section> sections = new ArrayList<>();
        sections.add(new Section("Menus", panorama(), coins(), pin()));
        if (desktopScale > 0) {
            sections.add(new Section("Display", desktopScale(desktopScale)));
        }
        sections.add(new Section(IonBrand.SERVER_NAME,
                new Link("Website", IonBrand.WEBSITE),
                new Link("Discord", IonBrand.DISCORD_INVITE)));
        return sections;
    }

    private static Choice panorama() {
        List<String> keys = new ArrayList<>();
        List<String> names = new ArrayList<>();
        keys.add(IonSettings.RANDOM_PANORAMA);
        names.add("Random");
        for (String set : IonPanoramas.SETS) {
            keys.add(set);
            names.add(IonPanoramas.displayName(set));
        }
        keys.add(IonSettings.VANILLA_PANORAMA);
        names.add("Minecraft");
        List<String> values = Collections.unmodifiableList(names);
        return new Choice("Panorama", "The main menu background. Random shows a different ION lobby shot each launch; "
                + "Minecraft brings back the vanilla panorama.", true, false) {
            @Override
            public List<String> values() {
                return values;
            }

            @Override
            public int selected() {
                int index = keys.indexOf(IonSettings.panorama());
                return index < 0 ? 0 : index;
            }

            @Override
            public void select(int index) {
                IonSettings.setPanorama(keys.get(index));
            }
        };
    }

    private static Choice coins() {
        return toggle("ION Coins", "Shows your ION coin balance in the main menu's top-right corner.", true, false,
                IonSettings::showCoins, IonSettings::setShowCoins);
    }

    private static Choice pin() {
        return toggle("Pinned Server", "Keeps " + IonBrand.SERVER_NAME + " at the top of your multiplayer server list.",
                true, false, IonSettings::pinServer, IonSettings::setPinServer);
    }

    private static Choice desktopScale(double scale) {
        boolean scaled = scale > 1.0;
        String tooltip = "Multiplies the GUI scale by your desktop's scale, so menus keep their size on a scaled display. "
                + (scaled ? String.format(Locale.ROOT, "Your desktop is scaled %.2fx.", scale).replace(".00x", "x")
                : "Only applies when the game runs on LWJGL 3 on a scaled Linux Wayland desktop.");
        return toggle("Desktop Scale", tooltip, scaled, true, IonSettings::matchDesktopScale, IonSettings::setMatchDesktopScale);
    }

    private interface Getter {
        boolean get();
    }

    private interface Setter {
        void set(boolean value);
    }

    private static Choice toggle(String label, String tooltip, boolean enabled, boolean resizesScreen, Getter get, Setter set) {
        return new Choice(label, tooltip, enabled, resizesScreen) {
            @Override
            public List<String> values() {
                return ON_OFF;
            }

            @Override
            public int selected() {
                return get.get() ? 0 : 1;
            }

            @Override
            public void select(int index) {
                set.set(index == 0);
            }
        };
    }

    // ---- layout -----------------------------------------------------------------------------

    /** Where a section heading goes. */
    public static final class Heading {
        public final String text;
        public final int x;
        public final int y;

        Heading(String text, int x, int y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }
    }

    /** Where an item's button goes. */
    public static final class Slot {
        public final Item item;
        public final int x;
        public final int y;
        public final int width;

        Slot(Item item, int x, int y, int width) {
            this.item = item;
            this.x = x;
            this.y = y;
            this.width = width;
        }
    }

    /** The placed headings and buttons of one screen size. */
    public static final class Layout {
        public final List<Heading> headings = new ArrayList<>();
        public final List<Slot> slots = new ArrayList<>();
    }

    /**
     * Places the sections in two columns, centred in the band between header and footer (or from
     * its top when they do not fit). A section's buttons fill left to right, top to bottom.
     */
    public static Layout layout(List<Section> sections, int screenWidth, int screenHeight) {
        int contentHeight = 0;
        for (Section section : sections) {
            contentHeight += HEADING_HEIGHT + rows(section) * ROW_HEIGHT;
        }
        contentHeight += SECTION_GAP * (sections.size() - 1) - (ROW_HEIGHT - BUTTON_HEIGHT);

        int bandTop = HEADER_HEIGHT;
        int bandHeight = screenHeight - HEADER_HEIGHT - FOOTER_HEIGHT;
        int y = bandTop + Math.max(SECTION_GAP, (bandHeight - contentHeight) / 2);
        int left = screenWidth / 2 - BUTTON_WIDTH - COLUMN_GAP / 2;

        Layout layout = new Layout();
        for (Section section : sections) {
            layout.headings.add(new Heading(section.title, left, y));
            y += HEADING_HEIGHT;
            for (int i = 0; i < section.items.size(); i++) {
                int column = i % 2;
                int row = i / 2;
                layout.slots.add(new Slot(section.items.get(i), left + column * (BUTTON_WIDTH + COLUMN_GAP),
                        y + row * ROW_HEIGHT, BUTTON_WIDTH));
            }
            y += rows(section) * ROW_HEIGHT + SECTION_GAP;
        }
        return layout;
    }

    private static int rows(Section section) {
        return (section.items.size() + 1) / 2;
    }

    /** The Done button's top edge, centred in the footer. */
    public static int doneY(int screenHeight) {
        return screenHeight - FOOTER_HEIGHT + (FOOTER_HEIGHT - BUTTON_HEIGHT) / 2;
    }

    /** Colour of column {@code x} of the header line, sweeping the logo gradient across the screen. */
    public static int separatorColor(int x, int screenWidth) {
        return IonBrand.gradientAt(screenWidth <= 1 ? 0f : x / (float) (screenWidth - 1));
    }
}
