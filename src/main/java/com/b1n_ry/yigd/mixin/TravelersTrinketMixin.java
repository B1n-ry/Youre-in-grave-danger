package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.config.YigdConfig;
import com.tiviacz.travelersbackpack.compat.trinkets.TravelersBackpackTrinket;
import com.tiviacz.travelersbackpack.config.TravelersBackpackConfig;
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
public class TravelersTrinketMixin {
    @Inject(method = "getDropRule", at = @At(value = "HEAD"))
    private void changeDropRule(ItemStack stack, SlotReference slot, LivingEntity entity, CallbackInfoReturnable<TrinketEnums.DropRule> cir) {
        TravelersBackpackConfig.backpackDeathPlace = TravelersBackpackConfig.backpackDeathPlace && !YigdConfig.getConfig().graveSettings.generateGraves;
    }
}
