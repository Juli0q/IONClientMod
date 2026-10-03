package de.ionnetwork.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared entry point for the 1.21.1 targets. */
public final class IonClient {

    public static final String MOD_ID = "ionclient";
    public static final Logger LOGGER = LoggerFactory.getLogger("IONClient");

    private IonClient() {
    }

    public static void init(String loader) {
        LOGGER.info("ION Client loaded on {} (Minecraft 1.21.1). Pinning {} to the server list.", loader, IonBrand.SERVER_ADDRESS);
    }
}
