package com.fubabeo.mod.client;

import com.fubabeo.mod.client.command.ModCommands;
import com.fubabeo.mod.client.hud.CompassHudOverlay;
import com.fubabeo.mod.network.ApiClient;
import com.fubabeo.mod.network.HeartbeatManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class DeathCompassClient implements ClientModInitializer {
    public static boolean isAfk = false;
    private static int afkTicks = 0;
    private static boolean afkMovingForward = true;
    private static final int TICKS_PER_STEP = 10; // 0.5s per direction (approx 1 block)

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
            isAfk = false; // Turn off AFK when disconnecting
        });

        // AFK Logic TICK
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (isAfk && client.player != null) {
                afkTicks++;
                if (afkTicks >= TICKS_PER_STEP) {
                    afkTicks = 0;
                    afkMovingForward = !afkMovingForward; // Switch direction
                }

                // Simulate key presses
                if (afkMovingForward) {
                    client.options.forwardKey.setPressed(true);
                    client.options.backKey.setPressed(false);
                } else {
                    client.options.forwardKey.setPressed(false);
                    client.options.backKey.setPressed(true);
                }
            } else {
                // If AFK is off, ensure we aren't holding the keys down
                // (Only release them once when turning off, but this simple check works safely if we don't interfere with real key presses otherwise)
                // Actually, if we just do nothing, the user can press keys normally. 
                // We'll release keys in the command itself when `/afk off` is triggered.
            }
        });
    }
}
