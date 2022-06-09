package com.b1n_ry.yigd.block.entity;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.b1n_ry.yigd.core.DeathInfoManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class GraveBlockEntity extends BlockEntity {
    private GameProfile graveOwner;
    private int storedXp;
    private String customName;
    private DefaultedList<ItemStack> storedInventory;
    private Map<String, Object> moddedInventories;
    private UUID killer;
    public int age;
    private UUID graveId;

    private GameProfile graveSkull;

    private BlockState previousState;

    private boolean glowing;

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }
    public GraveBlockEntity(String customName, BlockPos pos, BlockState state) {
        super(Yigd.GRAVE_BLOCK_ENTITY, pos, state);

        this.graveOwner = null;
        this.storedXp = 0;
        this.customName = customName;
        this.storedInventory = DefaultedList.ofSize(41, ItemStack.EMPTY);

        this.age = 0;
        this.graveSkull = null;

        this.glowing = YigdConfig.getConfig().graveSettings.graveRenderSettings.glowingGrave;
        this.previousState = Blocks.AIR.getDefaultState();

        this.graveId = UUID.randomUUID();
    }

    @Override
    public void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);

        tag.putInt("StoredXp", storedXp);
        tag.put("Items", Inventories.writeNbt(new NbtCompound(), this.storedInventory, true));
        tag.putInt("ItemCount", this.storedInventory.size());
        tag.putLong("age", this.age);
        tag.put("replaceState", NbtHelper.fromBlockState(this.previousState));
        tag.putUuid("graveId", this.graveId);

        if (this.graveOwner != null) tag.put("owner", NbtHelper.writeGameProfile(new NbtCompound(), this.graveOwner));
        if (this.customName != null) tag.putString("CustomName", this.customName);
        if (this.graveSkull != null) tag.put("skull", NbtHelper.writeGameProfile(new NbtCompound(), this.graveSkull));
        if (this.killer != null) tag.putUuid("killer", this.killer);

        if (this.moddedInventories != null) {
            NbtCompound modNbt = new NbtCompound();
            for (YigdApi yigdApi : Yigd.apiMods) {
                String modName = yigdApi.getModName();
                if (modName == null || !this.moddedInventories.containsKey(modName)) continue;
                modNbt.put(modName, yigdApi.writeNbt(this.moddedInventories.get(modName)));
            }
            tag.put("ModdedInventoryItems", modNbt);
        }
    }

    public void onBroken() {
        if (world != null && !world.isClient) {
            if (this.graveOwner != null) {
                DeadPlayerData data = DeathInfoManager.findUserGrave(this.graveOwner.getId(), this.graveId);
                if (data != null && data.availability == 1) data.availability = -1;
                DeathInfoManager.INSTANCE.markDirty();
            } else if (this.graveSkull != null) {
                dropCosmeticSkull();
            }
        }
        super.markRemoved();
    }

    public void dropCosmeticSkull() {
        ItemStack stack = Items.PLAYER_HEAD.getDefaultStack();
        NbtCompound nbt = stack.getNbt();
        if (this.graveSkull.getId() != null) {
            stack.setSubNbt("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), this.graveSkull));
        } else if (nbt != null) {
            nbt.putString("SkullOwner", this.graveSkull.getName());
            stack.writeNbt(nbt);
        }
        if (world == null) return;
        ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack);
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);

        this.storedInventory = DefaultedList.ofSize(tag.getInt("ItemCount"), ItemStack.EMPTY);

        Inventories.readNbt(tag.getCompound("Items"), this.storedInventory);

        this.storedXp = tag.getInt("StoredXp");
        this.age = tag.getInt("age");
        this.previousState = NbtHelper.toBlockState(tag.getCompound("replaceState"));
        this.graveId = tag.getUuid("graveId");

        if (tag.contains("owner")) this.graveOwner = NbtHelper.toGameProfile(tag.getCompound("owner"));
        if (tag.contains("CustomName")) this.customName = tag.getString("CustomName");
        if (tag.contains("skull")) this.graveSkull = NbtHelper.toGameProfile(tag.getCompound("skull"));
        if (tag.contains("killer")) this.killer = tag.getUuid("killer");

        if (tag.contains("ModdedInventoryItems")) {
            this.moddedInventories = new HashMap<>();
            NbtCompound modNbt = tag.getCompound("ModdedInventoryItems");

            for (YigdApi yigdApi : Yigd.apiMods) {
                String modName = yigdApi.getModName();

                NbtCompound nbt = modNbt.getCompound(modName);
                this.moddedInventories.computeIfAbsent(modName, s -> yigdApi.readNbt(nbt));
            }
        }

        // Not from NBT. Static definitions
        this.glowing = YigdConfig.getConfig().graveSettings.graveRenderSettings.glowingGrave;
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    public static void tick(World world, BlockPos pos, BlockState ignoredState, BlockEntity blockEntity) {
        if (!(blockEntity instanceof GraveBlockEntity grave)) return;
        if (world == null || world.isClient) return;
        grave.age++;
        if (grave.getGraveOwner() == null) return;

        YigdConfig.GraveDeletion deletion = YigdConfig.getConfig().graveSettings.graveDeletion;
        if (!deletion.canDelete) return;

        boolean timeHasPassed = grave.age > deletion.afterTime * deletion.timeType.tickFactor();

        if (!timeHasPassed) return;
        if (deletion.dropInventory) {
            int xp = grave.getStoredXp();
            DefaultedList<ItemStack> dropItems = DefaultedList.of();
            for (ItemStack stack : grave.getStoredInventory()) {
                if (stack.isEmpty()) continue;
                dropItems.add(stack);
            }
            for (YigdApi yigdApi : Yigd.apiMods) {
                Object o = grave.moddedInventories.get(yigdApi.getModName());
                dropItems.addAll(yigdApi.toStackList(o));
            }
            if (!dropItems.isEmpty()) {
                ItemScatterer.spawn(world, pos, dropItems);
            }
            if (xp > 0 && world instanceof ServerWorld sWorld) {
                ExperienceOrbEntity.spawn(sWorld, Vec3d.of(pos), xp);
            }
        }
        world.removeBlock(pos, false);
        if (world.getServer() == null || grave.graveOwner == null) return;
        ServerPlayerEntity graveOwner = world.getServer().getPlayerManager().getPlayer(grave.graveOwner.getId());
        if (graveOwner == null) {
            Yigd.notNotifiedPlayers.add(grave.graveOwner.getId());
            return;
        }
        graveOwner.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.timeout" + (deletion.dropInventory ? ".dropped" : ""))), MessageType.SYSTEM);
    }

    public void setGraveOwner(GameProfile owner) {
        this.graveOwner = owner;
    }
    public void setStoredXp(int xp) {
        this.storedXp = xp;
    }
    public void setCustomName(String name) {
        this.customName = name;
    }
    public void setInventory(DefaultedList<ItemStack> inventory) {
        this.storedInventory = inventory;
    }
    public void setModdedInventories(Map<String, Object> inventories) {
        this.moddedInventories = inventories;
    }
    public void setKiller(UUID killerId) {
        this.killer = killerId;
    }
    public void setPreviousState(BlockState state) {
        this.previousState = state;
    }
    public void setGraveSkull(GameProfile skullOwner) {
        this.graveSkull = skullOwner;
    }

    public GameProfile getGraveOwner() {
        return this.graveOwner;
    }
    public String getCustomName() {
        return customName;
    }
    public DefaultedList<ItemStack> getStoredInventory() {
        return storedInventory;
    }
    public int getStoredXp() {
        return storedXp;
    }
    public Map<String, Object> getModdedInventories() {
        return moddedInventories;
    }
    public UUID getKiller() {
        return this.killer;
    }
    public boolean canGlow() {
        return glowing;
    }
    public BlockState getPreviousState() {
        return this.previousState;
    }
    public UUID getGraveId() {
        return this.graveId;
    }
    public GameProfile getGraveSkull() {
        return this.graveSkull;
    }
}
