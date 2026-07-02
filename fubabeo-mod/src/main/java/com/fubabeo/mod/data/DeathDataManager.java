package com.fubabeo.mod.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeathDataManager {
    private static final Gson GSON = new Gson();
    private static final int MAX_DEATHS = 20; // Keep last 20 deaths
    private static List<DeathPosition> deathHistory = null;

    public static class DeathPosition {
        public double x, y, z;
        public String dimension;
        public long timestamp;

        public DeathPosition(double x, double y, double z, String dimension) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static File getFile() {
        File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "fubabeo");
        if (!configDir.exists()) configDir.mkdirs();
        return new File(configDir, "deaths.json");
    }

    private static void load() {
        if (deathHistory != null) return;
        deathHistory = new ArrayList<>();
        File file = getFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<List<DeathPosition>>(){}.getType();
                List<DeathPosition> loaded = GSON.fromJson(reader, type);
                if (loaded != null) deathHistory = loaded;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void save() {
        try (FileWriter writer = new FileWriter(getFile())) {
            GSON.toJson(deathHistory, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveDeathPosition(double x, double y, double z, String dimension) {
        load();
        deathHistory.add(0, new DeathPosition(x, y, z, dimension)); // Add at front (newest first)
        // Trim to MAX_DEATHS
        while (deathHistory.size() > MAX_DEATHS) {
            deathHistory.remove(deathHistory.size() - 1);
        }
        save();
    }

    public static DeathPosition getLastDeath() {
        load();
        if (deathHistory.isEmpty()) return null;
        return deathHistory.get(0);
    }

    public static List<DeathPosition> getDeathHistory() {
        load();
        return Collections.unmodifiableList(deathHistory);
    }
}
