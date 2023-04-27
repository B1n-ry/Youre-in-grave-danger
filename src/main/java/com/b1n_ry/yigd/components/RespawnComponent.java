package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.data.DeathInfoManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;

public class RespawnComponent {
    private DefaultedList<ItemStack> soulboundItems;

    public RespawnComponent() {

    }

    public void setSoulboundItems(DefaultedList<ItemStack> items) {
        this.soulboundItems = items;
    }

    public void primeForRespawn(GameProfile profile) {
        DeathInfoManager.INSTANCE.addRespawnComponent(profile, this);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        return nbt;
    }

    public static RespawnComponent fromNbt(NbtCompound nbt) {
        RespawnComponent component = new RespawnComponent();

        return component;
    }
}
