package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.compat.misc_compat_mods.CommonProtectionApiCompat;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.events.LoadModCompatEvent;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public interface InvModCompat<T> {
    List<InvModCompat<?>> invCompatMods = new ArrayList<>();
    static void reloadModCompat() {
        invCompatMods.clear();
        FabricLoader loader = FabricLoader.getInstance();
        YigdConfig.CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

        boolean accessoriesPresent = loader.isModLoaded("accessories");

        boolean trinketsPresent = loader.isModLoaded("trinkets");

        if (accessoriesPresent && compatConfig.enableAccessoriesCompat)
            invCompatMods.add(new AccessoriesCompat());
        if (trinketsPresent && compatConfig.enableTrinketsCompat && !loader.isModLoaded("tclayer"))
            invCompatMods.add(new TrinketsCompat());
/*        if (compatConfig.enableInventorioCompat && loader.isModLoaded("inventorio"))
            invCompatMods.add(new InventorioCompat());*/
        if (loader.isModLoaded("travelersbackpack")) {
            if (compatConfig.enableTravelersBackpackCompat && !((accessoriesPresent || trinketsPresent) && TravelersBackpackCompat.isIntegrationEnabled()))
                invCompatMods.add(new TravelersBackpackCompat());
        }
        if (compatConfig.enableLevelzCompat && loader.isModLoaded("levelz"))
            invCompatMods.add(new LevelzCompat());
/*        if (compatConfig.enableNumismaticOverhaulCompat && loader.isModLoaded("numismatic-overhaul"))
            invCompatMods.add(new NumismaticOverhaulCompat());*/
//        if (compatConfig.enableOriginsInventoryCompat && loader.isModLoaded("apoli"))
//            invCompatMods.add(new OriginsCompat());
//        if (loader.isModLoaded("beansbackpacks")) {
//            if (trinketsPresent)
//                BeansBackpacksCompat.prepForTrinkets();
//            else if (compatConfig.enableBeansBackpacksCompat)
//                invCompatMods.add(new BeansBackpacksCompat());
//        }

        if (compatConfig.enableProtectionApiCompat && loader.isModLoaded("common-protection-api"))
            CommonProtectionApiCompat.init();
//        if (loader.isModLoaded("orpheus"))
//            OrpheusCompat.init();
//        if (compatConfig.enableRespawnObelisksCompat && loader.isModLoaded("respawnobelisks"))
//            RespawnObelisksCompat.init();

        LoadModCompatEvent.EVENT.invoker().loadModCompat(invCompatMods);
    }

    String getModName();
    void clear(ServerPlayer player);
    CompatComponent<T> readNbt(CompoundTag nbt, HolderLookup.Provider registryLookup);

    CompatComponent<T> getNewComponent(ServerPlayer player);
}
