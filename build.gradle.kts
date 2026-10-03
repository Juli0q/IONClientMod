plugins {
    id("xyz.wagyourtail.unimined") version "1.4.1" apply false
}

allprojects {
    group = property("mod_group") as String
    version = property("mod_version") as String

    repositories {
        mavenCentral()
        maven("https://maven.wagyourtail.xyz/releases")
        maven("https://repo.spongepowered.org/maven")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases")
        maven("https://libraries.minecraft.net/")
    }
}

subprojects {
    apply(plugin = "java")

    // Shared, loader-agnostic code (brand constants, colours) is compiled into every mod target.
    // The LWJGL3 runtime is not one: it must not carry ION classes.
    if (name != "mc-1.8.9-lwjgl3") {
        extensions.configure<JavaPluginExtension> {
            sourceSets.named("main") {
                java.srcDir(rootProject.file("common/src/main/java"))
                resources.srcDir(rootProject.file("common/src/main/resources"))
            }
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}

// `./gradlew buildAll` produces every jar: Forge 1.8.9 from this build, Fabric/NeoForge/Forge 1.21.1
// from the included build. The jars are collected into build/libs at the repository root.
val collectJars by tasks.registering(Sync::class) {
    val included = gradle.includedBuild("mc-1.21.1")
    dependsOn(":mc-1.8.9:build", ":mc-1.8.9-lwjgl3:build")
    listOf("fabric", "neoforge", "forge").forEach { dependsOn(included.task(":$it:build")) }

    from(project(":mc-1.8.9").layout.buildDirectory.dir("libs"))
    from(project(":mc-1.8.9-lwjgl3").layout.buildDirectory.dir("libs"))
    listOf("fabric", "neoforge", "forge").forEach { from(included.projectDir.resolve("$it/build/libs")) }
    listOf("fabric", "neoforge", "forge").forEach { include("*-${project.version}-$it-*.jar") }
    exclude("*-dev.jar", "*-sources.jar")
    into(layout.buildDirectory.dir("libs"))
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds the mod for every supported Minecraft version and loader."
    dependsOn(collectJars)
}
