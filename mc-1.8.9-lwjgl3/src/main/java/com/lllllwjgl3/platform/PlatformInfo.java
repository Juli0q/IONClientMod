package com.lllllwjgl3.platform;

import java.util.Locale;
import java.util.Map;

/** Pure platform policy used by the early bootstrap and unit tests. */
public final class PlatformInfo {
    public enum OperatingSystem { WINDOWS, MACOS, LINUX, OTHER }
    public enum Backend { AUTO, WAYLAND, X11 }

    /**
     * ION Client build: native Wayland by default. Its pointer lock and HiDPI framebuffer are the reason
     * to use LWJGL3 at all; players who need a CJK input method opt into XWayland with
     * {@code -Dlllllwjgl3.waylandIme=xwayland}. Upstream defaults to "xwayland".
     */
    public static final String DEFAULT_WAYLAND_IME = "native";

    private PlatformInfo() {
    }

    public static OperatingSystem detectOperatingSystem(String osName) {
        String value = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (value.contains("win")) return OperatingSystem.WINDOWS;
        if (value.contains("mac") || value.contains("darwin")) return OperatingSystem.MACOS;
        if (value.contains("linux")) return OperatingSystem.LINUX;
        return OperatingSystem.OTHER;
    }

    public static boolean isWayland(Map<String, String> environment) {
        if (environment == null) return false;
        String session = environment.get("XDG_SESSION_TYPE");
        if (session != null && "wayland".equalsIgnoreCase(session.trim())) return true;
        String display = environment.get("WAYLAND_DISPLAY");
        return display != null && !display.trim().isEmpty();
    }

    /**
     * ION Client build: whether to replace LWJGL2 at all. Only Linux can: the jar bundles Linux natives
     * only, and macOS would also need -XstartOnFirstThread for GLFW. On Linux "true"/"false" force it;
     * anything else is auto, which only switches on Wayland sessions. That is where LWJGL2 breaks
     * (XWayland pointer warps, xrandr fullscreen, scaling); on X11 vanilla LWJGL2 is the lower-risk
     * choice for mod compatibility.
     */
    public static boolean shouldActivate(String requested, String osName, Map<String, String> environment) {
        if (detectOperatingSystem(osName) != OperatingSystem.LINUX) return false;
        String value = requested == null ? "" : requested.trim();
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        return isWayland(environment);
    }

    public static Backend parseBackend(String value) {
        if (value == null) return Backend.AUTO;
        if ("wayland".equalsIgnoreCase(value.trim())) return Backend.WAYLAND;
        if ("x11".equalsIgnoreCase(value.trim())) return Backend.X11;
        return Backend.AUTO;
    }

    public static Backend chooseBackend(String requested, String osName, Map<String, String> environment) {
        return chooseBackend(requested, osName, environment, true, DEFAULT_WAYLAND_IME);
    }

    /**
     * GLFW 3.x has no Wayland text-input-v3/XIM integration. With the "xwayland"
     * IME mode, AUTO therefore uses the XWayland surface when a Wayland session
     * exposes a DISPLAY socket. Fcitx5/IBus then talks to the GLFW X11 backend
     * through XIM and committed Unicode text reaches Keyboard's char callback.
     *
     * <p>X11 and Wayland only exist on Linux. GLFW on Windows and macOS has neither platform, and
     * hinting one there makes {@code glfwInit} fail, so every other OS stays on AUTO and lets GLFW
     * pick Win32 or Cocoa itself.
     */
    public static Backend chooseBackend(String requested, String osName, Map<String, String> environment,
            boolean ximEnabled, String waylandImeMode) {
        if (detectOperatingSystem(osName) != OperatingSystem.LINUX) return Backend.AUTO;
        Backend parsed = parseBackend(requested);
        if (parsed != Backend.AUTO) return parsed;
        if (!isWayland(environment)) return Backend.X11;
        if (ximEnabled && shouldUseXwaylandForIme(environment, waylandImeMode)) {
            return Backend.X11;
        }
        return Backend.WAYLAND;
    }

    public static boolean shouldUseXwaylandForIme(Map<String, String> environment,
            String waylandImeMode) {
        if (!isWayland(environment)) return false;
        if ("native".equalsIgnoreCase(waylandImeMode == null ? "" : waylandImeMode.trim())) {
            return false;
        }
        String display = environment.get("DISPLAY");
        return display != null && !display.trim().isEmpty();
    }

    public static boolean isXimEnabled(String value) {
        return value == null || !("0".equals(value.trim()) || "false".equalsIgnoreCase(value.trim()));
    }
}
