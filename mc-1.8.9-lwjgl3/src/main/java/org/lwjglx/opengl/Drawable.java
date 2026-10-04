package org.lwjglx.opengl;

import org.lwjglx.LWJGLException;

/**
 * Type-only stand-in for LWJGL2's Drawable. Forge's loading screen declares fields of this type; the
 * screen itself is switched off (Lwjgl3ClassTransformer.disableForgeSplash), but the JVM still has to
 * resolve the type when it verifies SplashProgress. With LWJGL2 gone from the classpath (Java 21
 * profiles) nothing else provides it.
 */
public interface Drawable {
    boolean isCurrent() throws LWJGLException;

    void makeCurrent() throws LWJGLException;

    void releaseContext() throws LWJGLException;

    void destroy();
}
