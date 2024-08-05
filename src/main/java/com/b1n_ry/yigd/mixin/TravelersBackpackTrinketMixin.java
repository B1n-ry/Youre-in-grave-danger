package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.config.YigdConfig;
import com.tiviacz.travelersbackpack.compat.trinkets.TravelersBackpackTrinket;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketEnums;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(TravelersBackpackTrinket.class)
public class TravelersBackpackTrinketMixin {
    @Inject(method = "getDropRule", at = @At(value = "HEAD"), cancellable = true)
    private void getBackpackDropRule(ItemStack stack, SlotReference slot, LivingEntity entity, CallbackInfoReturnable<TrinketEnums.DropRule> cir) {
        // If trinket compat is not enabled, TB should handle backpack according to its own rules. If it's enabled, we should handle it
        if (!YigdConfig.getConfig().compatConfig.enableTrinketsCompat) return;

        TrinketEnums.DropRule dropRule = switch (YigdConfig.getConfig().compatConfig.defaultTravelersBackpackDropRule) {
            case DESTROY -> TrinketEnums.DropRule.DESTROY;
            case KEEP -> TrinketEnums.DropRule.KEEP;
            default -> TrinketEnums.DropRule.DEFAULT;
        };
        cir.setReturnValue(dropRule);
    }
}