package com.b1n_ry.yigd;

import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.block.GraveBlock;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import com.b1n_ry.yigd.compat.InventorioCompat;
import com.b1n_ry.yigd.compat.TrinketsCompat;
import com.b1n_ry.yigd.config.PriorityInventoryConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeathInfoManager;
import com.b1n_ry.yigd.enchantment.DeathSightEnchantment;
import com.b1n_ry.yigd.enchantment.SoulboundEnchantment;
import com.b1n_ry.yigd.core.YigdCommand;
import com.b1n_ry.yigd.item.ScrollItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Yigd implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("YIGD");

    public static List<UUID> notNotifiedPlayers = new ArrayList<>();
    public static List<UUID> notNotifiedRobberies = new ArrayList<>();

    public static Map<UUID, PriorityInventoryConfig> clientPriorities = new HashMap<>();
    public static Map<UUID, PriorityInventoryConfig> clientRobPriorities = new HashMap<>();

    public static final GraveBlock GRAVE_BLOCK = new GraveBlock(FabricBlockSettings.of(Material.STONE).strength(0.8f, 3600000.0f));
    public static BlockEntityType<GraveBlockEntity> GRAVE_BLOCK_ENTITY;

    public static final Enchantment DEATH_SIGHT = new DeathSightEnchantment();

    public static JsonObject graveyard;

    public static Item SCROLL_ITEM = new ScrollItem(new Item.Settings().group(ItemGroup.MISC));

    public static final List<YigdApi> apiMods = new ArrayList<>();
    public static final List<Runnable> NEXT_TICK = new ArrayList<>();

    @Override
    public void onInitialize() {
        AutoConfig.register(YigdConfig.class, Toml4jConfigSerializer::new);

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            @SuppressWarnings("deprecation")
            public void reload(ResourceManager manager) {
                graveyard = null;

                for(Identifier id : manager.findResources("custom", path -> path.equals("graveyard.json"))) {
                    if (!id.getNamespace().equals("yigd")) continue;
                    try (InputStream stream = manager.getResource(id).getInputStream()) {
                        LOGGER.info("Reloading graveyard");
                        graveyard = (JsonObject) JsonParser.parseReader(new InputStreamReader(stream));
                        break;
                    } catch(Exception e) {
                        LOGGER.error("Error occurred while loading resource json " + id + "\n" + e);
                    }
                }
                // Using experimental features so things may or may not cause issues
                try {
                    if (FabricLoader.getInstance() != null) {
                        if (FabricLoader.getInstance().getGameInstance() instanceof MinecraftServer) {
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
                    }
                }
                catch (Exception e) {
                    LOGGER.error("Error occurred while trying to generate server side voxel-shape\n" + e);
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

        if (YigdConfig.getConfig().utilitySettings.soulboundEnchant) {
            // Add the soulbound enchantment if it should be loaded (configured to enable)
            Registry.register(Registry.ENCHANTMENT, new Identifier("yigd", "soulbound"), new SoulboundEnchantment());
        }
        if (YigdConfig.getConfig().utilitySettings.deathSightEnchant) {
            // Add the death sight enchantment if it should be loaded (configured to enable)
            Registry.register(Registry.ENCHANTMENT, new Identifier("yigd", "death_sight"), DEATH_SIGHT);
        }
        if (YigdConfig.getConfig().utilitySettings.teleportScroll) {
            // Add the tp scroll item if it should be loaded (will write an error on world load if not enabled, but this can be ignored)
            Registry.register(Registry.ITEM, new Identifier("yigd", "tp_scroll"), SCROLL_ITEM);
        }

        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            apiMods.add(new TrinketsCompat());
        }
        if (FabricLoader.getInstance().isModLoaded("inventorio")) {
            apiMods.add(new InventorioCompat());
        }
        apiMods.addAll(FabricLoader.getInstance().getEntrypoints("yigd", YigdApi.class));

        YigdCommand.registerCommands();

        ServerPlayNetworking.registerGlobalReceiver(new Identifier("yigd", "config_update"), (server, player, handler, buf, responseSender) -> {
            if (player == null) return;
            PriorityInventoryConfig normalPriority = buf.readEnumConstant(PriorityInventoryConfig.class);
            PriorityInventoryConfig robbingPriority = buf.readEnumConstant(PriorityInventoryConfig.class);

            UUID playerId = player.getUuid();
            clientPriorities.put(playerId, normalPriority);
            clientRobPriorities.put(playerId, robbingPriority);

            LOGGER.info("Priority overwritten for player " + player.getDisplayName().asString() + ". Normal: " + normalPriority.name() + " / Robbing: " + robbingPriority.name());
        });

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world == server.getOverworld()) {
                DeathInfoManager.INSTANCE = (DeathInfoManager) world.getPersistentStateManager().getOrCreate(DeathInfoManager::fromNbt, DeathInfoManager::new, "yigd_grave_data");
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID playerId = handler.player.getUuid();
            if (notNotifiedPlayers.contains(playerId)) {
                handler.player.sendMessage(new TranslatableText("message.yigd.grave.timeout.offline"), false);
                notNotifiedPlayers.remove(playerId);
            }
            if (notNotifiedRobberies.contains(playerId)) {
                handler.player.sendMessage(new TranslatableText("message.yigd.grave.robbed.offline"), false);
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
}