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
    static void initModCompat() {
        invCompatMods.clear();
        ModList modList = ModList.get();
        YigdConfig.CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

        boolean accessoriesPresent = compatConfig.enableAccessoriesCompat && modList.isLoaded("accessories");
        boolean curiosPresent = compatConfig.enableCuriosCompat && modList.isLoaded("curios");

        if (accessoriesPresent)
            invCompatMods.add(new AccessoriesCompat());
        if (curiosPresent && !modList.isLoaded("cclayer"))
            invCompatMods.add(new CuriosCompat());
        if (modList.isLoaded("travelersbackpack")) {
            if (compatConfig.enableTravelersBackpackCompat && !(accessoriesPresent && TravelersBackpackCompat.isAccessoriesIntegrationEnabled()))
                invCompatMods.add(new TravelersBackpackCompat());
        }
    }

    String getModName();
    void clear(ServerPlayer player);
    CompatComponent<T> readNbt(CompoundTag nbt, HolderLookup.Provider registries);

    CompatComponent<T> getNewComponent(ServerPlayer player);
}
