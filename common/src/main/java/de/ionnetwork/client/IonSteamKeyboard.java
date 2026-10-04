package de.ionnetwork.client;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Steam's on-screen keyboard, for playing on a Steam Deck (or with a controller in Big Picture).
 *
 * <p>Minecraft is not a Steam game, so the Steamworks text-input API is out of reach: it needs an
 * App ID. Steam's {@code steam://open/keyboard} and {@code steam://close/keyboard} links need none,
 * work from Prism's Flatpak (through the desktop portal's "open link" request) and are harmless to
 * repeat: opening an open keyboard or closing a closed one does nothing. The keyboard types into
 * the focused window like a real one, so text fields need no changes.
 *
 * <p>The versions call {@link #open} when the player clicks a text field or opens a screen made for
 * typing (chat, a sign, a book), and {@link #screenChanged} whenever the screen changes, which closes
 * the keyboard again if this opened it. Links run on one background thread, in order, so the game
 * never waits for {@code xdg-open}.
 *
 * <p>Pure Java 8, no Minecraft imports.
 */
public final class IonSteamKeyboard {

    /** {@link IonSettings#steamKeyboard()} value: on only in Steam's Game Mode or Big Picture. */
    public static final String AUTO = "auto";
    public static final String ON = "on";
    public static final String OFF = "off";

    static final String OPEN_URL = "steam://open/keyboard";
    static final String CLOSE_URL = "steam://close/keyboard";
    /** A click on an already focused field opens again; that is harmless, but not worth a process. */
    private static final long REOPEN_MILLIS = 1000L;
    /**
     * Turns the chat lift on outside Game Mode, where the keyboard floats in its own window instead
     * of docking, to try it on a desktop: {@code -Dionclient.steamKeyboard.lift=true}.
     */
    private static final String LIFT_PROPERTY = "ionclient.steamKeyboard.lift";

    /** Where diagnostics go; each version points it at its own logger. */
    public interface Log {
        void info(String message);
    }

    private static volatile Log log = message -> { };
    private static ExecutorService runner;
    private static volatile boolean opened;
    private static volatile long openedAt;

    private IonSteamKeyboard() {
    }

    public static void setLog(Log target) {
        log = target;
    }

    /** Logs what the auto mode sees, once at startup, so one log file explains a Deck that does nothing. */
    public static void logEnvironment() {
        Map<String, String> env = System.getenv();
        log.info(String.format(Locale.ROOT,
                "Steam keyboard: setting %s, %s (SteamDeck=%s SteamGamepadUI=%s SteamOS=%s gamescope=%s flatpak=%s)",
                IonSettings.steamKeyboard(), active() ? "active" : "inactive",
                env.get("SteamDeck"), env.get("SteamGamepadUI"), env.get("SteamOS"),
                env.containsKey("GAMESCOPE_WAYLAND_DISPLAY"), env.containsKey("FLATPAK_ID")));
    }

    /** Whether the keyboard should pop up at all, by the setting and, for "auto", the environment. */
    public static boolean active() {
        return isActive(IonSettings.steamKeyboard(), System.getenv());
    }

    static boolean isActive(String setting, Map<String, String> env) {
        if (ON.equals(setting)) return true;
        if (OFF.equals(setting)) return false;
        // "on" alone is not enough outside Game Mode, because a steam:// link starts Steam when it
        // is not running.
        return isGameMode(env);
    }

    /** Steam sets these for everything it starts from Game Mode (and Big Picture). */
    static boolean isGameMode(Map<String, String> env) {
        return "1".equals(env.get("SteamDeck")) || "1".equals(env.get("SteamGamepadUI"));
    }

    /**
     * How far the chat screen should be drawn above its place, in GUI pixels of a screen {@code
     * screenHeight} high: 0 unless this opened the keyboard and it is docked over the bottom of the
     * screen, otherwise {@link IonSettings#chatLift()} percent of the height. The versions move the
     * drawing up by this and the mouse down by it, so the chat behaves as if the screen ended at the
     * keyboard.
     */
    public static int chatLift(int screenHeight) {
        if (!opened || !active()) return 0;
        return lift(IonSettings.chatLift(), System.getProperty(LIFT_PROPERTY), System.getenv(), screenHeight);
    }

    static int lift(int percent, String property, Map<String, String> env, int screenHeight) {
        if (property == null && !isGameMode(env)) return 0;
        return screenHeight * Math.max(0, Math.min(90, percent)) / 100;
    }

    /** The player is about to type: show the keyboard. */
    public static void open() {
        if (!active()) return;
        long now = System.currentTimeMillis();
        if (opened && now - openedAt < REOPEN_MILLIS) return;
        opened = true;
        openedAt = now;
        send(OPEN_URL);
    }

    /** The screen changed: hide the keyboard if this opened it. A typing screen reopens it in its init. */
    public static void screenChanged() {
        if (!opened) return;
        opened = false;
        send(CLOSE_URL);
    }

    private static synchronized void send(String url) {
        if (runner == null) {
            runner = Executors.newSingleThreadExecutor(task -> {
                Thread thread = new Thread(task, "ION Steam keyboard");
                thread.setDaemon(true);
                return thread;
            });
        }
        runner.execute(() -> run(url));
    }

    private static void run(String url) {
        String[] command = command(System.getProperty("os.name", ""), url);
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            process.getOutputStream().close();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                // The portal can wait on an "open with" dialog; leave it to the player.
                log.info("Steam keyboard: " + command[0] + " " + url + " is still waiting (a chooser dialog?)");
                return;
            }
            int exit = process.exitValue();
            if (exit != 0) {
                log.info("Steam keyboard: " + command[0] + " " + url + " exited with " + exit);
            }
        } catch (Exception e) {
            log.info("Steam keyboard: cannot run " + command[0] + ": " + e);
        }
    }

    static String[] command(String osName, String url) {
        String os = osName.toLowerCase(Locale.ROOT);
        if (os.contains("win")) return new String[] {"rundll32", "url.dll,FileProtocolHandler", url};
        if (os.contains("mac")) return new String[] {"open", url};
        return new String[] {"xdg-open", url};
    }
}
