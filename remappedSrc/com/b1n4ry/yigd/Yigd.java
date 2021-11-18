package com.b1n4ry.yigd;

import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.block.GraveBlock;
import com.b1n4ry.yigd.block.entity.GraveBlockEntity;
import com.b1n4ry.yigd.compat.InventorioCompat;
import com.b1n4ry.yigd.compat.TrinketsCompat;
import com.b1n4ry.yigd.config.YigdConfig;
import com.b1n4ry.yigd.core.DeadPlayerData;
import com.b1n4ry.yigd.core.SoulboundEnchantment;
import com.b1n4ry.yigd.core.YigdCommand;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;

public class Yigd implements ModInitializer {

    public static final GraveBlock GRAVE_BLOCK = new GraveBlock(FabricBlockSettings.of(Material.STONE).strength(0.8f, 3000.0f));
    public static BlockEntityType<GraveBlockEntity> GRAVE_BLOCK_ENTITY;

    public static DeadPlayerData deadPlayerData = new DeadPlayerData();

    private static final Enchantment SOULBOUND = Registry.register(Registry.ENCHANTMENT, new Identifier("yigd", "soulbound"), new SoulboundEnchantment());

    public static final ArrayList<YigdApi> apiMods = new ArrayList<>();

    @Override
    public void onInitialize() {
        AutoConfig.register(YigdConfig.class, Toml4jConfigSerializer::new);

        Registry.register(Registry.BLOCK, new Identifier("yigd", "grave"), GRAVE_BLOCK);
        Registry.register(Registry.ITEM, new Identifier("yigd", "grave"), new BlockItem(GRAVE_BLOCK, new FabricItemSettings().group(ItemGroup.DECORATIONS)));

        GRAVE_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, "yigd:grave_block_entity", FabricBlockEntityTypeBuilder.create(GraveBlockEntity::new, GRAVE_BLOCK).build(null));

        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            apiMods.add(new TrinketsCompat());
        }
        if (FabricLoader.getInstance().isModLoaded("inventorio")) {
            apiMods.add(new InventorioCompat());
        }
        apiMods.addAll(FabricLoader.getInstance().getEntrypoints("yigd", YigdApi.class));

        YigdCommand.registerCommands();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            deadPlayerData = new DeadPlayerData();
        });
    }
}