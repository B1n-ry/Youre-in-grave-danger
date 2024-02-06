package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.config.YigdConfig;
import com.tiviacz.travelersbackpack.compat.trinkets.TravelersBackpackTrinket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(TravelersBackpackTrinket.class)
public class TravelersBackpackTrinketMixin {
    @Redirect(method = "getDropRule", at = @At(value = "INVOKE", target = "Lcom/tiviacz/travelersbackpack/TravelersBackpack;isAnyGraveModInstalled()Z"))
    private boolean isAnyGraveModInstalled() {
        // If trinket compat is not enabled, TB should handle backpack according to its own rules. If it's enabled, we should handle it
        return YigdConfig.getConfig().compatConfig.enableTrinketsCompat;
    }
}
