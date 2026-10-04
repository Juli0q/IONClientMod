import java.util.zip.ZipFile

// LWJGL3 runtime for Forge 1.8.9: an FML coremod that swaps the game's LWJGL2 window, input and
// audio for LWJGL3/GLFW (native Wayland, real pointer lock, HiDPI). Vendored from LLLLLwjgl3, see
// THIRD_PARTY_NOTICES.md. It references no Minecraft classes, only FML, LaunchWrapper and ASM, so it
// is a plain Java project compiled against the Forge universal jar instead of a unimined build.
//
// Its jar is not shipped on its own: mc-1.8.9 merges it into the ION Client jar, whose bundled Mixin
// loads the coremod from the FMLCorePlugin manifest entry. It only activates on Linux Wayland.
plugins {
    java
}

val modId: String by rootProject.extra { property("mod_id") as String }
val mcVersion = "1.8.9"
val lwjglVersion = "3.3.3"
val lwjglModules = listOf("lwjgl", "lwjgl-glfw", "lwjgl-openal", "lwjgl-opengl")
// The coremod only activates on Linux, so only those natives are bundled.
val lwjglNatives = listOf("natives-linux")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

base {
    archivesName.set("$modId-lwjgl3-bundle")
}

// LWJGL3 and its natives are unpacked into the jar: the coremod puts this jar ahead of the game's
// LWJGL2 on LaunchWrapper's classpath, so everything it needs has to come from this one file.
val embed: Configuration by configurations.creating {
    isTransitive = false
}

// What the game provides at runtime (Forge 1.8.9's LaunchWrapper classpath).
val gameRuntime: Configuration by configurations.creating {
    isTransitive = false
}

configurations {
    compileOnly { extendsFrom(gameRuntime) }
    testImplementation { extendsFrom(gameRuntime) }
    implementation { extendsFrom(embed) }
}

dependencies {
    gameRuntime("net.minecraftforge:forge:${property("mc189_forge_version").let { "$mcVersion-$it" }}:universal")
    gameRuntime("net.minecraft:launchwrapper:1.12")
    // asm-debug-all keeps the generic signatures that asm-all strips; the classes are otherwise identical.
    gameRuntime("org.ow2.asm:asm-debug-all:5.0.3")
    gameRuntime("com.google.guava:guava:17.0")
    gameRuntime("org.apache.logging.log4j:log4j-api:2.0-beta9")

    lwjglModules.forEach { module ->
        embed("org.lwjgl:$module:$lwjglVersion")
        lwjglNatives.forEach { embed("org.lwjgl:$module:$lwjglVersion:$it") }
    }

    testImplementation("junit:junit:4.13.2")
    // Only the manual WindowCompatibilitySmoke decodes an icon with STB; it is not bundled.
    testImplementation("org.lwjgl:lwjgl-stb:$lwjglVersion")
}

tasks.jar {
    from({ embed.map { zipTree(it) } }) {
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/versions/**", "META-INF/INDEX.LIST")
        exclude("META-INF/MANIFEST.MF", "module-info.class")
        // Checksums of the Windows/macOS natives, which are not bundled.
        exclude("META-INF/windows/**", "META-INF/macos/**")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // The upstream LWJGL3 jars carry no license files; ship every applicable text.
    from("THIRD_PARTY_NOTICES.md") { into("META-INF/lwjgl3") }
    from("licenses") { into("META-INF/lwjgl3/licenses") }
}

// The jar has to win over vanilla LWJGL2 and must not leak LWJGL2 classes of its own.
val verifyJar by tasks.registering {
    dependsOn(tasks.jar)
    val jarFile = tasks.jar.flatMap { it.archiveFile }
    doLast {
        ZipFile(jarFile.get().asFile).use { zip ->
            listOf(
                "com/lllllwjgl3/boot/Lwjgl3Coremod.class",
                "org/lwjglx/opengl/Display.class",
                "org/lwjglx/openal/AL10.class",
                "org/lwjgl/glfw/GLFW.class",
                "linux/x64/org/lwjgl/glfw/libglfw.so",
                "META-INF/lwjgl3/THIRD_PARTY_NOTICES.md",
            ).forEach { check(zip.getEntry(it) != null) { "Missing $it in ${zip.name}" } }
            val leaked = zip.entries().asSequence().map { it.name }.filter {
                it.startsWith("org/lwjgl/input/") || it == "org/lwjgl/opengl/Display.class" ||
                    it == "org/lwjgl/LWJGLException.class" || it == "org/lwjgl/Sys.class" ||
                    it.startsWith("de/ionnetwork/") || it == "mcmod.info"
            }.toList()
            check(leaked.isEmpty()) { "Unexpected classes in ${zip.name}: $leaked" }
        }
    }
}

tasks.test {
    dependsOn(tasks.jar)
    systemProperty("lllllwjgl3.testJar", tasks.jar.get().archiveFile.get().asFile.absolutePath)
}

tasks.build {
    dependsOn(verifyJar)
}
