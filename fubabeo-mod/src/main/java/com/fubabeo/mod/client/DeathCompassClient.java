package com.fubabeo.mod.client;

import com.fubabeo.mod.network.ApiClient;
import com.fubabeo.mod.network.ApiConfig;
import com.fubabeo.mod.network.HeartbeatManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.HashSet;
import java.util.Set;

public class DeathCompassClient implements ClientModInitializer {

    private static int joinTicks = 0;
    private static boolean joined = false;

    private static int liveTrackingTicks = 0;
    private static int proximityTicks = 0;
    
    // Proximity state
    private static Set<String> lastNearbyPlayers = new HashSet<>();
    private static int stableProximityTicks = 0;
    private static final int STABLE_REPORT_TICKS = 12000; // 10 minutes (20 ticks/sec * 60 sec * 10 = 12000)

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            joined = true;
            joinTicks = 0;
            
            String serverIp = "Singleplayer";
            if (client.getCurrentServerEntry() != null) {
                serverIp = client.getCurrentServerEntry().address;
            }
            HeartbeatManager.start(serverIp);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            joined = false;
            HeartbeatManager.stop();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!joined || client.player == null || client.world == null) return;

            // Delayed Login Event (2 seconds = 40 ticks)
            if (joinTicks < 40) {
                joinTicks++;
                if (joinTicks == 40) {
                    ApiClient.sendLogin("1.0.1-ServerMonitor");
                }
            }

            // Live Tracking (Chức năng 2)
            if (ApiConfig.LIVE_TRACKING_ENABLED) {
                liveTrackingTicks++;
                if (liveTrackingTicks >= 20) { // 1 second
                    liveTrackingTicks = 0;
                    ApiClient.sendHeartbeat(client.player.getX(), client.player.getY(), client.player.getZ(), client.player.getWorld().getRegistryKey().getValue().toString());
                }
            }

            // Proximity Scan (Chức năng 3)
            proximityTicks++;
            if (proximityTicks >= 40) { // 2 seconds
                proximityTicks = 0;
                
                Set<String> currentNearby = new HashSet<>();
                String closestPlayer = null;
                double closestDistSq = Double.MAX_VALUE;

                double px = client.player.getX();
                double py = client.player.getY();
                double pz = client.player.getZ();
                
                int r = ApiConfig.PROXIMITY_RADIUS;
                
                for (AbstractClientPlayerEntity p : client.world.getPlayers()) {
                    if (p != client.player) {
                        double dx = p.getX() - px;
                        double dy = p.getY() - py;
                        double dz = p.getZ() - pz;
                        if (Math.abs(dx) <= r && Math.abs(dy) <= r && Math.abs(dz) <= r) {
                            String name = p.getName().getString();
                            currentNearby.add(name);
                            double distSq = dx*dx + dy*dy + dz*dz;
                            if (distSq < closestDistSq) {
                                closestDistSq = distSq;
                                closestPlayer = name;
                            }
                        }
                    }
                }

                // Check for new players or changes
                boolean hasNew = false;
                for (String p : currentNearby) {
                    if (!lastNearbyPlayers.contains(p)) {
                        hasNew = true;
                        break;
                    }
                }

                if (hasNew || currentNearby.size() != lastNearbyPlayers.size()) { 
                    // Changed in proximity
                    if (hasNew) {
                        ApiClient.sendProximityEvent("NEARBY_CHANGED", px, py, pz, client.player.getWorld().getRegistryKey().getValue().toString(), currentNearby, closestPlayer);
                        stableProximityTicks = 0; // Reset counter
                    }
                }

                // If no new players, waiting for stable report
                stableProximityTicks += 40;
                if (stableProximityTicks >= STABLE_REPORT_TICKS) {
                    ApiClient.sendProximityEvent("NEARBY_STABLE_10M", px, py, pz, client.player.getWorld().getRegistryKey().getValue().toString(), currentNearby, closestPlayer);
                    stableProximityTicks = 0; // Reset counter
                }

                lastNearbyPlayers = currentNearby;
            }
        });
    }
}
