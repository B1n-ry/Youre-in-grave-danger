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
        FabricLoader loader = FabricLoader.getInstance();
        YigdConfig.CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

        if (compatConfig.enableTrinketsCompat && loader.isModLoaded("trinkets"))
            invCompatMods.add(new TrinketsCompat());
        if (compatConfig.enableInventorioCompat && loader.isModLoaded("inventorio"))
            invCompatMods.add(new InventorioCompat());
        if (compatConfig.enableTravelersBackpackCompat && loader.isModLoaded("travelersbackpack"))
            invCompatMods.add(new TravelersBackpackCompat());
        if (compatConfig.enableLevelzCompat && loader.isModLoaded("levelz"))
            invCompatMods.add(new LevelzCompat());
        if (compatConfig.enableNumismaticOverhaulCompat && loader.isModLoaded("numismatic-overhaul"))
            invCompatMods.add(new NumismaticOverhaulCompat());
        if (compatConfig.enableOriginsInventoryCompat && loader.isModLoaded("apoli"))
            invCompatMods.add(new OriginsCompat());
        if (compatConfig.enableBeansBackpacksCompat && loader.isModLoaded("beansbackpacks"))
            invCompatMods.add(new BeansBackpacksCompat());

        if (loader.isModLoaded("common-protection-api"))
            CommonProtectionApiCompat.init();
        if (loader.isModLoaded("orpheus"))
            OrpheusCompat.init();
    }

    String getModName();
    void clear(ServerPlayerEntity player);
    CompatComponent<T> readNbt(NbtCompound nbt);

    CompatComponent<T> getNewComponent(ServerPlayerEntity player);
}
