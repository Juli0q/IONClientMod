package com.lllllwjgl3.platform;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlatformInfoTest {
    @Test
    public void detectsWaylandFromSessionTypeOrSocket() {
        Map<String, String> session = new HashMap<String, String>();
        session.put("XDG_SESSION_TYPE", "Wayland");
        assertTrue(PlatformInfo.isWayland(session));

        Map<String, String> socket = new HashMap<String, String>();
        socket.put("WAYLAND_DISPLAY", "wayland-0");
        assertTrue(PlatformInfo.isWayland(socket));
    }

    @Test
    public void doesNotTreatX11AsWayland() {
        Map<String, String> env = new HashMap<String, String>();
        env.put("XDG_SESSION_TYPE", "x11");
        env.put("WAYLAND_DISPLAY", " ");
        assertFalse(PlatformInfo.isWayland(env));
        assertFalse(PlatformInfo.isWayland(Collections.<String, String>emptyMap()));
        assertFalse(PlatformInfo.isWayland(null));
    }

    @Test
    public void explicitBackendWinsOverEnvironment() {
        Map<String, String> env = new HashMap<String, String>();
        env.put("WAYLAND_DISPLAY", "wayland-0");
        env.put("DISPLAY", ":0");
        assertEquals(PlatformInfo.Backend.X11, PlatformInfo.chooseBackend("x11", "Linux", env));
        assertEquals(PlatformInfo.Backend.WAYLAND, PlatformInfo.chooseBackend(null, "Linux", env));
        assertEquals(PlatformInfo.Backend.X11,
                PlatformInfo.chooseBackend(null, "Linux", env, true, "xwayland"));
    }

    @Test
    public void onlyLinuxGetsAnExplicitGlfwPlatform() {
        // GLFW on Windows and macOS has no X11 or Wayland platform, so hinting one makes glfwInit fail.
        Map<String, String> env = new HashMap<String, String>();
        env.put("WAYLAND_DISPLAY", "wayland-0");
        assertEquals(PlatformInfo.Backend.AUTO,
                PlatformInfo.chooseBackend(null, "Windows 10", Collections.<String, String>emptyMap()));
        assertEquals(PlatformInfo.Backend.AUTO, PlatformInfo.chooseBackend("x11", "Windows 10", env));
        assertEquals(PlatformInfo.Backend.AUTO, PlatformInfo.chooseBackend(null, "Mac OS X", env));
        assertEquals(PlatformInfo.Backend.AUTO,
                PlatformInfo.chooseBackend(null, "Mac OS X", env, true, "xwayland"));
        assertEquals(PlatformInfo.Backend.X11,
                PlatformInfo.chooseBackend(null, "Linux", Collections.<String, String>emptyMap()));
        assertEquals(PlatformInfo.Backend.WAYLAND, PlatformInfo.chooseBackend(null, "Linux", env));
    }

    @Test
    public void waylandImeFallsBackToXwaylandWhenDisplayIsAvailable() {
        Map<String, String> env = new HashMap<String, String>();
        env.put("XDG_SESSION_TYPE", "wayland");
        env.put("WAYLAND_DISPLAY", "wayland-0");
        env.put("DISPLAY", ":0");
        assertTrue(PlatformInfo.shouldUseXwaylandForIme(env, "xwayland"));
        assertFalse(PlatformInfo.shouldUseXwaylandForIme(env, "native"));
        env.remove("DISPLAY");
        assertFalse(PlatformInfo.shouldUseXwaylandForIme(env, "xwayland"));
    }

    @Test
    public void autoActivatesOnlyOnLinuxWayland() {
        Map<String, String> wayland = new HashMap<String, String>();
        wayland.put("XDG_SESSION_TYPE", "wayland");
        wayland.put("WAYLAND_DISPLAY", "wayland-0");
        Map<String, String> x11 = new HashMap<String, String>();
        x11.put("XDG_SESSION_TYPE", "x11");
        x11.put("DISPLAY", ":0");

        assertTrue(PlatformInfo.shouldActivate(null, "Linux", wayland));
        assertTrue(PlatformInfo.shouldActivate("auto", "Linux", wayland));
        assertFalse(PlatformInfo.shouldActivate(null, "Linux", x11));
        assertFalse(PlatformInfo.shouldActivate(null, "Windows 11", wayland));
        assertFalse(PlatformInfo.shouldActivate(null, "Mac OS X", Collections.<String, String>emptyMap()));
    }

    @Test
    public void explicitSwitchOverridesDetection() {
        Map<String, String> wayland = new HashMap<String, String>();
        wayland.put("WAYLAND_DISPLAY", "wayland-0");
        Map<String, String> x11 = new HashMap<String, String>();
        x11.put("DISPLAY", ":0");
        assertFalse(PlatformInfo.shouldActivate("false", "Linux", wayland));
        assertTrue(PlatformInfo.shouldActivate("TRUE", "Linux", x11));
        // Only Linux natives are bundled, so forcing it elsewhere is ignored.
        assertFalse(PlatformInfo.shouldActivate("true", "Windows 10", Collections.<String, String>emptyMap()));
    }

    @Test
    public void ximIsEnabledByDefaultAndCanBeDisabled() {
        assertTrue(PlatformInfo.isXimEnabled(null));
        assertTrue(PlatformInfo.isXimEnabled("true"));
        assertFalse(PlatformInfo.isXimEnabled("0"));
        assertFalse(PlatformInfo.isXimEnabled("false"));
    }
}
