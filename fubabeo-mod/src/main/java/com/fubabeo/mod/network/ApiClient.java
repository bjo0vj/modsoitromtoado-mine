package com.fubabeo.mod.network;

import com.fubabeo.mod.data.PlayerIdManager;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class ApiClient {
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static void sendPostRequest(String endpoint, JsonObject payload, int retryCount) {
        if (retryCount > 3) return; // Max 3 retries

        // Always inject uuid and ign
        payload.addProperty("uuid", PlayerIdManager.getUuid());
        payload.addProperty("ign", PlayerIdManager.getIgn());
        
        // Add server IP if possible (for simplicity, we might leave it empty if unknown, or set it when joined)
        if (!payload.has("serverIp")) {
            payload.addProperty("serverIp", HeartbeatManager.currentServerIp);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.API_URL + endpoint))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("X-API-Key", ApiConfig.API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    // Success, no visible log
                })
                .exceptionally(e -> {
                    // Fail silently, retry after exponential backoff
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
        payload.addProperty("timestamp", System.currentTimeMillis());
        sendPostRequest("/api/heartbeat", payload, 0);
    }

    public static void sendDeathEvent(double x, double y, double z, String dimension) {
        JsonObject payload = new JsonObject();
        payload.addProperty("x", x);
        payload.addProperty("y", y);
        payload.addProperty("z", z);
        payload.addProperty("dimension", dimension);
        sendPostRequest("/api/event/death", payload, 0);
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

    public static void sendWaypointEvent(String name, double x, double y, double z, String dimension) {
        JsonObject payload = new JsonObject();
        payload.addProperty("name", name);
        payload.addProperty("x", x);
        payload.addProperty("y", y);
        payload.addProperty("z", z);
        payload.addProperty("dimension", dimension);
        sendPostRequest("/api/event/waypoint", payload, 0);
    }
}
