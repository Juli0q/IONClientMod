package org.lwjglx.opengl;

import org.lwjglx.LWJGLException;

/** Type-only stand-in for LWJGL2's SharedDrawable; see {@link Drawable}. Shared contexts are not emulated. */
public class SharedDrawable implements Drawable {
    public SharedDrawable(Drawable drawable) throws LWJGLException {
        throw new LWJGLException("Shared OpenGL contexts are not supported on LWJGL3");
    }

    @Override
    public boolean isCurrent() {
        return false;
    }

    @Override
    public void makeCurrent() throws LWJGLException {
        throw new LWJGLException("Shared OpenGL contexts are not supported on LWJGL3");
    }

    @Override
    public void releaseContext() {
    }

    @Override
    public void destroy() {
    }
}
