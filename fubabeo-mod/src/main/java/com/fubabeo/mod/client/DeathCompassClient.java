package com.fubabeo.mod.client;

import com.fubabeo.mod.client.command.ModCommands;
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
    private static final int TICKS_PER_STEP = 10;
    private static boolean wasDead = false;

    // Navigation state
    public static boolean isNavigating = false;
    public static double targetX = 0;
    public static double targetZ = 0;
    public static String targetName = "Target";

    @Override
    public void onInitializeClient() {
        // Register commands
        ModCommands.register();

        // No HUD Overlay to avoid rendering crashes across minor versions

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
            if (client.player != null) {
                // Death detection (Health <= 1.0f which is 0.5 hearts)
                float health = client.player.getHealth();
                boolean isNearDeath = health <= 1.0f;
                if (isNearDeath && !wasDead) {
                    double x = client.player.getX();
                    double y = client.player.getY();
                    double z = client.player.getZ();
                    String dim = client.player.getWorld().getRegistryKey().getValue().toString();
                    
                    try {
                        com.fubabeo.mod.data.DeathDataManager.saveDeathPosition(x, y, z, dim);
                        ApiClient.sendDeathEvent(x, y, z, dim);
                    } catch (Exception e) {}
                }
                wasDead = isNearDeath;

                // AFK logic
                if (isAfk) {
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
                }

                // Particle Navigation Logic
                if (isNavigating && client.world != null) {
                    double dx = targetX - client.player.getX();
                    double dz = targetZ - client.player.getZ();
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    
                    if (distance > 2.0) {
                        dx /= distance;
                        dz /= distance;
                        
                        double startX = client.player.getX();
                        double startY = client.player.getY() + 0.1; // At the feet
                        double startZ = client.player.getZ();
                        
                        // Spawn a bright trail pointing to the target
                        if (client.player.age % 2 == 0) {
                            for (int i = 1; i <= 3; i++) {
                                double px = startX + dx * i;
                                double pz = startZ + dz * i;
                                client.world.addParticle(net.minecraft.particle.ParticleTypes.END_ROD, px, startY, pz, 0, 0, 0);
                            }
                        }
                    } else {
                        // Arrived indicator
                        if (client.player.age % 5 == 0) {
                            client.world.addParticle(net.minecraft.particle.ParticleTypes.HAPPY_VILLAGER, 
                                client.player.getX() + (Math.random() - 0.5) * 2, 
                                client.player.getY() + Math.random() * 2, 
                                client.player.getZ() + (Math.random() - 0.5) * 2, 
                                0, 0, 0);
                        }
                    }
                }
            } else {
                wasDead = false;
            }
        });
    }
}
