package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.compat.TravelersBackpackCompat;
import com.b1n_ry.yigd.config.YigdConfig;
import com.tiviacz.travelersbackpack.util.BackpackUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(BackpackUtils.class)
public class TravelersBackpackMixin {
    @Inject(method = "onPlayerDrops", at = @At("HEAD"), cancellable = true)
    private static void shouldNotLetTravelersBackpackHandlePlacement(World world, PlayerEntity player, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        boolean travelersbackpackCompatLoaded = YigdConfig.getConfig().compatConfig.enableTravelersBackpackCompat;
        boolean accessoriesLoaded = FabricLoader.getInstance().isModLoaded("accessories");
        boolean accessoriesEnabled = YigdConfig.getConfig().compatConfig.enableAccessoriesCompat;
        boolean accessoriesIntegration = TravelersBackpackCompat.isAccessoriesIntegrationEnabled();
        if (
                (travelersbackpackCompatLoaded && !accessoriesIntegration)
                || (travelersbackpackCompatLoaded && !accessoriesLoaded)
                || (accessoriesLoaded && accessoriesEnabled && accessoriesIntegration)
        ) {
            cir.setReturnValue(false);
        }
    }
}
