package com.b1n_ry.yigd;

import com.b1n_ry.yigd.api.ClaimModsApi;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.block.GraveBlock;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import com.b1n_ry.yigd.compat.*;
import com.b1n_ry.yigd.config.PriorityInventoryConfig;
import com.b1n_ry.yigd.config.ScrollTypeConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeathInfoManager;
import com.b1n_ry.yigd.core.GraveAreaOverride;
import com.b1n_ry.yigd.core.ServerPacketReceivers;
import com.b1n_ry.yigd.core.YigdCommand;
import com.b1n_ry.yigd.enchantment.DeathSightEnchantment;
import com.b1n_ry.yigd.enchantment.SoulboundEnchantment;
import com.b1n_ry.yigd.item.KeyItem;
import com.b1n_ry.yigd.item.ScrollItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
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
import net.minecraft.item.ItemGroup;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Yigd implements ModInitializer, DedicatedServerModInitializer, ServerLifecycleEvents.ServerStarted, ServerLifecycleEvents.ServerStopped {
    public static final Logger LOGGER = LoggerFactory.getLogger("YIGD");

    public static List<UUID> notNotifiedPlayers = new ArrayList<>();
    public static List<UUID> notNotifiedRobberies = new ArrayList<>();

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
            public void reload(ResourceManager manager) {
                graveyard = null;

                for (Identifier id : manager.findResources("custom", path -> path.equals("graveyard.json"))) {
                    if (!id.getNamespace().equals("yigd")) continue;
                    try (InputStream stream = manager.getResource(id).getInputStream()) {
                        LOGGER.info("Reloading graveyard");
                        graveyard = (JsonObject) JsonParser.parseReader(new InputStreamReader(stream));
                        break;
                    } catch(Exception e) {
                        LOGGER.error("Error occurred while loading resource json " + id + "\n" + e);
                    }
                }

                for (Identifier id : manager.findResources("custom", path -> path.equals("grave_areas.json"))) {
                    if (!id.getNamespace().equals("yigd")) continue;
                    try (InputStream stream = manager.getResource(id).getInputStream()) {
                        LOGGER.info("Reloading custom grave areas");
                        GraveAreaOverride.reloadGraveAreas((JsonObject) JsonParser.parseReader(new InputStreamReader(stream)));
                        break;
                    }
                    catch (Exception e) {
                        LOGGER.error("Error occurred while loading custom grave areas\n" + e);
                    }
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
                GraveBlock.customModel = null;

                Collection<Identifier> ids = manager.findResources("models/block", path -> path.equals("grave.json"));

                for (Identifier id : ids) {
                    if (!id.getNamespace().equals("yigd")) continue;
                    try (InputStream stream = manager.getResource(id).getInputStream()) {
                        LOGGER.info("Reloading grave model (client)");
                        GraveBlock.customModel = (JsonObject) JsonParser.parseReader(new InputStreamReader(stream));
                        GraveBlock.reloadVoxelShapes(GraveBlock.customModel);
                        GraveBlockEntityRenderer.reloadCustomModel();
                        break;
                    } catch (Exception e) {
                        LOGGER.error("Error occurred while loading custom grave model " + id + "\n" + e);
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

        YigdConfig.UtilitySettings utilityConfig = YigdConfig.getConfig().utilitySettings;
        if (utilityConfig.soulboundEnchant.enabled) {
            // Add the soulbound enchantment if it should be loaded (configured to enable)
            Registry.register(Registry.ENCHANTMENT, new Identifier("yigd", "soulbound"), new SoulboundEnchantment());
        }
        if (utilityConfig.deathSightEnchant.enabled) {
            // Add the death sight enchantment if it should be loaded (configured to enable)
            DEATH_SIGHT = new DeathSightEnchantment();
            Registry.register(Registry.ENCHANTMENT, new Identifier("yigd", "death_sight"), DEATH_SIGHT);
        }
        if (utilityConfig.scrollItem.scrollType != ScrollTypeConfig.DISABLED) {
            // Add the scroll item if it should be loaded (will write an error on world load if not enabled, but this can be ignored)
            SCROLL_ITEM = new ScrollItem(new Item.Settings().group(ItemGroup.MISC));
            Registry.register(Registry.ITEM, new Identifier("yigd", "death_scroll"), SCROLL_ITEM);
        }
        if (utilityConfig.graveKeySettings.enableKeys) {
            // Add the grave key item if it should be loaded
            KEY_ITEM = new KeyItem(new Item.Settings().group(ItemGroup.MISC));
            Registry.register(Registry.ITEM, new Identifier("yigd", "grave_key"), KEY_ITEM);
        }

        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            apiMods.add(new TrinketsCompat());
        }
        if (FabricLoader.getInstance().isModLoaded("levelz")) {
            apiMods.add(new LevelzCompat());
        }
        if (FabricLoader.getInstance().isModLoaded("inventorio")) {
            apiMods.add(new InventorioCompat());
        }
        if (FabricLoader.getInstance().isModLoaded("travelersbackpack")) {
            apiMods.add(new TravelersBackpackCompat());
        }
        apiMods.addAll(FabricLoader.getInstance().getEntrypoints("yigd", YigdApi.class));

        if (FabricLoader.getInstance().isModLoaded("flan")) {
            claimMods.add(new FlanCompat());
        }
        if (FabricLoader.getInstance().isModLoaded("ftbchunks")) {
            claimMods.add(new FtbChunksCompat());
        }
        if (FabricLoader.getInstance().isModLoaded("goml")) {
            claimMods.add(new GomlCompat());
        }

        if (FabricLoader.getInstance().isModLoaded("graveyard")) {
            miscCompatMods.add("graveyard");
        }
        if (FabricLoader.getInstance().isModLoaded("fabric-permissions-api")) {
            miscCompatMods.add("permissions");
        }

        YigdCommand.registerCommands();
        ServerPacketReceivers.register();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID playerId = handler.player.getUuid();
            if (notNotifiedPlayers.contains(playerId)) {
                handler.player.sendMessage(new TranslatableText("text.yigd.message.timeout.offline"), false);
                notNotifiedPlayers.remove(playerId);
            }
            if (notNotifiedRobberies.contains(playerId)) {
                handler.player.sendMessage(new TranslatableText("text.yigd.message.robbed.offline"), false);
                notNotifiedRobberies.remove(playerId);
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
                for (Identifier id : manager.findResources("custom", path -> path.equals("grave.json"))) {
                    if (!id.getNamespace().equals("yigd")) continue;
                    try (InputStream stream = manager.getResource(id).getInputStream()) {
                        LOGGER.info("Reloading grave shape (server side)");
                        GraveBlock.reloadVoxelShapes((JsonObject) JsonParser.parseReader(new InputStreamReader(stream)));
                        break;
                    } catch (Exception e) {
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

    @Override
    public void onServerStarted(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        if (world == null) { // If for some reason the overworld is not loaded
            for (ServerWorld serverWorld : server.getWorlds()) {
                world = serverWorld;
                break;
            }
        }
        if (world == null) return; // If for some reason there's no world loaded
        DeathInfoManager.INSTANCE = (DeathInfoManager) world.getPersistentStateManager().getOrCreate(DeathInfoManager::fromNbt, DeathInfoManager::new, "yigd_grave_data");
        LOGGER.info("Loaded data from grave data file");
    }

    @Override
    public void onServerStopped(MinecraftServer server) {
        DeathInfoManager.INSTANCE = null;
    }
}