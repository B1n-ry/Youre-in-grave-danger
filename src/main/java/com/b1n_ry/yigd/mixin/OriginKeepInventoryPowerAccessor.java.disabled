package com.b1n_ry.yigd.mixin;

import io.github.apace100.apoli.power.KeepInventoryPower;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import java.util.function.Predicate;

@Pseudo
@Mixin(KeepInventoryPower.class)
public interface OriginKeepInventoryPowerAccessor {
    @Accessor("slots")
    Set<Integer> getSlots();

    @Accessor("keepItemCondition")
    Predicate<ItemStack> getKeepItemCondition();
}
