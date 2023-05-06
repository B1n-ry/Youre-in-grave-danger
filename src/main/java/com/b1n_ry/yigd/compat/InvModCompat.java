package com.b1n_ry.yigd.compat;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;

public interface InvModCompat<T> {
    Map<String, InvModCompat<?>> invCompatMods = new HashMap<>();
    static void initModCompat() {
        invCompatMods.put("trinkets", new TrinketsCompat());
    }

    String getModName();
    void clear(ServerPlayerEntity player);
    CompatComponent<T> readNbt(NbtCompound nbt);

    CompatComponent<T> getNewComponent(ServerPlayerEntity player);
}
