package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.compat.misc_compat_mods.CommonProtectionApiCompat;
import com.b1n_ry.yigd.compat.misc_compat_mods.OrpheusCompat;
import com.b1n_ry.yigd.config.YigdConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public interface InvModCompat<T> {
    List<InvModCompat<?>> invCompatMods = new ArrayList<>();
    static void initModCompat() {
        invCompatMods.clear();
        FabricLoader loader = FabricLoader.getInstance();
        YigdConfig.CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

        boolean trinketsPresent = compatConfig.enableTrinketsCompat && loader.isModLoaded("trinkets");

        if (trinketsPresent)
            invCompatMods.add(new TrinketsCompat());
        if (compatConfig.enableInventorioCompat && loader.isModLoaded("inventorio"))
            invCompatMods.add(new InventorioCompat());
        if (loader.isModLoaded("travelersbackpack")) {
            if (compatConfig.enableTravelersBackpackCompat && !TravelersBackpackCompat.isTrinketIntegrationEnabled())
                invCompatMods.add(new TravelersBackpackCompat());
        }
//        if (compatConfig.enableLevelzCompat && loader.isModLoaded("levelz"))
//            invCompatMods.add(new LevelzCompat());
        if (compatConfig.enableNumismaticOverhaulCompat && loader.isModLoaded("numismatic-overhaul"))
            invCompatMods.add(new NumismaticOverhaulCompat());
        if (compatConfig.enableOriginsInventoryCompat && loader.isModLoaded("apoli"))
            invCompatMods.add(new OriginsCompat());
//        if (loader.isModLoaded("beansbackpacks")) {
//            if (trinketsPresent)
//                BeansBackpacksCompat.prepForTrinkets();
//            else if (compatConfig.enableBeansBackpacksCompat)
//                invCompatMods.add(new BeansBackpacksCompat());
//        }

        if (loader.isModLoaded("common-protection-api"))
            CommonProtectionApiCompat.init();
        if (loader.isModLoaded("orpheus"))
            OrpheusCompat.init();
//        if (compatConfig.enableRespawnObelisksCompat && loader.isModLoaded("respawnobelisks"))
//            RespawnObelisksCompat.init();
    }

    String getModName();
    void clear(ServerPlayerEntity player);
    CompatComponent<T> readNbt(NbtCompound nbt);

    CompatComponent<T> getNewComponent(ServerPlayerEntity player);
}
