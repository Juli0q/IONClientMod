package de.ionnetwork.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/** Shared entry point for the 1.8.9 targets. */
public final class IonClient {

    public static final String MOD_ID = "ionclient";
    /** Kept in step with mod_version in gradle.properties; the build checks the two agree. */
    public static final String VERSION = "1.1.0";
    public static final Logger LOGGER = LogManager.getLogger("IONClient");

    private IonClient() {
    }

    public static void init(String loader, File configDir) {
        try {
            IonSettings.load(configDir);
        } catch (IOException e) {
            LOGGER.warn("Could not read the ION Client settings; using the defaults", e);
        }
        IonSteamKeyboard.setLog(LOGGER::info);
        IonSteamKeyboard.logEnvironment();
        LOGGER.info("ION Client loaded on {} (Minecraft 1.8.9). Pinning {} to the server list.", loader, IonBrand.SERVER_ADDRESS);
    }

}
