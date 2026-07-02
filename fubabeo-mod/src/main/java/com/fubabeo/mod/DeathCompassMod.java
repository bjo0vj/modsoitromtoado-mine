package com.fubabeo.mod;

import com.fubabeo.mod.network.ApiConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeathCompassMod implements ModInitializer {
    public static final String MOD_ID = "fubabeo";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Load API config
        ApiConfig.loadConfig();
        
        LOGGER.info("fubabeo.mod initialized.");
    }
}
