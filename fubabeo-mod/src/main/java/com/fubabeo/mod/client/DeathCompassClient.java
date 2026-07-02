package com.fubabeo.mod.client;

import com.fubabeo.mod.client.command.ModCommands;
import com.fubabeo.mod.client.hud.CompassHudOverlay;
import com.fubabeo.mod.network.ApiClient;
import com.fubabeo.mod.network.HeartbeatManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class DeathCompassClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register commands
        ModCommands.register();

        // Register HUD Overlay
        HudRenderCallback.EVENT.register(new CompassHudOverlay());

        // Handle joining a server/world
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String serverIp = "Singleplayer";
            if (client.getCurrentServerEntry() != null) {
                serverIp = client.getCurrentServerEntry().address;
            }
            
            // Send login event
            ApiClient.sendLogin("1.0.0"); // Current mod version
            
            // Start heartbeat sync
            HeartbeatManager.start(serverIp);
        });

        // Handle disconnecting
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            HeartbeatManager.stop();
        });
    }
}
