package com.fubabeo.mod.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class WaypointManager {
    private static final Gson GSON = new Gson();
    private static Map<String, Waypoint> waypoints = null;

    public static class Waypoint {
        public String name;
        public double x, y, z;
        public String dimension;

        public Waypoint(String name, double x, double y, double z, String dimension) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
        }
    }

    private static File getFile() {
        File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "fubabeo");
        if (!configDir.exists()) configDir.mkdirs();
        return new File(configDir, "waypoints.json");
    }

    private static void load() {
        if (waypoints != null) return;
        waypoints = new HashMap<>();
        File file = getFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, Waypoint>>(){}.getType();
                Map<String, Waypoint> loaded = GSON.fromJson(reader, type);
                if (loaded != null) waypoints = loaded;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void save() {
        try (FileWriter writer = new FileWriter(getFile())) {
            GSON.toJson(waypoints, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addWaypoint(String name, double x, double y, double z, String dimension) {
        load();
        waypoints.put(name.toLowerCase(), new Waypoint(name, x, y, z, dimension));
        save();
    }

    public static void removeWaypoint(String name) {
        load();
        waypoints.remove(name.toLowerCase());
        save();
    }

    public static Waypoint getWaypoint(String name) {
        load();
        return waypoints.get(name.toLowerCase());
    }

    public static Map<String, Waypoint> getAllWaypoints() {
        load();
        return waypoints;
    }
}
