package com.fubabeo.mod.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartbeatManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Fubabeo");
    private static Timer timer;
    public static String currentServerIp = "Singleplayer";

    public static void start(String serverIp) {
        currentServerIp = serverIp;
        stop(); // Ensure old timer is stopped

        // Load config to get interval
        ApiConfig.loadConfig();
        
        LOGGER.info("Starting HeartbeatManager... Targeting API: " + ApiConfig.API_URL);

        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null && client.player != null) {
                        ClientPlayerEntity player = client.player;
                        double x = player.getX();
                        double y = player.getY();
                        double z = player.getZ();
                        String dimension = player.getWorld().getRegistryKey().getValue().toString();

                        ApiClient.sendHeartbeat(x, y, z, dimension);
                    }
                } catch (Exception e) {
                    LOGGER.error("HeartbeatManager Exception", e);
                }
            }
        }, 2000, ApiConfig.HEARTBEAT_INTERVAL * 1000L);
    }

    public static void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
