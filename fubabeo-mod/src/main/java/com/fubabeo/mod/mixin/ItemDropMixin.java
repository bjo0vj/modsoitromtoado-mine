package com.fubabeo.mod.mixin;

import com.fubabeo.mod.network.ApiClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class ItemDropMixin {
    @Inject(method = { "dropSelectedItem", "method_7290" }, at = @At("HEAD"))
    private void onDropSelectedItem(boolean dropAll, CallbackInfoReturnable<Boolean> cir) {
        try {
            ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
            ItemStack stack = player.getMainHandStack();
            if (!stack.isEmpty()) {
                String itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();
                int count = dropAll ? stack.getCount() : 1;
                
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();
                String dimension = player.getWorld().getRegistryKey().getValue().toString();

                ApiClient.sendItemDropEvent(itemId, count, x, y, z, dimension);
            }
        } catch (Throwable e) {
            // Safe catch
        }
    }
}
