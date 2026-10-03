pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.wagyourtail.xyz/releases")
        maven("https://maven.wagyourtail.xyz/snapshots")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "IONClientMod"

include("mc-1.8.9")

// Architectury Loom (1.21.1) and unimined (1.8.9) cannot share one buildscript classpath, so the
// 1.21.1 targets are a separate build composed into this one.
includeBuild("mc-1.21.1")
