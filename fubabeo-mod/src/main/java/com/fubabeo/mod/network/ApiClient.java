package com.fubabeo.mod.network;

import com.fubabeo.mod.data.PlayerIdManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("Fubabeo");
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(60))
            .build();
            
    private static long pauseUntil = 0;

    private static void sendPostRequest(String endpoint, JsonObject payload, int retryCount) {
        if (System.currentTimeMillis() < pauseUntil) return;

        if (retryCount > 3) {
            pauseUntil = System.currentTimeMillis() + 60000; // 60 seconds
            return;
        }

        payload.addProperty("uuid", PlayerIdManager.getUuid());
        payload.addProperty("ign", PlayerIdManager.getIgn());
        
        if (!payload.has("serverIp")) {
            payload.addProperty("serverIp", HeartbeatManager.currentServerIp != null ? HeartbeatManager.currentServerIp : "Unknown");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.API_URL + endpoint))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("X-API-Key", ApiConfig.API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    try {
                        if (response.statusCode() >= 400) {
                            LOGGER.error("[API Error] " + endpoint + " returned " + response.statusCode() + ": " + response.body());
                        } else {
                            LOGGER.info("[API Success] " + endpoint + ": " + response.body());
                        }
                        JsonObject resJson = com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();
                        if (resJson.has("data") && !resJson.get("data").isJsonNull()) {
                            JsonObject data = resJson.getAsJsonObject("data");
                            if (data.has("isLiveTracking") || data.has("is_live_tracking")) {
                                boolean isLive = data.has("isLiveTracking") ? data.get("isLiveTracking").getAsBoolean() : data.get("is_live_tracking").getAsBoolean();
                                if (ApiConfig.LIVE_TRACKING_ENABLED != isLive) {
                                    ApiConfig.LIVE_TRACKING_ENABLED = isLive;
                                    ApiConfig.saveConfig();
                                }
                            }
                        }
                    } catch (Exception e) {
                        // ignore parsing errors
                    }
                })
                .exceptionally(e -> {
                    LOGGER.error("[API Exception] " + endpoint + ": " + e.getMessage(), e);
                    int backoffMs = (int) Math.pow(2, retryCount) * 1000;
                    CompletableFuture.delayedExecutor(backoffMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                            .execute(() -> sendPostRequest(endpoint, payload, retryCount + 1));
                    return null;
                });
    }

    public static void sendLogin(String modVersion) {
        JsonObject payload = new JsonObject();
        payload.addProperty("modVersion", modVersion);
        sendPostRequest("/api/player/login", payload, 0);
    }

    public static void sendHeartbeat(double x, double y, double z, String dimension) {
        JsonObject payload = new JsonObject();
        payload.addProperty("x", x);
        payload.addProperty("y", y);
        payload.addProperty("z", z);
        payload.addProperty("dimension", dimension);
        sendPostRequest("/api/heartbeat", payload, 0);
    }

    public static void sendBlockPlaceEvent(String blockType, double x, double y, double z, String dimension) {
        JsonObject payload = new JsonObject();
        payload.addProperty("blockType", blockType);
        payload.addProperty("x", x);
        payload.addProperty("y", y);
        payload.addProperty("z", z);
        payload.addProperty("dimension", dimension);
        sendPostRequest("/api/event/block_place", payload, 0);
    }

    public static void sendItemDropEvent(String itemId, int count, double x, double y, double z, String dimension) {
        JsonObject payload = new JsonObject();
        payload.addProperty("itemId", itemId);
        payload.addProperty("count", count);
        payload.addProperty("x", x);
        payload.addProperty("y", y);
        payload.addProperty("z", z);
        payload.addProperty("dimension", dimension);
        sendPostRequest("/api/event/item_drop", payload, 0);
    }
    
    public static void sendProximityEvent(String eventType, double x, double y, double z, String dimension, Set<String> nearbyPlayers, String closestPlayer) {
        JsonObject payload = new JsonObject();
        payload.addProperty("eventType", eventType);
        payload.addProperty("x", x);
        payload.addProperty("y", y);
        payload.addProperty("z", z);
        payload.addProperty("dimension", dimension);
        
        JsonArray playersArray = new JsonArray();
        for (String p : nearbyPlayers) {
            playersArray.add(new JsonPrimitive(p));
        }
        payload.add("nearbyPlayers", playersArray);
        payload.addProperty("closestPlayer", closestPlayer);
        
        sendPostRequest("/api/proximity/scan", payload, 0);
    }
}
