package com.fubabeo.mod.mixin;

import com.fubabeo.mod.network.ApiClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.item.BlockItem.class)
public class BlockPlaceMixin {
    @Inject(method = "useOnBlock", at = @At("RETURN"))
    private void onBlockPlace(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        try {
            ActionResult result = cir.getReturnValue();
            String resStr = result != null ? result.toString() : "";
            if (resStr.contains("FAIL") || resStr.contains("PASS")) return;

            World world = context.getWorld();
            if (context.getPlayer() == null) return;

            BlockPos pos = context.getBlockPos();
            Block block = ((net.minecraft.item.BlockItem)(Object)this).getBlock();

            String blockType = null;
            if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) blockType = "minecraft:chest";
            else if (block == Blocks.ENDER_CHEST) blockType = "minecraft:ender_chest";
            else if (block == Blocks.BARREL) blockType = "minecraft:barrel";
            else if (block.getTranslationKey().contains("bed")) blockType = "minecraft:white_bed"; // Normalize all beds for simplicity or use getTranslationKey() if we want specific color. Let's use string formatting.
            
            if (block.getTranslationKey().contains("bed")) {
                // e.g., block.minecraft.red_bed
                blockType = "minecraft:" + block.getTranslationKey().replace("block.minecraft.", "");
            }

            if (blockType != null) {
                double x = pos.getX();
                double y = pos.getY();
                double z = pos.getZ();
                String dimension = world.getRegistryKey().getValue().toString();

                ApiClient.sendBlockPlaceEvent(blockType, x, y, z, dimension);
            }
        } catch (Throwable e) {
            // Never crash the game
        }
    }
}
