import java.util.Properties

val rootProps = Properties().apply {
    rootDir.resolve("../gradle.properties").inputStream().use { load(it) }
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${rootProps.getProperty("mc1211_fabric_loader")}")
}
