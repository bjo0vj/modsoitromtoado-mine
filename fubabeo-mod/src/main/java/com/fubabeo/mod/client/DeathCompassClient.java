package com.fubabeo.mod.client;

import com.fubabeo.mod.network.ApiClient;
import com.fubabeo.mod.network.ApiConfig;
import com.fubabeo.mod.network.HeartbeatManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class DeathCompassClient implements ClientModInitializer {

    private static int joinTicks = 0;
    private static boolean joined = false;
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

    private static int liveTrackingTicks = 0;
    private static int proximityTicks = 0;
    
    // Proximity state
    private static Set<String> lastNearbyPlayers = new HashSet<>();
    private static int stableProximityTicks = 0;
    private static final int STABLE_REPORT_TICKS = 12000; // 10 minutes (20 ticks/sec * 60 sec * 10 = 12000)

    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register(this::onBlockUse);
        ClientPlayConnectionEvents.JOIN.register(this::onClientJoin);
        ClientPlayConnectionEvents.DISCONNECT.register(this::onClientDisconnect);
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private ActionResult onBlockUse(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (world.isClient) {
            try {
                net.minecraft.util.math.BlockPos pos = hitResult.getBlockPos();
                net.minecraft.block.BlockState state = world.getBlockState(pos);
                net.minecraft.block.Block blockClicked = state.getBlock();

                // 1. Send event when opening chests/barrels
                if (!player.isSneaking() && isStorageBlock(blockClicked)) {
                    String blockType = getStorageBlockType(blockClicked);
                    ApiClient.sendBlockPlaceEvent(blockType, pos.getX(), pos.getY(), pos.getZ(), world.getRegistryKey().getValue().toString());
                }

                // 2. Send event when placing special blocks
                net.minecraft.item.ItemStack stackInHand = player.getStackInHand(hand);
                if (stackInHand.getItem() instanceof net.minecraft.item.BlockItem) {
                    net.minecraft.block.Block blockInHand = ((net.minecraft.item.BlockItem) stackInHand.getItem()).getBlock();
                    String blockType = getPlaceableBlockType(blockInHand);
                    
                    if (blockType != null) {
                        net.minecraft.util.math.BlockPos placePos = pos.offset(hitResult.getSide());
                        ApiClient.sendBlockPlaceEvent(blockType, placePos.getX(), placePos.getY(), placePos.getZ(), world.getRegistryKey().getValue().toString());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ActionResult.PASS;
    }

    private boolean isStorageBlock(net.minecraft.block.Block block) {
        return block == net.minecraft.block.Blocks.CHEST 
            || block == net.minecraft.block.Blocks.TRAPPED_CHEST 
            || block == net.minecraft.block.Blocks.ENDER_CHEST 
            || block == net.minecraft.block.Blocks.BARREL;
    }

    private String getStorageBlockType(net.minecraft.block.Block block) {
        if (block == net.minecraft.block.Blocks.ENDER_CHEST) return "OPEN:ender_chest";
        if (block == net.minecraft.block.Blocks.BARREL) return "OPEN:barrel";
        return "OPEN:chest";
    }

    private String getPlaceableBlockType(net.minecraft.block.Block block) {
        if (block == net.minecraft.block.Blocks.CHEST || block == net.minecraft.block.Blocks.TRAPPED_CHEST) return "minecraft:chest";
        if (block == net.minecraft.block.Blocks.ENDER_CHEST) return "minecraft:ender_chest";
        if (block == net.minecraft.block.Blocks.BARREL) return "minecraft:barrel";
        if (block == net.minecraft.block.Blocks.ENCHANTING_TABLE) return "minecraft:enchanting_table";
        if (block == net.minecraft.block.Blocks.HOPPER) return "minecraft:hopper";
        return null;
    }

    private void onClientJoin(net.minecraft.client.network.ClientPlayNetworkHandler handler, net.fabricmc.fabric.api.networking.v1.PacketSender sender, MinecraftClient client) {
        joined = true;
        joinTicks = 0;
        
        String serverIp = "Singleplayer";
        if (client.getCurrentServerEntry() != null) {
            serverIp = client.getCurrentServerEntry().address;
        }
        HeartbeatManager.start(serverIp);
    }

    private void onClientDisconnect(net.minecraft.client.network.ClientPlayNetworkHandler handler, MinecraftClient client) {
        joined = false;
        HeartbeatManager.stop();
    }

    private void onClientTick(MinecraftClient client) {
        if (!joined || client.player == null || client.world == null) return;

        handleDelayedLoginEvent();
        handleLiveTracking(client);
        handleProximityScan(client);
    }

    private void handleDelayedLoginEvent() {
        if (joinTicks < 40) {
            joinTicks++;
            if (joinTicks == 40) {
                ApiClient.sendLogin("1.0.1-ServerMonitor");
            }
        }
    }

    private void handleLiveTracking(MinecraftClient client) {
        if (ApiConfig.LIVE_TRACKING_ENABLED) {
            liveTrackingTicks++;
            if (liveTrackingTicks >= 40) { // 2 seconds
                liveTrackingTicks = 0;
                ApiClient.sendHeartbeat(client.player.getX(), client.player.getY(), client.player.getZ(), client.player.getWorld().getRegistryKey().getValue().toString());
            }
        }
    }

    private void handleProximityScan(MinecraftClient client) {
        proximityTicks++;
        if (proximityTicks >= 40) { // 2 seconds
            proximityTicks = 0;
            
            Set<String> currentNearby = new HashSet<>();
            String closestPlayer = null;
            double closestDistSq = Double.MAX_VALUE;

            double px = client.player.getX();
            double py = client.player.getY();
            double pz = client.player.getZ();
            
            for (AbstractClientPlayerEntity p : client.world.getPlayers()) {
                if (p != client.player) {
                    double dx = p.getX() - px;
                    double dy = p.getY() - py;
                    double dz = p.getZ() - pz;
                    
                    String name = p.getName().getString();
                    
                    // Filter out fake players (NPCs, Citizens, Holograms)
                    if (!VALID_NAME_PATTERN.matcher(name).matches()) continue;

                    currentNearby.add(name);
                    
                    double distSq = dx*dx + dy*dy + dz*dz;
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closestPlayer = name;
                    }
                }
            }

            boolean hasNew = currentNearby.stream().anyMatch(p -> !lastNearbyPlayers.contains(p));

            if (hasNew || currentNearby.size() != lastNearbyPlayers.size()) { 
                if (hasNew) {
                    ApiClient.sendProximityEvent("NEARBY_CHANGED", px, py, pz, client.player.getWorld().getRegistryKey().getValue().toString(), currentNearby, closestPlayer);
                    stableProximityTicks = 0; // Reset counter
                }
            }

            stableProximityTicks += 40;
            if (stableProximityTicks >= STABLE_REPORT_TICKS) {
                ApiClient.sendProximityEvent("NEARBY_STABLE_10M", px, py, pz, client.player.getWorld().getRegistryKey().getValue().toString(), currentNearby, closestPlayer);
                stableProximityTicks = 0; // Reset counter
            }

            lastNearbyPlayers = currentNearby;
        }
    }
}
