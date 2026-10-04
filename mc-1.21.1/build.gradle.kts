import java.util.Properties

plugins {
    id("dev.architectury.loom") version "1.11.454" apply false
}

// The repository root's gradle.properties is the single source of truth for mod id, version and
// loader versions; this build is included from there, so read it directly.
val rootProps = Properties().apply {
    rootDir.resolve("../gradle.properties").inputStream().use { load(it) }
}

fun prop(name: String): String = rootProps.getProperty(name) ?: error("missing property $name")

val mcVersion = "1.21.1"
val modId = prop("mod_id")

subprojects {
    apply(plugin = "java")
    apply(plugin = "dev.architectury.loom")

    group = prop("mod_group")
    version = prop("mod_version")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        // Every loader compiles the same 1.21.1 sources (../src: the pinned entry, its renderer and
        // the mixins) plus the version-agnostic brand constants (../../common).
        sourceSets.named("main") {
            java.srcDir(rootDir.resolve("src/main/java"))
            resources.srcDir(rootDir.resolve("src/main/resources"))
            java.srcDir(rootDir.resolve("../common/src/main/java"))
            resources.srcDir(rootDir.resolve("../common/src/main/resources"))
        }
    }

    extensions.configure<BasePluginExtension> {
        archivesName.set(modId)
    }

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
    }

    val loom = extensions.getByType<net.fabricmc.loom.api.LoomGradleExtensionAPI>()

    dependencies {
        "minecraft"("com.mojang:minecraft:$mcVersion")
        "mappings"(loom.officialMojangMappings())
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    // The client announces its version to ION's servers from IonClient.VERSION, which cannot be
    // expanded at build time. Fail loudly if it drifts from gradle.properties.
    tasks.named("compileJava") {
        doFirst {
            val source = rootProject.file("src/main/java/de/ionnetwork/client/IonClient.java").readText()
            val expected = "VERSION = \"${prop("mod_version")}\""
            check(source.contains(expected)) { "IonClient.VERSION does not match mod_version=${prop("mod_version")} in gradle.properties" }
        }
    }


    val expandProps = mapOf(
        "version" to prop("mod_version"),
        "mod_id" to modId,
        "mod_name" to prop("mod_name"),
        "mod_description" to prop("mod_description"),
        "mod_author" to prop("mod_author"),
        "mod_homepage" to prop("mod_homepage"),
    )

    tasks.withType<ProcessResources>().configureEach {
        inputs.properties(expandProps)
        filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
            expand(expandProps)
        }
    }

    // One jar per loader, named like ionclient-1.0.0-fabric-1.21.1.jar.
    tasks.named<Jar>("jar") {
        archiveClassifier.set("${project.name}-$mcVersion-dev")
    }
    tasks.withType<AbstractArchiveTask>().matching { it.name == "remapJar" }.configureEach {
        archiveClassifier.set("${project.name}-$mcVersion")
    }
}
