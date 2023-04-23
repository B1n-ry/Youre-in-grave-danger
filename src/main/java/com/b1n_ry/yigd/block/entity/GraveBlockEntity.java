package com.b1n_ry.yigd.block.entity;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.OnDestroyedDrop;
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
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GraveBlockEntity extends BlockEntity {
    private GameProfile graveOwner;
    private int storedXp;
    private String customName;
    private DefaultedList<ItemStack> storedInventory;
    private Map<String, Object> moddedInventories;
    private UUID killer;
    public long creationTime;
    private UUID graveId;

    private GameProfile graveSkull;

    private BlockState previousState;
    private boolean claimed;

    private boolean glowing;

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(Yigd.GRAVE_BLOCK_ENTITY, pos, state);

        this.graveOwner = null;
        this.storedXp = 0;
        this.storedInventory = DefaultedList.ofSize(41, ItemStack.EMPTY);

        this.creationTime = world != null ? world.getTime() : 0;
        this.graveSkull = null;

        this.glowing = YigdConfig.getConfig().graveSettings.graveRenderSettings.glowingGrave;
        this.previousState = Blocks.AIR.getDefaultState();
        this.claimed = false;

        this.graveId = UUID.randomUUID();
    }

    @Override
    public void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);

        tag.putInt("StoredXp", storedXp);
        tag.put("Items", Inventories.writeNbt(new NbtCompound(), this.storedInventory, true));
        tag.putInt("ItemCount", this.storedInventory.size());
        tag.putLong("creationTime", this.creationTime);
        tag.put("replaceState", NbtHelper.fromBlockState(this.previousState));
        tag.putBoolean("claimed", this.claimed);
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

                Yigd.NEXT_TICK.add(() -> {
                    OnDestroyedDrop onDestroyedDrop = savedConfig.graveSettings.onDestroyedDrop;
                    if (!this.claimed && onDestroyedDrop != OnDestroyedDrop.NONE) {
                        if (onDestroyedDrop == OnDestroyedDrop.ITEM_CONTENTS) {
                            DefaultedList<ItemStack> itemsToDrop = DefaultedList.of();

                            itemsToDrop.addAll(this.storedInventory);

                            for (YigdApi yigdApi : Yigd.apiMods) {
                                Object inventory = this.moddedInventories.get(yigdApi.getModName());
                                itemsToDrop.addAll(yigdApi.toStackList(inventory));
                            }
                            ItemScatterer.spawn(this.world, this.pos, itemsToDrop);
                        } else if (onDestroyedDrop == OnDestroyedDrop.GRAVE_BLOCK) {
                            ItemStack stack = Yigd.GRAVE_BLOCK.asItem().getDefaultStack();
                            NbtCompound itemNbt = new NbtCompound();
                            NbtCompound blockNbt = new NbtCompound();
                            this.writeNbt(blockNbt);
                            itemNbt.put("BlockEntityTag", blockNbt);
                            stack.setNbt(itemNbt);

                            ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                        }
                    }
                });
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
        } else {
            if (nbt == null) {
                nbt = new NbtCompound();
            }
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
        this.creationTime = tag.contains("creationTime") ? tag.getLong("creationTime") : 0;
        this.previousState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), tag.getCompound("replaceState"));
        this.claimed = tag.contains("claimed") && tag.getBoolean("claimed");
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
                if (nbt == null) continue;

                this.moddedInventories.computeIfAbsent(modName, s -> yigdApi.readNbt(nbt));
            }
        }

        // Not from NBT. Static definitions
        this.glowing = YigdConfig.getConfig().graveSettings.graveRenderSettings.glowingGrave;

        this.markDirty();
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = this.createNbt();
        nbt.putBoolean("claimed", this.claimed);
        return nbt;
    }

    private static YigdConfig savedConfig = YigdConfig.getConfig();
    public static void tick(World world, BlockPos pos, BlockState ignoredState, BlockEntity blockEntity) {
        if (!(blockEntity instanceof GraveBlockEntity grave)) return;
        if (world == null || world.isClient) return;
        if (grave.getGraveOwner() == null) return;

        if ((int) world.getTime() % 2400 == 0) savedConfig = YigdConfig.getConfig(); // Every two minutes the config will be updated. This is so there won't be any lag if the getConfig method is demanding to run

        YigdConfig.GraveDeletion deletion = savedConfig.graveSettings.graveDeletion;
        if (!deletion.canDelete || grave.claimed) return;

        if (grave.creationTime == 0) grave.creationTime = world.getTime();
        boolean timeHasPassed = grave.creationTime + (long) deletion.afterTime * deletion.timeType.tickFactor() <= world.getTime();

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
        Yigd.LOGGER.info("Grave at %d %d %d expired".formatted(pos.getX(), pos.getY(), pos.getZ()));
        if (world.getServer() == null || grave.graveOwner == null) return;
        ServerPlayerEntity graveOwner = world.getServer().getPlayerManager().getPlayer(grave.graveOwner.getId());
        if (graveOwner == null) {
            Yigd.notNotifiedPlayers.add(grave.graveOwner.getId());
            return;
        }
        graveOwner.sendMessage(Text.translatable("text.yigd.message.timeout" + (deletion.dropInventory ? ".dropped" : "")), false);
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
    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
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
    public boolean isClaimed() {
        return this.claimed;
    }
}
