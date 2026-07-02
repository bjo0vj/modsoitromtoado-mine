package com.fubabeo.mod.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ApiConfig {
    private static final Gson GSON = new Gson();
    public static String API_URL = "https://modsoitromtoado-mine.onrender.com";
    public static String API_KEY = "iufubabeobeo";
    public static int HEARTBEAT_INTERVAL = 300; // seconds

    public static void loadConfig() {
        File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "fubabeo");
        if (!configDir.exists()) configDir.mkdirs();

        File file = new File(configDir, "config.json");
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json.has("apiUrl")) API_URL = json.get("apiUrl").getAsString();
                if (json.has("apiKey")) API_KEY = json.get("apiKey").getAsString();
                if (json.has("heartbeatInterval")) HEARTBEAT_INTERVAL = json.get("heartbeatInterval").getAsInt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Create default
            try (FileWriter writer = new FileWriter(file)) {
                JsonObject json = new JsonObject();
                json.addProperty("apiUrl", API_URL);
                json.addProperty("apiKey", API_KEY);
                json.addProperty("heartbeatInterval", HEARTBEAT_INTERVAL);
                GSON.toJson(json, writer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
