package com.b1n_ry.yigd;

import com.b1n_ry.yigd.api.ClaimModsApi;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.block.GraveBlock;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.client.gui.GraveViewScreen;
import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import com.b1n_ry.yigd.compat.*;
import com.b1n_ry.yigd.config.PriorityInventoryConfig;
import com.b1n_ry.yigd.config.ScrollTypeConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.*;
import com.b1n_ry.yigd.enchantment.DeathSightEnchantment;
import com.b1n_ry.yigd.enchantment.SoulboundEnchantment;
import com.b1n_ry.yigd.item.KeyItem;
import com.b1n_ry.yigd.item.ScrollItem;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Yigd implements ModInitializer, DedicatedServerModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("YIGD");

    public static List<UUID> notNotifiedPlayers = new ArrayList<>();
    public static Map<UUID, String> notNotifiedRobberies = new HashMap<>();

    public static Map<UUID, PriorityInventoryConfig> clientPriorities = new HashMap<>();
    public static Map<UUID, PriorityInventoryConfig> clientRobPriorities = new HashMap<>();

    public static final GraveBlock GRAVE_BLOCK = new GraveBlock(FabricBlockSettings.of(Material.STONE).strength(0.8f, 3600000.0f));
    public static BlockEntityType<GraveBlockEntity> GRAVE_BLOCK_ENTITY;

    public static Enchantment DEATH_SIGHT;

    public static JsonObject graveyard;

    public static Item SCROLL_ITEM;
    public static Item KEY_ITEM;

    public static final List<YigdApi> apiMods = new ArrayList<>();
    public static final List<ClaimModsApi> claimMods = new ArrayList<>();
    public static final List<String> miscCompatMods = new ArrayList<>();
    public static final List<Runnable> NEXT_TICK = new ArrayList<>();

    public static YigdConfig defaultConfig = null;

    @Override
    public void onInitialize() {
        try {
            AutoConfig.register(YigdConfig.class, Toml4jConfigSerializer::new);
        }
        catch (Exception e) {
            defaultConfig = new YigdConfig();
            LOGGER.error("Loading default YIGD config due to an error reading the config file. Delete yigd.toml, and a new working config file should generate", e);
        }

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier("yigd", "custom/graveyard");
            }

            @Override
            public void reload(ResourceManager manager) {
                graveyard = null;
                List<Resource> graveyardResources = manager.getAllResources(new Identifier("yigd", "custom/graveyard.json"));
                for (Resource resource : graveyardResources) {
                    try (InputStream stream = resource.getInputStream()) {
                        LOGGER.info("Reloading graveyard");
                        graveyard = (JsonObject) JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                    }
                    catch (Exception e) {
                        LOGGER.error("Error occurred while loading resource json yigd:graveyard" + "\n" + e);
                    }
                }

                List<Resource> graveAreaResources = manager.getAllResources(new Identifier("yigd", "custom/grave_areas.json"));
                for (Resource resource : graveAreaResources) {
                    try (InputStream stream = resource.getInputStream()) {
                        LOGGER.info("Reloading custom grave areas");
                        GraveAreaOverride.reloadGraveAreas((JsonObject) JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
                        break;
                    }
                    catch (Exception e) {
                        LOGGER.error("Error occurred while loading custom grave areas\n" + e);
                    }
                }
            }
        });
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public void reload(ResourceManager manager) {
                GraveBlock.customModel = null;

                List<Resource> blockResources = manager.getAllResources(new Identifier("yigd", "models/block/grave.json"));
                for (Resource resource : blockResources) {
                    try (InputStream stream = resource.getInputStream()) {
                        LOGGER.info("Reloading grave model (client)");
                        GraveBlock.customModel = (JsonObject) JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                        GraveBlock.reloadVoxelShapes(GraveBlock.customModel);
                        GraveBlockEntityRenderer.reloadCustomModel();
                    } catch (Exception e) {
                        LOGGER.error("Error occurred while loading custom grave model (client)" + "\n" + e);
                    }
                }

                GraveViewScreen.dimensionNameOverrides.clear();
                List<Resource> overrideResources = manager.getAllResources(new Identifier("yigd", "texts/dim_names.json"));
                for (Resource resource : overrideResources) {
                    try (InputStream stream = resource.getInputStream()) {
                        LOGGER.info("Reloading dimension name overrides for grave GUI");
                        JsonObject jObject = (JsonObject) JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                        for (Map.Entry<String, JsonElement> entry : jObject.entrySet()) {
                            GraveViewScreen.dimensionNameOverrides.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                    catch (Exception e) {
                        LOGGER.error("Error occurred while loading dimension name overrides for grave GUI\n" + e);
                    }
                }
            }

            @Override
            public Identifier getFabricId() {
                return new Identifier("yigd", "models/block/grave");
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerWorld world = server.getOverworld();
            if (world == null) { // If for some reason the overworld is not loaded
                for (ServerWorld serverWorld : server.getWorlds()) {
                    world = serverWorld;
                    break;
                }
            }
            if (world == null) return; // If for some reason there's NO world loaded
            DeathInfoManager.INSTANCE = (DeathInfoManager) world.getPersistentStateManager().getOrCreate(DeathInfoManager::fromNbt, DeathInfoManager::new, "yigd_grave_data");
            DeathInfoManager.INSTANCE.markDirty();
            LOGGER.info("Loaded data from grave data file");
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> DeathInfoManager.INSTANCE = null);

        GRAVE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, "yigd:grave_block_entity", FabricBlockEntityTypeBuilder.create(GraveBlockEntity::new, GRAVE_BLOCK).build(null));

        Registry.register(Registries.BLOCK, new Identifier("yigd", "grave"), GRAVE_BLOCK);
        Registry.register(Registries.ITEM, new Identifier("yigd", "grave"), new BlockItem(GRAVE_BLOCK, new FabricItemSettings()));

        YigdConfig.UtilitySettings utilityConfig = YigdConfig.getConfig().utilitySettings;
        if (utilityConfig.soulboundEnchant.enabled) {
            // Add the soulbound enchantment if it should be loaded (configured to enable)
            Registry.register(Registries.ENCHANTMENT, new Identifier("yigd", "soulbound"), new SoulboundEnchantment());
        }
        if (utilityConfig.deathSightEnchant.enabled) {
            // Add the death sight enchantment if it should be loaded (configured to enable)
            DEATH_SIGHT = new DeathSightEnchantment();
            Registry.register(Registries.ENCHANTMENT, new Identifier("yigd", "death_sight"), DEATH_SIGHT);
        }
        if (utilityConfig.scrollItem.scrollType != ScrollTypeConfig.DISABLED) {
            // Add the scroll item if it should be loaded (will write an error on world load if not enabled, but this can be ignored)
            SCROLL_ITEM = new ScrollItem(new Item.Settings());
            Registry.register(Registries.ITEM, new Identifier("yigd", "death_scroll"), SCROLL_ITEM);
        }
        if (utilityConfig.graveKeySettings.enableKeys) {
            // Add the grave key item if it should be loaded
            KEY_ITEM = new KeyItem(new Item.Settings());
            Registry.register(Registries.ITEM, new Identifier("yigd", "grave_key"), KEY_ITEM);
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> content.add(GRAVE_BLOCK.asItem()));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            if (utilityConfig.scrollItem.scrollType != ScrollTypeConfig.DISABLED)
                content.add(SCROLL_ITEM);
            if (utilityConfig.graveKeySettings.enableKeys)
                content.add(KEY_ITEM);
        });

        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            apiMods.add(new TrinketsCompat());
        }
//        if (FabricLoader.getInstance().isModLoaded("levelz")) {
//            apiMods.add(new LevelzCompat());
//        }
        if (FabricLoader.getInstance().isModLoaded("inventorio")) {
            apiMods.add(new InventorioCompat());
        }
        if (FabricLoader.getInstance().isModLoaded("travelersbackpack") && !TravelersBackpackCompat.isTrinketIntegrationOn()) {
            apiMods.add(new TravelersBackpackCompat());
        }
        if (FabricLoader.getInstance().isModLoaded("apoli")) {
            apiMods.add(new OriginsCompat());
            miscCompatMods.add("apoli");
        }
        if (FabricLoader.getInstance().isModLoaded("numismatic-overhaul")) {
            apiMods.add(new NumismaticOverhaulCompat());
        }
        apiMods.addAll(FabricLoader.getInstance().getEntrypoints("yigd", YigdApi.class));

//        if (FabricLoader.getInstance().isModLoaded("flan")) {
//            claimMods.add(new FlanCompat());
//        }
//        if (FabricLoader.getInstance().isModLoaded("ftbchunks")) {
//            claimMods.add(new FtbChunksCompat());
//        }
        if (FabricLoader.getInstance().isModLoaded("common-protection-api")) {
            claimMods.add(new ProtectionApiCompat());
        }

        if (FabricLoader.getInstance().isModLoaded("graveyard")) {
            miscCompatMods.add("graveyard");
        }
        if (FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0")) {
            miscCompatMods.add("permissions");
        }

        YigdCommand.registerCommands();
        ServerPacketReceivers.register();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            YigdConfig config = YigdConfig.getConfig();
            UUID playerId = handler.player.getUuid();
            if (notNotifiedPlayers.contains(playerId)) {
                handler.player.sendMessage(Text.translatable("text.yigd.message.timeout.offline"), false);
                notNotifiedPlayers.remove(playerId);
            }
            if (notNotifiedRobberies.containsKey(playerId)) {
                if (config.graveSettings.graveRobbing.tellRobber) {
                    handler.player.sendMessage(Text.translatable("text.yigd.message.robbed_by.offline", notNotifiedRobberies.get(playerId)), false);
                } else {
                    handler.player.sendMessage(Text.translatable("text.yigd.message.robbed.offline"), false);
                }
                notNotifiedRobberies.remove(playerId);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (!YigdConfig.getConfig().graveSettings.betrayOfflinePeople) return;

            UUID playerId = handler.player.getUuid();
            if (DeathInfoManager.INSTANCE.data.containsKey(playerId)) {
                List<DeadPlayerData> data = DeathInfoManager.INSTANCE.data.get(playerId);
                if (data.size() <= 0) return;

                DeadPlayerData grave = data.get(data.size() - 1);

                if (grave.availability != 1) return;

                BlockPos gravePos = grave.gravePos;
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    player.sendMessage(Text.translatable("text.yigd.message.rob_player_broadcast", gravePos.getY(), gravePos.getY(), gravePos.getZ(), grave.dimensionName));
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            List<Runnable> tickFunctions = new ArrayList<>(NEXT_TICK);
            NEXT_TICK.clear();
            for (Runnable function : tickFunctions) {
                function.run();
            }
        });
    }

    @Override
    public void onInitializeServer() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public void reload(ResourceManager manager) {
                List<Resource> graveResources = manager.getAllResources(new Identifier("yigd", "custom/grave.json"));
                for (Resource resource : graveResources) {
                    try (InputStream stream = resource.getInputStream()) {
                        LOGGER.info("Reloading grave shape (server side)");
                        GraveBlock.reloadVoxelShapes((JsonObject) JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
                    }
                    catch (Exception e) {
                        LOGGER.error("Error occurred while loading custom grave shape (server side)\n" + e);
                    }
                }
            }

            @Override
            public Identifier getFabricId() {
                return new Identifier("yigd", "grave_model");
            }
        });
    }
}