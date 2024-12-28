package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.config.YigdConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

public interface InvModCompat<T> {
    List<InvModCompat<?>> invCompatMods = new ArrayList<>();
    static void reloadModCompat() {
        invCompatMods.clear();
        ModList modList = ModList.get();
        YigdConfig.CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

        boolean accessoriesPresent = modList.isLoaded("accessories");
        boolean curiosPresent = modList.isLoaded("curios");

        if (compatConfig.enableAccessoriesCompat && accessoriesPresent)
            invCompatMods.add(new AccessoriesCompat());
        if (compatConfig.enableCuriosCompat && curiosPresent && !modList.isLoaded("cclayer"))
            invCompatMods.add(new CuriosCompat());
        if (modList.isLoaded("travelersbackpack")) {
            if (compatConfig.enableTravelersBackpackCompat && !((accessoriesPresent || curiosPresent) && TravelersBackpackCompat.isIntegrationEnabled()))
                invCompatMods.add(new TravelersBackpackCompat());
        }
        if (modList.isLoaded("cosmeticarmorreworked") && compatConfig.enableCosmeticArmorCompat)
            invCompatMods.add(new CosmeticArmorCompat());
    }

    String getModName();
    void clear(ServerPlayer player);
    CompatComponent<T> readNbt(CompoundTag nbt, HolderLookup.Provider registries);

    CompatComponent<T> getNewComponent(ServerPlayer player);
}
