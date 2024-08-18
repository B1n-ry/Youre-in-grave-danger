package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.networking.packets.UpdateConfigC2SPacket;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractConfigScreen.class)
public class AbstractConfigScreenMixin {
    @Inject(method = "save", at = @At("RETURN"))
    private void updateServerConfigs(CallbackInfo ci) {
        YigdConfig.GraveConfig config = YigdConfig.getConfig().graveConfig;
        try {
            PacketDistributor.sendToServer(new UpdateConfigC2SPacket(config.claimPriority, config.graveRobbing.robPriority));
            Yigd.LOGGER.info("Synced client priority configs to server");
        } catch (NullPointerException e) {
            Yigd.LOGGER.warn("Tried to sync client config, but didn't find a server to sync to");
        }
    }
}
