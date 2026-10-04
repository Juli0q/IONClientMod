package de.ionnetwork.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/** Shared entry point for the 1.21.1 targets. */
public final class IonClient {

    public static final String MOD_ID = "ionclient";
    /** Kept in step with mod_version in gradle.properties; the build checks the two agree. */
    public static final String VERSION = "1.1.0";
    public static final Logger LOGGER = LoggerFactory.getLogger("IONClient");

    private IonClient() {
    }

    public static void init(String loader, File configDir) {
        try {
            IonSettings.load(configDir);
        } catch (IOException e) {
            LOGGER.warn("Could not read the ION Client settings; using the defaults", e);
        }
        LOGGER.info("ION Client loaded on {} (Minecraft 1.21.1). Pinning {} to the server list.", loader, IonBrand.SERVER_ADDRESS);
    }
}
