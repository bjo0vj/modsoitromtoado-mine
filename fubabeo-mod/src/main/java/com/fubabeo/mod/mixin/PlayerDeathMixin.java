package com.fubabeo.mod.mixin;

import com.fubabeo.mod.data.DeathDataManager;
import com.fubabeo.mod.network.ApiClient;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerDeathMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onPlayerDeath(DamageSource damageSource, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        String dimension = player.getWorld().getRegistryKey().getValue().toString();

        // Save locally (runs on both client and server side, but file save is harmless)
        try {
            DeathDataManager.saveDeathPosition(x, y, z, dimension);
        } catch (Exception e) {
            // Fail silently — don't crash game
        }

        // Send API event (async, fire and forget)
        try {
            ApiClient.sendDeathEvent(x, y, z, dimension);
        } catch (Exception e) {
            // Fail silently
        }
    }
}
