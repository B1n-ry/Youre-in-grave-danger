package com.b1n_ry.yigd;

import com.b1n_ry.yigd.block.GraveBlock;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.compat.InvModCompat;
import com.b1n_ry.yigd.config.ClaimPriority;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.events.ServerEventHandler;
import com.b1n_ry.yigd.events.YigdServerEventHandler;
import com.b1n_ry.yigd.item.DeathScrollItem;
import com.b1n_ry.yigd.item.GraveKeyItem;
import com.b1n_ry.yigd.networking.PacketInitializer;
import com.b1n_ry.yigd.util.YigdCommands;
import com.b1n_ry.yigd.util.YigdResourceHandler;
import com.mojang.serialization.MapCodec;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.*;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.*;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Yigd.MOD_ID)
public class Yigd
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "yigd";

    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Yigd.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Yigd.MOD_ID);

    public static final DeferredRegister<MapCodec<? extends Block>> BLOCK_REGISTER = DeferredRegister.create(BuiltInRegistries.BLOCK_TYPE, Yigd.MOD_ID);
    public static final DeferredHolder<MapCodec<? extends Block>, MapCodec<GraveBlock>> GRAVE_BLOCK_CODEC = BLOCK_REGISTER.register(
            "grave",
            () -> BlockBehaviour.simpleCodec(GraveBlock::new));

    public static final DeferredBlock<Block> GRAVE = BLOCKS.registerBlock("grave", GraveBlock::new,
            BlockBehaviour.Properties.of().strength(0.8f, 3600000.0f).noOcclusion());

    public static final DeferredItem<BlockItem> GRAVE_ITEM = ITEMS.registerSimpleBlockItem("grave", GRAVE);
    public static final DeferredItem<DeathScrollItem> DEATH_SCROLL_ITEM = ITEMS.registerItem("death_scroll", DeathScrollItem::new, new Item.Properties());
    public static final DeferredItem<GraveKeyItem> GRAVE_KEY_ITEM = ITEMS.registerItem("grave_key", GraveKeyItem::new, new Item.Properties());

    public static final DeferredRegister<BlockEntityType<?>> BE_REGISTER = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Yigd.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GraveBlockEntity>> GRAVE_BLOCK_ENTITY = BE_REGISTER.register("grave_block_entity", () -> BlockEntityType.Builder.of(GraveBlockEntity::new, GRAVE.get()).build(null));

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Yigd.MOD_ID);
    public static final Supplier<AttachmentType<Vec3>> LAST_GROUND_POS = ATTACHMENT_TYPES.register("last_ground_pos", () -> AttachmentType.builder(() -> Vec3.ZERO).serialize(Vec3.CODEC).build());

    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Yigd.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> GRAVE_ID = DATA_COMPONENTS.registerComponentType("grave_id", builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> GRAVE_LOCATION = DATA_COMPONENTS.registerComponentType("grave_location", builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));
    public static final DeathHandler DEATH_HANDLER = new DeathHandler();

    /**
     * Any runnable added to this list will be executed on the end of the current server tick.
     * Use if runnable is required to run before some other event that would have otherwise ran before.
     */
    public static List<Runnable> END_OF_TICK = new ArrayList<>();

    public static Map<UUID, List<String>> NOT_NOTIFIED_ROBBERIES = new HashMap<>();
    public static Map<UUID, ClaimPriority> CLAIM_PRIORITIES = new HashMap<>();
    public static Map<UUID, ClaimPriority> ROB_PRIORITIES = new HashMap<>();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Yigd(IEventBus modEventBus, ModContainer modContainer)
    {
        AutoConfig.register(YigdConfig.class, GsonConfigSerializer::new);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::modInitializer);

        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so block entities get registered
        BE_REGISTER.register(modEventBus);
        // Register the Deferred Register to the mod event bus so attachments get registered
        ATTACHMENT_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so data component types get registered
        DATA_COMPONENTS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(new ServerEventHandler());
        NeoForge.EVENT_BUS.register(new YigdServerEventHandler());

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(PacketInitializer::register);
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, e -> InvModCompat.reloadModCompat());
        NeoForge.EVENT_BUS.addListener(YigdCommands::registerCommands);
        NeoForge.EVENT_BUS.addListener(YigdResourceHandler::serverDataEvent);
    }

    private void modInitializer(final FMLCommonSetupEvent event)
    {
        InvModCompat.reloadModCompat();
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
            event.accept(GRAVE_ITEM);
    }
}
