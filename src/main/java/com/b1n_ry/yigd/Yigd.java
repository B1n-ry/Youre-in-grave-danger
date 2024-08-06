package com.b1n_ry.yigd;

import com.b1n_ry.yigd.block.GraveBlock;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.compat.InvModCompat;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.ClaimPriority;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.events.ServerEventHandler;
import com.b1n_ry.yigd.events.YigdServerEventHandler;
import com.b1n_ry.yigd.item.DeathScrollItem;
import com.b1n_ry.yigd.item.GraveKeyItem;
import com.b1n_ry.yigd.networking.PacketInitializer;
import com.b1n_ry.yigd.util.GraveCompassHelper;
import com.b1n_ry.yigd.util.YigdCommands;
import com.b1n_ry.yigd.networking.ServerPacketHandler;
import com.b1n_ry.yigd.util.YigdResourceHandler;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Yigd implements ModInitializer {
    public static final String MOD_ID = "yigd";

    public static Logger LOGGER = LoggerFactory.getLogger("YIGD");

    public static GraveBlock GRAVE_BLOCK = new GraveBlock(AbstractBlock.Settings.create().strength(0.8f, 3600000.0f).nonOpaque());
    public static BlockEntityType<GraveBlockEntity> GRAVE_BLOCK_ENTITY;


    // Optional registries
    public static DeathScrollItem DEATH_SCROLL_ITEM = new DeathScrollItem(new Item.Settings());
    public static GraveKeyItem GRAVE_KEY_ITEM = new GraveKeyItem(new Item.Settings());

    /**
     * Any runnable added to this list will be executed on the end of the current server tick.
     * Use if runnable is required to run before some other event that would have otherwise ran before.
     */
    public static List<Runnable> END_OF_TICK = new ArrayList<>();

    public static Map<UUID, List<String>> NOT_NOTIFIED_ROBBERIES = new HashMap<>();
    public static Map<UUID, ClaimPriority> CLAIM_PRIORITIES = new HashMap<>();
    public static Map<UUID, ClaimPriority> ROB_PRIORITIES = new HashMap<>();

    @Override
    public void onInitialize() {
        AutoConfig.register(YigdConfig.class, GsonConfigSerializer::new);

        GRAVE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "grave_block_entity"), BlockEntityType.Builder.create(GraveBlockEntity::new, GRAVE_BLOCK).build());

        Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "grave"), GRAVE_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "grave"), new BlockItem(GRAVE_BLOCK, new Item.Settings()));

        Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(Yigd.MOD_ID, "grave_location"), GraveCompassHelper.GRAVE_LOCATION);
        Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(Yigd.MOD_ID, "grave_id"), GraveComponent.GRAVE_ID);

        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "death_scroll"), DEATH_SCROLL_ITEM);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "grave_key"), GRAVE_KEY_ITEM);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(GRAVE_BLOCK.asItem());

            entries.add(DEATH_SCROLL_ITEM.getDefaultStack());
            entries.add(GRAVE_KEY_ITEM.getDefaultStack());
        });

        PacketInitializer.init();

        // Makes sure proper mod compatibilities are loaded (on world load to check mods' config)
        ServerLifecycleEvents.SERVER_STARTED.register(server -> InvModCompat.initModCompat());

        YigdServerEventHandler.registerEventCallbacks();
        ServerEventHandler.registerEvents();
        ServerPacketHandler.registerReceivers();
        YigdResourceHandler.init();

        YigdCommands.register();
    }
}
