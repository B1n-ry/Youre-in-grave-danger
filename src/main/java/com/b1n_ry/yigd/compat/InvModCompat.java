package com.b1n_ry.yigd.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public interface InvModCompat<T> {
    List<InvModCompat<?>> invCompatMods = new ArrayList<>();
    static void initModCompat() {
        FabricLoader loader = FabricLoader.getInstance();

        if (loader.isModLoaded("trinkets")) invCompatMods.add(new TrinketsCompat());
        if (loader.isModLoaded("inventorio")) invCompatMods.add(new InventorioCompat());
        if (loader.isModLoaded("travelersbackpack")) invCompatMods.add(new TravelersBackpackCompat());
        if (loader.isModLoaded("levelz")) invCompatMods.add(new LevelzCompat());
        if (loader.isModLoaded("numismatic-overhaul")) invCompatMods.add(new NumismaticOverhaulCompat());
        if (loader.isModLoaded("apoli")) invCompatMods.add(new OriginsCompat());

        if (loader.isModLoaded("common-protection-api")) {
            CommonProtectionApiCompat.init();
        }
    }

    String getModName();
    void clear(ServerPlayerEntity player);
    CompatComponent<T> readNbt(NbtCompound nbt);

    CompatComponent<T> getNewComponent(ServerPlayerEntity player);
}
