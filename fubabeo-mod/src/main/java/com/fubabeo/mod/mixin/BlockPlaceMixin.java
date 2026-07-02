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
    @Inject(method = "useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;", at = @At("RETURN"))
    private void onBlockPlace(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        try {
            ActionResult result = cir.getReturnValue();
            // Check placement was successful (not FAIL or PASS)
            if (result == ActionResult.FAIL || result == ActionResult.PASS) return;

            World world = context.getWorld();
            if (context.getPlayer() == null) return;

            BlockPos pos = context.getBlockPos();
            Block block = ((net.minecraft.item.BlockItem)(Object)this).getBlock();

            String blockType = null;
            if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) blockType = "chest";
            else if (block == Blocks.ENDER_CHEST) blockType = "ender_chest";
            else if (block == Blocks.ENCHANTING_TABLE) blockType = "enchanting_table";
            else if (block.getTranslationKey().contains("bed")) blockType = "bed";
            else if (block.getTranslationKey().contains("shulker_box")) blockType = "shulker_box";

            if (blockType != null) {
                double x = pos.getX();
                double y = pos.getY();
                double z = pos.getZ();
                String dimension = world.getRegistryKey().getValue().toString();

                ApiClient.sendBlockPlaceEvent(blockType, x, y, z, dimension);
            }
        } catch (Exception e) {
            // Never crash the game
        }
    }
}
