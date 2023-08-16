package com.b1n_ry.yigd;

import com.b1n_ry.yigd.block.GraveBlock;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.compat.InvModCompat;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.events.ServerEventHandler;
import com.b1n_ry.yigd.events.YigdEventHandler;
import com.b1n_ry.yigd.other.Commands;
import com.b1n_ry.yigd.packets.ServerPacketHandler;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Yigd implements ModInitializer {
    public static final String MOD_ID = "yigd";

    public static Logger LOGGER = LoggerFactory.getLogger("YIGD");

    public static GraveBlock GRAVE_BLOCK = new GraveBlock(FabricBlockSettings.create().strength(0.8f, 3600000.0f).nonOpaque());
    public static BlockEntityType<GraveBlockEntity> GRAVE_BLOCK_ENTITY;

    /**
     * Any runnable added to this list will be executed on the end of the current server tick.
     * Use if runnable is required to run before some other event that would have otherwise ran before.
     */
    public static List<Runnable> END_OF_TICK = new ArrayList<>();

    @Override
    public void onInitialize() {
        AutoConfig.register(YigdConfig.class, JanksonConfigSerializer::new);

        GRAVE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(MOD_ID, "grave_block_entity"), FabricBlockEntityTypeBuilder.create(GraveBlockEntity::new, GRAVE_BLOCK).build());

        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "grave"), GRAVE_BLOCK);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "grave"), new BlockItem(GRAVE_BLOCK, new FabricItemSettings()));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(GRAVE_BLOCK.asItem()));

        InvModCompat.initModCompat();

        YigdEventHandler.registerEventCallbacks();
        ServerEventHandler.registerEvents();
        ServerPacketHandler.registerReceivers();

        Commands.register();
    }
}
