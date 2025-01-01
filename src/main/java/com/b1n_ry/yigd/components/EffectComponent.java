package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.config.YigdConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodData;

import java.util.ArrayList;
import java.util.List;

public class EffectComponent {
    private final List<MobEffectInstance> effects;
    private final int resetHp;
    private final int resetHunger;
    private final float resetSaturation;


    public EffectComponent(ServerPlayer player) {
        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.RespawnConfig rConfig = config.respawnConfig;

        this.effects = new ArrayList<>();
        this.loadEffectsFromConfig(rConfig);

        this.resetHp = rConfig.respawnHealth;

        FoodData hungerManager = player.getFoodData();
        if (!rConfig.resetHunger) {
            this.resetHunger = hungerManager.getFoodLevel();
        } else {
            this.resetHunger = rConfig.respawnHunger;
        }
        if (!rConfig.resetSaturation) {
            this.resetSaturation = hungerManager.getSaturationLevel();
        } else {
            this.resetSaturation = rConfig.respawnSaturation;
        }
    }

    public EffectComponent(List<MobEffectInstance> effects, int resetHp, int resetHunger, float resetSaturation) {
        this.effects = effects;
        this.resetHp = resetHp;
        this.resetHunger = resetHunger;
        this.resetSaturation = resetSaturation;
    }

    public void applyToPlayer(ServerPlayer player) {
        if (this.resetHp > 0)
            player.setHealth(this.resetHp);

        FoodData hungerManager = player.getFoodData();
        if (this.resetHunger >= 0)
            hungerManager.setFoodLevel(this.resetHunger);
        if (this.resetSaturation >= 0)
            hungerManager.setSaturation(this.resetSaturation);

        for (MobEffectInstance effect : this.effects) {
            player.addEffect(effect);
        }
    }

    public CompoundTag toNbt() {
        CompoundTag nbtCompound = new CompoundTag();
        nbtCompound.putInt("hp", this.resetHp);
        nbtCompound.putInt("hunger", this.resetHunger);
        nbtCompound.putFloat("saturation", this.resetSaturation);

        ListTag nbtEffects = new ListTag();
        for (MobEffectInstance instance : this.effects) {
            nbtEffects.add(instance.save());
        }
        nbtCompound.put("effects", nbtEffects);

        return nbtCompound;
    }

    private void loadEffectsFromConfig(YigdConfig.RespawnConfig rConfig) {
        for (YigdConfig.RespawnConfig.EffectConfig effect : rConfig.respawnEffects) {
            MobEffect statusEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse(effect.effectName));
            if (statusEffect == null) continue;

            Holder<MobEffect> effectRegistryEntry = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(statusEffect);
            MobEffectInstance effectInstance = new MobEffectInstance(effectRegistryEntry, effect.effectTime, effect.effectLevel - 1, false, effect.showBubbles);
            this.effects.add(effectInstance);
        }
    }

    public static EffectComponent fromNbt(CompoundTag nbt) {
        int resetHp = nbt.getInt("hp");
        int resetHunger = nbt.getInt("hunger");
        float resetSaturation = nbt.getFloat("saturation");

        List<MobEffectInstance> effects = new ArrayList<>();
        ListTag effectsNbt = nbt.getList("effects", Tag.TAG_COMPOUND);
        for (Tag e : effectsNbt) {
            CompoundTag compound = (CompoundTag) e;
            MobEffectInstance instance = MobEffectInstance.load(compound);
            effects.add(instance);
        }

        return new EffectComponent(effects, resetHp, resetHunger, resetSaturation);
    }
}
