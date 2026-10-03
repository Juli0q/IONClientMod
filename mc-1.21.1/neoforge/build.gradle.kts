import java.util.Properties

val rootProps = Properties().apply {
    rootDir.resolve("../gradle.properties").inputStream().use { load(it) }
}

dependencies {
    "neoForge"("net.neoforged:neoforge:21.1.${rootProps.getProperty("mc1211_neoforge_version")}")
}
