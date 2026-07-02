package com.fubabeo.mod.client.command;

import com.fubabeo.mod.client.DeathCompassClient;
import com.fubabeo.mod.client.hud.CompassHudOverlay;
import com.fubabeo.mod.data.DeathDataManager;
import com.fubabeo.mod.data.WaypointManager;
import com.fubabeo.mod.network.ApiClient;
import net.minecraft.client.MinecraftClient;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;

public class ModCommands {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(ModCommands::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        // /see dead ...
        dispatcher.register(ClientCommandManager.literal("see")
                .then(ClientCommandManager.literal("dead")
                        .then(ClientCommandManager.literal("on").executes(context -> {
                            DeathDataManager.DeathPosition lastDeath = DeathDataManager.getLastDeath();
                            if (lastDeath != null) {
                                CompassHudOverlay.targetX = lastDeath.x;
                                CompassHudOverlay.targetZ = lastDeath.z;
                                CompassHudOverlay.targetName = "Last Death";
                                CompassHudOverlay.isVisible = true;
                                context.getSource().sendFeedback(Text.literal("§a✦ Đang dẫn đường tới điểm chết gần nhất"));
                            } else {
                                context.getSource().sendFeedback(Text.literal("§c✖ Chưa có dữ liệu về điểm chết."));
                            }
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("off").executes(context -> {
                            CompassHudOverlay.isVisible = false;
                            context.getSource().sendFeedback(Text.literal("§e✦ Đã tắt la bàn"));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("list").executes(context -> {
                            List<DeathDataManager.DeathPosition> deaths = DeathDataManager.getDeathHistory();
                            if (deaths.isEmpty()) {
                                context.getSource().sendFeedback(Text.literal("§cChưa có dữ liệu về điểm chết."));
                            } else {
                                context.getSource().sendFeedback(Text.literal("§b§l--- Lịch sử điểm chết ---"));
                                int i = 1;
                                for (DeathDataManager.DeathPosition d : deaths) {
                                    String dim = d.dimension.replace("minecraft:", "");
                                    context.getSource().sendFeedback(Text.literal(
                                            String.format("§e#%d §f%.0f, %.0f, %.0f §7(%s)", i, d.x, d.y, d.z, dim)
                                    ));
                                    i++;
                                    if (i > 10) {
                                        context.getSource().sendFeedback(Text.literal("§7... còn " + (deaths.size() - 10) + " điểm nữa"));
                                        break;
                                    }
                                }
                            }
                            return 1;
                        }))
                )
                // /see <tên>
                .then(ClientCommandManager.argument("name", StringArgumentType.string()).executes(context -> {
                    String name = StringArgumentType.getString(context, "name");
                    WaypointManager.Waypoint wp = WaypointManager.getWaypoint(name);
                    if (wp != null) {
                        CompassHudOverlay.targetX = wp.x;
                        CompassHudOverlay.targetZ = wp.z;
                        CompassHudOverlay.targetName = wp.name;
                        CompassHudOverlay.isVisible = true;
                        context.getSource().sendFeedback(Text.literal("§a✦ Đang dẫn đường tới: §f" + wp.name));
                    } else {
                        context.getSource().sendFeedback(Text.literal("§c✖ Không tìm thấy điểm đánh dấu: " + name));
                    }
                    return 1;
                }))
                // /see danhdau list
                .then(ClientCommandManager.literal("danhdau")
                        .then(ClientCommandManager.literal("list").executes(context -> {
                            Map<String, WaypointManager.Waypoint> waypoints = WaypointManager.getAllWaypoints();
                            if (waypoints == null || waypoints.isEmpty()) {
                                context.getSource().sendFeedback(Text.literal("§eBạn chưa có điểm đánh dấu nào."));
                            } else {
                                context.getSource().sendFeedback(Text.literal("§b§l--- Các điểm đánh dấu ---"));
                                for (WaypointManager.Waypoint wp : waypoints.values()) {
                                    String dim = wp.dimension.replace("minecraft:", "");
                                    context.getSource().sendFeedback(Text.literal(
                                            String.format("§a📍 %s §f%.0f, %.0f, %.0f §7(%s)", wp.name, wp.x, wp.y, wp.z, dim)
                                    ));
                                }
                            }
                            return 1;
                        }))
                )
                // /see list
                .then(ClientCommandManager.literal("list").executes(context -> {
                    context.getSource().sendFeedback(Text.literal("§b§l--- Lệnh fubabeo.mod ---"));
                    context.getSource().sendFeedback(Text.literal("§6La Bàn:"));
                    context.getSource().sendFeedback(Text.literal("  §f/see dead on§7/§foff §8- Bật/tắt la bàn tới điểm chết"));
                    context.getSource().sendFeedback(Text.literal("  §f/see dead list §8- Xem lịch sử điểm chết"));
                    context.getSource().sendFeedback(Text.literal("  §f/toado <x> <y> <z> §8- Dẫn đường tới tọa độ"));
                    context.getSource().sendFeedback(Text.literal("§6Đánh dấu:"));
                    context.getSource().sendFeedback(Text.literal("  §f/danhdau <tên> §8- Lưu vị trí hiện tại"));
                    context.getSource().sendFeedback(Text.literal("  §f/del <tên> §8- Xóa điểm đã lưu"));
                    context.getSource().sendFeedback(Text.literal("  §f/see <tên> §8- Bật la bàn tới điểm đã lưu"));
                    context.getSource().sendFeedback(Text.literal("  §f/see danhdau list §8- Xem tất cả điểm đã lưu"));
                    context.getSource().sendFeedback(Text.literal("§6Tiện ích:"));
                    context.getSource().sendFeedback(Text.literal("  §f/afk on§7/§foff §8- Tự động đi lùi tiến (chống kick)"));
                    return 1;
                }))
        );

        // /afk on|off
        dispatcher.register(ClientCommandManager.literal("afk")
                .then(ClientCommandManager.literal("on").executes(context -> {
                    if (DeathCompassClient.isAfk) {
                        context.getSource().sendFeedback(Text.literal("§e⚠ Chế độ AFK đã được bật từ trước!"));
                        return 0;
                    }
                    DeathCompassClient.isAfk = true;
                    context.getSource().sendFeedback(Text.literal("§a✦ Đã BẬT chế độ AFK! (Auto đi lùi-tiến 1 block)"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("off").executes(context -> {
                    if (!DeathCompassClient.isAfk) {
                        context.getSource().sendFeedback(Text.literal("§e⚠ Chế độ AFK đang tắt."));
                        return 0;
                    }
                    DeathCompassClient.isAfk = false;
                    // Release the keys so the player stops walking
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.options.forwardKey.setPressed(false);
                    client.options.backKey.setPressed(false);
                    context.getSource().sendFeedback(Text.literal("§c✦ Đã TẮT chế độ AFK!"));
                    return 1;
                }))
        );

        // /danhdau <tên>
        dispatcher.register(ClientCommandManager.literal("danhdau")
                .then(ClientCommandManager.argument("name", StringArgumentType.string()).executes(context -> {
                    String name = StringArgumentType.getString(context, "name");
                    double x = context.getSource().getPlayer().getX();
                    double y = context.getSource().getPlayer().getY();
                    double z = context.getSource().getPlayer().getZ();
                    String dimension = context.getSource().getPlayer().getWorld().getRegistryKey().getValue().toString();

                    WaypointManager.addWaypoint(name, x, y, z, dimension);

                    // Sync to server (silent)
                    try {
                        ApiClient.sendWaypointEvent(name, x, y, z, dimension);
                    } catch (Exception e) {
                        // Fail silently
                    }

                    context.getSource().sendFeedback(Text.literal(
                            String.format("§a✦ Đã lưu điểm: §f%s §atại §f%.0f, %.0f, %.0f", name, x, y, z)
                    ));
                    return 1;
                }))
        );

        // /del <tên>
        dispatcher.register(ClientCommandManager.literal("del")
                .then(ClientCommandManager.argument("name", StringArgumentType.string()).executes(context -> {
                    String name = StringArgumentType.getString(context, "name");
                    WaypointManager.Waypoint existing = WaypointManager.getWaypoint(name);
                    if (existing == null) {
                        context.getSource().sendFeedback(Text.literal("§c✖ Không tìm thấy điểm: " + name));
                        return 0;
                    }
                    WaypointManager.removeWaypoint(name);
                    context.getSource().sendFeedback(Text.literal("§a✦ Đã xóa điểm: §f" + name));
                    if (CompassHudOverlay.isVisible && CompassHudOverlay.targetName.equalsIgnoreCase(name)) {
                        CompassHudOverlay.isVisible = false;
                    }
                    return 1;
                }))
        );

        // /toado <x> <y> <z>
        dispatcher.register(ClientCommandManager.literal("toado")
                .then(ClientCommandManager.argument("x", DoubleArgumentType.doubleArg())
                        .then(ClientCommandManager.argument("y", DoubleArgumentType.doubleArg())
                                .then(ClientCommandManager.argument("z", DoubleArgumentType.doubleArg()).executes(context -> {
                                    double x = DoubleArgumentType.getDouble(context, "x");
                                    double y = DoubleArgumentType.getDouble(context, "y");
                                    double z = DoubleArgumentType.getDouble(context, "z");

                                    CompassHudOverlay.targetX = x;
                                    CompassHudOverlay.targetZ = z;
                                    CompassHudOverlay.targetName = String.format("Target (%d, %d)", (int)x, (int)z);
                                    CompassHudOverlay.isVisible = true;

                                    context.getSource().sendFeedback(Text.literal(
                                            String.format("§a✦ Đang dẫn đường tới §f%.0f, %.0f, %.0f", x, y, z)
                                    ));
                                    return 1;
                                }))
                        )
                )
        );
    }
}
