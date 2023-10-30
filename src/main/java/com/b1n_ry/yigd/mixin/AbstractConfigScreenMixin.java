package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.packets.ClientPacketHandler;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractConfigScreen.class)
public class AbstractConfigScreenMixin {
    @Inject(method = "saveAll", at = @At(value = "RETURN"), remap = false)
    private void updateConfig(boolean openOtherScreens, CallbackInfo ci) {
        YigdConfig config = YigdConfig.getConfig();

        try {
            ClientPacketHandler.sendConfigUpdate(config);
            Yigd.LOGGER.info("Synced client priority configs to server");
        }
        catch (IllegalStateException e) {
            Yigd.LOGGER.warn("Tried to sync client config, but didn't find a server to sync to");
        }
    }
}
