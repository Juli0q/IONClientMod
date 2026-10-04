package de.ionnetwork.client.gui;

import java.lang.reflect.Method;

/**
 * The desktop's scale factor (pixels per logical window unit), e.g. 1.45 on a scaled Wayland output.
 *
 * Plain LWJGL2 always renders at logical size, so this is 1 there. Only the LWJGL3 compatibility layer
 * renders at full framebuffer resolution and reports the factor through {@code org.lwjglx.opengl.Display}.
 * That layer ships in this jar but only runs when its coremod activated (it then sets
 * {@code lllllwjgl3.detectedBackend}); otherwise its classes are not touched at all.
 */
public final class IonDisplayScale {
    private static final Method SCALE_X = find();

    private IonDisplayScale() {
    }

    public static double get() {
        if (SCALE_X == null) return 1.0;
        try {
            double scale = (Double) SCALE_X.invoke(null);
            return scale > 0 ? scale : 1.0;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 1.0;
        }
    }

    private static Method find() {
        if (System.getProperty("lllllwjgl3.detectedBackend") == null) return null;
        try {
            return Class.forName("org.lwjglx.opengl.Display").getMethod("getScaleX");
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }
}
