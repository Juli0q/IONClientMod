package com.lllllwjgl3.platform;

import java.util.Locale;

/** Runtime platform state shared by the bytecode hook and the Forge mod. */
public final class Lwjgl3Platform {
    public static final String ENABLED_PROPERTY = "ionclient.lwjgl3";
    public static final String BACKEND_PROPERTY = "lllllwjgl3.backend";
    public static final String XIM_PROPERTY = "lllllwjgl3.xim";
    public static final String WAYLAND_IME_PROPERTY = "lllllwjgl3.waylandIme";
    private static volatile boolean active;
    private static volatile PlatformInfo.Backend backend = PlatformInfo.Backend.AUTO;
    private static volatile boolean xim = true;
    private static volatile boolean xwaylandIme;

    private Lwjgl3Platform() {
    }

    /** Decides once, during coremod construction, whether this run uses LWJGL3 at all. */
    public static boolean decideActive() {
        active = PlatformInfo.shouldActivate(System.getProperty(ENABLED_PROPERTY),
                System.getProperty("os.name"), System.getenv());
        return active;
    }

    public static boolean isActive() {
        return active;
    }

    public static void detect() {
        xim = PlatformInfo.isXimEnabled(System.getProperty(XIM_PROPERTY));
        String imeMode = System.getProperty(WAYLAND_IME_PROPERTY, PlatformInfo.DEFAULT_WAYLAND_IME);
        backend = PlatformInfo.chooseBackend(System.getProperty(BACKEND_PROPERTY),
                System.getenv(), xim, imeMode);
        xwaylandIme = PlatformInfo.isWayland(System.getenv()) && backend == PlatformInfo.Backend.X11
                && PlatformInfo.parseBackend(System.getProperty(BACKEND_PROPERTY)) == PlatformInfo.Backend.AUTO;
        System.setProperty("lllllwjgl3.detectedBackend", backend.name().toLowerCase(Locale.ROOT));
        System.setProperty("lllllwjgl3.detectedIme",
                xwaylandIme ? "xwayland-xim" : (xim ? "unicode-callback" : "disabled"));
    }

    public static PlatformInfo.Backend getBackend() {
        return backend;
    }

    public static boolean isXimEnabled() {
        return xim;
    }

    public static boolean isXwaylandIme() {
        return xwaylandIme;
    }
}
