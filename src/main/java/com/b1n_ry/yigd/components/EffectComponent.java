package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.config.YigdConfig;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class EffectComponent {
    private final List<StatusEffectInstance> effects;
    private final int resetHp;
    private final int resetHunger;
    private final float resetSaturation;


    public EffectComponent(ServerPlayerEntity player) {
        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.RespawnConfig rConfig = config.respawnConfig;

        this.effects = new ArrayList<>();
        this.loadEffectsFromConfig(rConfig);

        this.resetHp = rConfig.respawnHealth;

        HungerManager hungerManager = player.getHungerManager();
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

    public EffectComponent(List<StatusEffectInstance> effects, int resetHp, int resetHunger, float resetSaturation) {
        this.effects = effects;
        this.resetHp = resetHp;
        this.resetHunger = resetHunger;
        this.resetSaturation = resetSaturation;
    }

    public void applyToPlayer(ServerPlayerEntity player) {
        if (this.resetHp > 0)
            player.setHealth(this.resetHp);

        HungerManager hungerManager = player.getHungerManager();
        if (this.resetHunger >= 0)
            hungerManager.setFoodLevel(this.resetHunger);
        if (this.resetSaturation >= 0)
            hungerManager.setSaturationLevel(this.resetSaturation);

        for (StatusEffectInstance effect : this.effects) {
            player.addStatusEffect(effect);
        }
    }

    public NbtCompound toNbt() {
        NbtCompound nbtCompound = new NbtCompound();
        nbtCompound.putInt("hp", this.resetHp);
        nbtCompound.putInt("hunger", this.resetHunger);
        nbtCompound.putFloat("saturation", this.resetSaturation);

        NbtList nbtEffects = new NbtList();
        for (StatusEffectInstance instance : this.effects) {
            NbtCompound effectNbt = new NbtCompound();
            instance.writeNbt(effectNbt);

            nbtEffects.add(effectNbt);
        }
        nbtCompound.put("effects", nbtEffects);

        return nbtCompound;
    }

    private void loadEffectsFromConfig(YigdConfig.RespawnConfig rConfig) {
        for (YigdConfig.RespawnConfig.EffectConfig effect : rConfig.respawnEffects) {
            StatusEffect statusEffect = Registries.STATUS_EFFECT.get(new Identifier(effect.effectName));
            if (statusEffect == null) continue;

            StatusEffectInstance effectInstance = new StatusEffectInstance(statusEffect, effect.effectTime, effect.effectLevel - 1, false, effect.showBubbles);
            this.effects.add(effectInstance);
        }
    }

    public static EffectComponent fromNbt(NbtCompound nbt) {
        int resetHp = nbt.getInt("hp");
        int resetHunger = nbt.getInt("hunger");
        float resetSaturation = nbt.getFloat("saturation");

        List<StatusEffectInstance> effects = new ArrayList<>();
        NbtList effectsNbt = nbt.getList("effects", NbtElement.COMPOUND_TYPE);
        for (NbtElement e : effectsNbt) {
            NbtCompound compound = (NbtCompound) e;
            StatusEffectInstance instance = StatusEffectInstance.fromNbt(compound);
            effects.add(instance);
        }

        return new EffectComponent(effects, resetHp, resetHunger, resetSaturation);
    }
}
