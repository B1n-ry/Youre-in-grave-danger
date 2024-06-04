package com.b1n_ry.yigd.mixin.accessor;

import net.minecraft.entity.damage.DamageRecord;
import net.minecraft.entity.damage.DamageTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(DamageTracker.class)
public interface DamageTrackerAccessor {
    @Accessor("recentDamage")
    List<DamageRecord> getRecentDamage();
}
