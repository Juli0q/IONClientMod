
plugins {
    java
    id("xyz.wagyourtail.unimined")
}

val modId: String by rootProject.extra { property("mod_id") as String }
val mcVersion = "1.8.9"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

base {
    archivesName.set(modId)
}

// `main` holds the loader-agnostic 1.8.9 code (MCP names): the pinned entry, its renderer and
// the mixins. The forge source set adds the @Mod entrypoint, mcmod.info and the Mixin bootstrap.
sourceSets {
    create("forge")
}

unimined.minecraft(sourceSets.main.get()) {
    version(mcVersion)
    side("client")

    mappings {
        searge()
        mcp(property("mc189_mcp_channel") as String, property("mc189_mcp_version") as String)
    }

    defaultRemapJar = false
}

unimined.minecraft(sourceSets["forge"]) {
    combineWith(sourceSets.main.get())

    minecraftForge {
        loader(property("mc189_forge_version") as String)
        mixinConfig("$modId.mixins.json")
    }

    runs {
        config("client") {
            // Forge 1.8.9 has no Mixin of its own: in the dev run the tweaker is handed to
            // LaunchWrapper explicitly, in the shipped jar it comes from the manifest below.
            args("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
    }

    defaultRemapJar = true
}

// Mixin runtime bundled into the Forge jar. 0.7.11 is the build the 1.8.9 ecosystem has
// standardised on (Essential, Skytils, SkyHanni all ship it), so co-existing with those mods
// is a known-good configuration. Compilation uses the 0.8.5 API, which is a superset.
val forgeShade: Configuration by configurations.creating {
    isTransitive = false
}

dependencies {
    implementation("org.spongepowered:mixin:0.8.5")
    forgeShade("org.spongepowered:mixin:0.7.11-SNAPSHOT")
    // The dev run needs the same Mixin build the shipped jar bundles: 0.8.x expects ASM 9, which
    // Minecraft 1.8.9's LaunchWrapper classpath (ASM 5) does not have.
    "forgeRuntimeOnly"("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
}

tasks.jar {
    enabled = false
}

tasks.named<Jar>("forgeJar") {
    archiveClassifier.set("forge-$mcVersion-dev")
    from(forgeShade.map { zipTree(it) }) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA", "module-info.class")
    }
    manifest {
        attributes(
            "ModSide" to "CLIENT",
            "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
            "TweakOrder" to "0",
            "ForceLoadAsMod" to "true",
            "FMLCorePluginContainsFMLMod" to "true",
            "MixinConfigs" to "$modId.mixins.json",
        )
    }
}

tasks.named<Jar>("remapForgeJar") {
    archiveClassifier.set("forge-$mcVersion")
}

val expandProps = mapOf(
    "version" to project.version.toString(),
    "mod_id" to modId,
    "mod_name" to property("mod_name"),
    "mod_description" to property("mod_description"),
    "mod_author" to property("mod_author"),
    "mod_homepage" to property("mod_homepage"),
)

tasks.withType<ProcessResources>().configureEach {
    inputs.properties(expandProps)
    filesMatching(listOf("mcmod.info")) {
        expand(expandProps)
    }
}

// Forge 1.8.9 reads the version from the @Mod annotation, which cannot be expanded at build time,
// so the constant in IonClient.java has to match gradle.properties. Fail loudly if it drifts.
tasks.named("compileJava") {
    doFirst {
        val source = file("src/main/java/de/ionnetwork/client/IonClient.java").readText()
        val expected = "VERSION = \"${project.version}\""
        check(source.contains(expected)) { "IonClient.VERSION does not match mod_version=${project.version} in gradle.properties" }
    }
}
