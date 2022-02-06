package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.client.YigdClient;
import com.b1n_ry.yigd.config.PriorityInventoryConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo // Probably unnecessary, but better safe than sorry
@Mixin(AbstractConfigScreen.class)
public abstract class AbstractConfigScreenMixin {
    @Inject(method = "saveAll", at = @At(value = "RETURN"), remap = false)
    private void updateConfig(boolean entries, CallbackInfo ci) {
        YigdConfig config = YigdConfig.getConfig();
        PriorityInventoryConfig normalPriority = config.graveSettings.priority;
        PriorityInventoryConfig robbingPriority = config.graveSettings.graveRobbing.robPriority;

        if (normalPriority == YigdClient.normalPriority && robbingPriority == YigdClient.robbingPriority) return;

        PacketByteBuf buf = PacketByteBufs.create()
                        .writeEnumConstant(normalPriority)
                        .writeEnumConstant(robbingPriority);
        try {
            ClientPlayNetworking.send(new Identifier("yigd", "config_update"), buf);

            YigdClient.normalPriority = normalPriority;
            YigdClient.robbingPriority = robbingPriority;
            Yigd.LOGGER.info("Synced client priority configs to server");
        }
        catch (IllegalStateException e) {
            Yigd.LOGGER.warn("Tried to sync client config, but didn't find a server to sync to");
        }
    }
}
