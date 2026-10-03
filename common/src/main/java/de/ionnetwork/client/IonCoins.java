package de.ionnetwork.client;

import java.util.UUID;

/**
 * The player's ION coin balance as shown on the main menu.
 *
 * <p>Version-agnostic state: the renderers only read {@link #state()} and {@link #balance()},
 * and call {@link #refreshIfStale} every frame, which is cheap and starts at most one fetch
 * every {@link #REFRESH_MILLIS}. The fetch itself is injected so the HTTP code can live here
 * too without this class knowing the endpoint.
 */
public final class IonCoins {

    /** How long a fetched balance is shown before it is refreshed. */
    public static final long REFRESH_MILLIS = 5L * 60L * 1000L;
    /** How long to wait after a failed fetch before trying again. */
    public static final long RETRY_MILLIS = 60L * 1000L;

    public enum State {
        /** Nothing fetched yet. */
        IDLE,
        /** A fetch is in flight and nothing is known yet. */
        LOADING,
        /** {@link #balance()} is valid. */
        READY,
        /** The API could not be reached, or the account has no ION profile. */
        UNAVAILABLE
    }

    /** Fetches a balance; returns null when the player has no ION profile. */
    public interface Fetcher {
        Long fetch(UUID player, String name) throws Exception;
    }

    /**
     * The player to look up. Normally the session's profile; a dev run can point it at a real
     * account with {@code IONCLIENT_DEV_UUID} / {@code IONCLIENT_DEV_NAME} in the environment
     * (or the {@code ionclient.devUuid} / {@code ionclient.devName} properties), since offline
     * dev sessions have no ION profile.
     */
    public static UUID playerUuid(UUID session) {
        String override = setting("IONCLIENT_DEV_UUID", "ionclient.devUuid");
        if (override != null) {
            try {
                return UUID.fromString(override.contains("-") ? override
                        : override.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5"));
            } catch (IllegalArgumentException ignored) {
                // fall through to the session
            }
        }
        return session;
    }

    public static String playerName(String session) {
        String override = setting("IONCLIENT_DEV_NAME", "ionclient.devName");
        return override != null ? override : session;
    }

    private static String setting(String env, String property) {
        String value = System.getProperty(property);
        if (value == null || value.isEmpty()) {
            value = System.getenv(env);
        }
        return value == null || value.isEmpty() ? null : value;
    }

    private static volatile State state = State.IDLE;
    private static volatile long balance;
    private static volatile long nextAttemptAt;
    private static volatile boolean inFlight;
    private static volatile UUID fetchedFor;

    private IonCoins() {
    }

    public static State state() {
        return state;
    }

    public static long balance() {
        return balance;
    }

    /**
     * Starts a background fetch when none is running and the last result is older than the
     * refresh interval (or the player changed). Safe to call every frame.
     */
    public static void refreshIfStale(UUID player, String name, Fetcher fetcher) {
        if (player == null || inFlight) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextAttemptAt && player.equals(fetchedFor)) {
            return;
        }
        inFlight = true;
        if (!player.equals(fetchedFor)) {
            state = State.LOADING;
        }
        Thread thread = new Thread(() -> {
            try {
                Long result = fetcher.fetch(player, name);
                if (result == null) {
                    state = State.UNAVAILABLE;
                } else {
                    balance = result;
                    state = State.READY;
                }
                nextAttemptAt = System.currentTimeMillis() + REFRESH_MILLIS;
            } catch (Exception e) {
                if (state != State.READY) {
                    state = State.UNAVAILABLE;
                }
                nextAttemptAt = System.currentTimeMillis() + RETRY_MILLIS;
            } finally {
                fetchedFor = player;
                inFlight = false;
            }
        }, "ION Coins");
        thread.setDaemon(true);
        thread.start();
    }

    /** {@code 1234567} as {@code 1,234,567}. */
    public static String format(long coins) {
        String digits = Long.toString(Math.abs(coins));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                out.append(',');
            }
            out.append(digits.charAt(i));
        }
        return coins < 0 ? "-" + out : out.toString();
    }
}
