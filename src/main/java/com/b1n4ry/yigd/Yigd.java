package com.b1n4ry.yigd;

import java.io.InputStream;
import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.block.GraveBlock;
import com.b1n4ry.yigd.block.entity.GraveBlockEntity;
import com.b1n4ry.yigd.client.render.GraveBlockEntityRenderer;
import com.b1n4ry.yigd.compat.InventorioCompat;
import com.b1n4ry.yigd.compat.TrinketsCompat;
import com.b1n4ry.yigd.config.PriorityInventoryConfig;
import com.b1n4ry.yigd.config.YigdConfig;
import com.b1n4ry.yigd.core.DeadPlayerData;
import com.b1n4ry.yigd.core.SoulboundEnchantment;
import com.b1n4ry.yigd.core.YigdCommand;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;

import java.io.InputStreamReader;
import java.util.*;

public class Yigd implements ModInitializer {

    public static final GraveBlock GRAVE_BLOCK = new GraveBlock(FabricBlockSettings.of(Material.STONE).strength(0.8f, 3600000.0f));
    public static BlockEntityType<GraveBlockEntity> GRAVE_BLOCK_ENTITY;

    public static DeadPlayerData deadPlayerData = new DeadPlayerData();
    public static JsonObject graveyard;
    public static Map<UUID, Pair<PriorityInventoryConfig, PriorityInventoryConfig>> clientPriority = new HashMap<>();

    private static Enchantment SOULBOUND;

    public static final ArrayList<YigdApi> apiMods = new ArrayList<>();
    public static final List<Runnable> NEXT_TICK = new ArrayList<>();

    @Override
    public void onInitialize() {
        AutoConfig.register(YigdConfig.class, Toml4jConfigSerializer::new);

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public void reload(ResourceManager manager) {
                System.out.println("[YIGD] Reloading graveyard");
                graveyard = null;

                for(Identifier id : manager.findResources("custom", path -> path.equals("graveyard.json"))) {
                    if (!id.getNamespace().equals("yigd")) continue;
                    try (InputStream stream = manager.getResource(id).getInputStream()) {
                        graveyard = (JsonObject) JsonParser.parseReader(new InputStreamReader(stream));
                    } catch(Exception e) { System.out.println("[YIGD] Error occurred while loading resource json " + id + "\n" + e); }
                }
            }

            @Override
            public Identifier getFabricId() {
                return new Identifier("yigd", "graveyard");
            }
        });
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public void reload(ResourceManager manager) {
                System.out.println("[YIGD] Reloading grave shape");
                GraveBlock.customModel = null;

                Collection<Identifier> ids = manager.findResources("models/block", path -> path.equals("grave.json"));

                for (Identifier id : ids) {
                    if (!id.getNamespace().equals("yigd")) continue;
                    try (InputStream stream = manager.getResource(id).getInputStream()) {
                        GraveBlock.customModel = (JsonObject) JsonParser.parseReader(new InputStreamReader(stream));
                        GraveBlock.reloadVoxelShapes();
                        GraveBlockEntityRenderer.reloadCustomModel();
                    } catch (Exception e) {
                        System.out.println("[YIGD] Error occurred while loading custom grave model " + id + "\n" + e);
                    }
                }
            }

            @Override
            public Identifier getFabricId() {
                return new Identifier("yigd", "models/block/grave");
            }
        });

        GRAVE_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, "yigd:grave_block_entity", FabricBlockEntityTypeBuilder.create(GraveBlockEntity::new, GRAVE_BLOCK).build(null));

        Registry.register(Registry.BLOCK, new Identifier("yigd", "grave"), GRAVE_BLOCK);
        Registry.register(Registry.ITEM, new Identifier("yigd", "grave"), new BlockItem(GRAVE_BLOCK, new FabricItemSettings().group(ItemGroup.DECORATIONS)));

        if (YigdConfig.getConfig().utilitySettings.soulboundEnchant) {
            SOULBOUND = Registry.register(Registry.ENCHANTMENT, new Identifier("yigd", "soulbound"), new SoulboundEnchantment());
        }

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
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            List<Runnable> tickFunctions = new ArrayList<>(NEXT_TICK);
            NEXT_TICK.clear();
            for (Runnable function : tickFunctions) {
                function.run();
            }
        });
    }
}