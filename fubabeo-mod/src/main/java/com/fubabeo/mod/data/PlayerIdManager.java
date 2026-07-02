package com.fubabeo.mod.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.UUID;

public class PlayerIdManager {
    private static final Gson GSON = new Gson();
    private static String playerUuid = null;

    public static String getUuid() {
        if (playerUuid != null) return playerUuid;

        File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "fubabeo");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File uuidFile = new File(configDir, "player_id.json");
        if (uuidFile.exists()) {
            try (FileReader reader = new FileReader(uuidFile)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null && json.has("uuid")) {
                    playerUuid = json.get("uuid").getAsString();
                    return playerUuid;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Generate new UUID
        playerUuid = UUID.randomUUID().toString();
        try (FileWriter writer = new FileWriter(uuidFile)) {
            JsonObject json = new JsonObject();
            json.addProperty("uuid", playerUuid);
            GSON.toJson(json, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return playerUuid;
    }

    public static String getIgn() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getSession() != null) {
            return client.getSession().getUsername();
        }
        return "Unknown";
    }
}
