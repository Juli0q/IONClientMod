// The 1.21.1 targets are their own Gradle build (included from the repository root) because they use
// Architectury Loom, whose buildscript classpath must not be mixed with unimined's (used for 1.8.9).
pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ionclient-1.21.1"

include("fabric", "neoforge", "forge")
