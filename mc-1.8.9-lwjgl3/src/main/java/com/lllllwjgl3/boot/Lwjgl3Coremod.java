package com.lllllwjgl3.boot;

import java.net.URL;
import java.util.Map;

import com.lllllwjgl3.platform.Lwjgl3Platform;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

/** Forge early-loading entry point. */
@IFMLLoadingPlugin.MCVersion("1.8.9")
// FMLDeobfTweaker runs at 1000. Its member lookup keys include the original
// descriptors, so relocating LWJGL types before it breaks SRG field/method names.
@IFMLLoadingPlugin.SortingIndex(1001)
public final class Lwjgl3Coremod implements IFMLLoadingPlugin {
    public Lwjgl3Coremod() {
        // Inactive: touch nothing. LaunchWrapper keeps delegating org.lwjgl.* to the game's LWJGL2 and
        // no transformer is registered, so the jar behaves as if it were not installed.
        if (!Lwjgl3Platform.decideActive()) {
            System.out.println("[LLLLLwjgl3] inactive, keeping LWJGL2 (on Linux, -D"
                    + Lwjgl3Platform.ENABLED_PROPERTY + "=true forces LWJGL3)");
            return;
        }
        URL replacementSource = Lwjgl3Classpath.prioritizeCoremod(
                Launch.classLoader, Lwjgl3Coremod.class);
        // LaunchWrapper delegates org.lwjgl.* to the application loader by
        // default. That loader only has Minecraft's LWJGL2 jars, while the
        // replacement LWJGL3 classes live in this coremod's fat jar.
        detachVanillaLwjgl(Launch.classLoader);
        Lwjgl3Platform.detect();
        // GLFW must see the platform hint before the vendored Display class
        // initializes. This is intentionally done during coremod construction.
        GlfwInitHint.apply(Launch.classLoader, Lwjgl3Platform.getBackend().name());
        verifyRuntimeNamespace(Launch.classLoader, replacementSource);
        System.out.println("[LLLLLwjgl3] active: LWJGL3/GLFW backend=" + Lwjgl3Platform.getBackend()
                + ", IME=" + Lwjgl3Platform.isXimEnabled() + ", XWayland-XIM=" + Lwjgl3Platform.isXwaylandIme());
    }

    private static void verifyRuntimeNamespace(ClassLoader loader, URL replacementSource) {
        try {
            Class.forName("org.lwjglx.opengl.Display", false, loader);
            Class.forName("org.lwjglx.input.Keyboard", false, loader);
            Class<?> gl11 = Class.forName("org.lwjgl.opengl.GL11", false, loader);
            URL glSource = gl11.getProtectionDomain().getCodeSource().getLocation();
            if (!Lwjgl3Classpath.sameLocation(replacementSource, glSource)) {
                throw new IllegalStateException("LWJGL3 classpath replacement failed: org.lwjgl.opengl.GL11 "
                        + "was loaded from " + glSource + " instead of " + replacementSource);
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("LWJGL3 runtime or compatibility namespace is missing from the mod jar", e);
        }
    }

    @SuppressWarnings("unchecked")
    static void detachVanillaLwjgl(LaunchClassLoader loader) {
        try {
            java.lang.reflect.Field field = LaunchClassLoader.class.getDeclaredField("classLoaderExceptions");
            field.setAccessible(true);
            ((java.util.Set<String>) field.get(loader)).remove("org.lwjgl.");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to replace the vanilla LWJGL2 class loader boundary", e);
        }
    }

    @Override public String[] getASMTransformerClass() {
        if (!Lwjgl3Platform.isActive()) return new String[0];
        return new String[] { "com.lllllwjgl3.boot.Lwjgl3ClassTransformer" };
    }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) { }
    @Override public String getAccessTransformerClass() { return null; }
}
