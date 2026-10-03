import java.util.Properties

val rootProps = Properties().apply {
    rootDir.resolve("../gradle.properties").inputStream().use { load(it) }
}

loom {
    forge {
        mixinConfig("${rootProps.getProperty("mod_id")}.mixins.json")
    }
}

dependencies {
    "forge"("net.minecraftforge:forge:1.21.1-${rootProps.getProperty("mc1211_forge_version")}")
}
