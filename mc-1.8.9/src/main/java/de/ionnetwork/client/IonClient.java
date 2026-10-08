package de.ionnetwork.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/** Shared entry point for the 1.8.9 targets. */
public final class IonClient {

    public static final String MOD_ID = "ionclient";
    /** Kept in step with mod_version in gradle.properties; the build checks the two agree. */
    public static final String VERSION = "1.2.0-beta.3";
    public static final Logger LOGGER = LogManager.getLogger("IONClient");

    private static boolean settingsLoaded;

    private IonClient() {
    }

    public static void init(String loader, File configDir) {
        loadSettings(configDir);
        IonSteamKeyboard.setLog(LOGGER::info);
        IonSteamKeyboard.logEnvironment();
        LOGGER.info("ION Client loaded on {} (Minecraft 1.8.9). Pinning {} to the server list.", loader, IonBrand.SERVER_ADDRESS);
    }

    /**
     * Reads the settings once. The modern texture pack needs them before Minecraft picks its
     * resource packs, which is before the mod's init, so whichever comes first reads them.
     */
    public static synchronized void loadSettings(File configDir) {
        if (settingsLoaded) {
            return;
        }
        settingsLoaded = true;
        try {
            IonSettings.load(configDir);
        } catch (IOException e) {
            LOGGER.warn("Could not read the ION Client settings; using the defaults", e);
        }
    }
}
