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

@Mixin(net.minecraft.network.ClientConnection.class)
public class NetworkHandlerMixin {
    @Inject(method = { "send", "method_10743" }, at = @At("HEAD"))
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        try {
            if (packet instanceof PlayerInteractBlockC2SPacket) {
                PlayerInteractBlockC2SPacket interactPacket = (PlayerInteractBlockC2SPacket) packet;
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.world == null || mc.player == null) return;
                
                BlockHitResult hitResult = interactPacket.getBlockHitResult();
                BlockPos pos = hitResult.getBlockPos();
                BlockState state = mc.world.getBlockState(pos);
                Block blockClicked = state.getBlock();
                
                // 1. Detect Opening Chests
                if (blockClicked == Blocks.CHEST || blockClicked == Blocks.TRAPPED_CHEST || blockClicked == Blocks.ENDER_CHEST || blockClicked == Blocks.BARREL) {
                    String blockType = blockClicked == Blocks.ENDER_CHEST ? "OPEN:ender_chest" : (blockClicked == Blocks.BARREL ? "OPEN:barrel" : "OPEN:chest");
                    ApiClient.sendBlockPlaceEvent(blockType, pos.getX(), pos.getY(), pos.getZ(), mc.world.getRegistryKey().getValue().toString());
                }
                
                // 2. Detect Placing Blocks (much more reliable than BlockItem.useOnBlock)
                ItemStack stackInHand = mc.player.getStackInHand(interactPacket.getHand());
                if (stackInHand.getItem() instanceof BlockItem) {
                    Block blockInHand = ((BlockItem) stackInHand.getItem()).getBlock();
                    String blockType = null;
                    if (blockInHand == Blocks.CHEST || blockInHand == Blocks.TRAPPED_CHEST) blockType = "minecraft:chest";
                    else if (blockInHand == Blocks.ENDER_CHEST) blockType = "minecraft:ender_chest";
                    else if (blockInHand == Blocks.ENCHANTING_TABLE) blockType = "minecraft:enchanting_table";
                    else if (blockInHand == Blocks.BARREL) blockType = "minecraft:barrel";
                    else if (blockInHand.getTranslationKey().contains("bed")) {
                        blockType = "minecraft:" + blockInHand.getTranslationKey().replace("block.minecraft.", "");
                    }
                    
                    if (blockType != null) {
                        BlockPos placePos = pos.offset(hitResult.getSide());
                        ApiClient.sendBlockPlaceEvent(blockType, placePos.getX(), placePos.getY(), placePos.getZ(), mc.world.getRegistryKey().getValue().toString());
                    }
                }
            }
        } catch (Throwable e) {
            // Silently ignore to avoid crashing game
        }
    }
}
