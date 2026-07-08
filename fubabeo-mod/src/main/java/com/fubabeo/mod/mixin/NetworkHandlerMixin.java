package com.fubabeo.mod.mixin;

import com.fubabeo.mod.network.ApiClient;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.packet.Packet;

@Mixin({net.minecraft.client.network.ClientPlayNetworkHandler.class, net.minecraft.network.ClientConnection.class})
public class NetworkHandlerMixin {
    @Inject(method = { "sendPacket", "method_52787", "send", "method_10743" }, at = @At("HEAD"), require = 0)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        // Obsolete: Packet-based interaction tracking moved to UseBlockCallback in DeathCompassClient
    }
}
