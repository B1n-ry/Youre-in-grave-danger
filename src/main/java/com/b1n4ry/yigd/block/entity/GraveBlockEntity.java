package com.b1n4ry.yigd.block.entity;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.config.YigdConfig;
import com.mojang.authlib.GameProfile;
import me.shedaniel.clothconfig2.api.TickableWidget;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class GraveBlockEntity extends BlockEntity implements TickableWidget {
    private GameProfile graveOwner;
    private int storedXp;
    private String customName;
    private DefaultedList<ItemStack> storedInventory;
    private DefaultedList<ItemStack> moddedInventories;
    private UUID killer;
    private long createdAt;

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }
    public GraveBlockEntity(String customName, BlockPos pos, BlockState state) {
        super(Yigd.GRAVE_BLOCK_ENTITY, pos, state);

        this.graveOwner = null;
        this.storedXp = 0;
        this.customName = customName;
        this.storedInventory = DefaultedList.ofSize(41, ItemStack.EMPTY);

        if (world != null) {
            this.createdAt = world.getTimeOfDay();
        } else {
            this.createdAt = 0;
        }
    }

    @Override
    public void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);

        tag.putInt("StoredXp", storedXp);
        tag.put("Items", Inventories.writeNbt(new NbtCompound(), this.storedInventory, true));
        tag.putInt("ItemCount", this.storedInventory.size());
        tag.putLong("CreatedAt", this.createdAt);

        if (graveOwner != null) tag.put("owner", NbtHelper.writeGameProfile(new NbtCompound(), this.graveOwner));
        if (customName != null) tag.putString("CustomName", customName);
        if (killer != null) tag.putUuid("killer", killer);

        if (moddedInventories != null) {
            tag.put("ModdedInventoryItems", Inventories.writeNbt(new NbtCompound(), this.moddedInventories, true));
        }
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);

        this.storedInventory = DefaultedList.ofSize(tag.getInt("ItemCount"), ItemStack.EMPTY);

        Inventories.readNbt(tag.getCompound("Items"), this.storedInventory);

        this.storedXp = tag.getInt("StoredXp");
        this.createdAt = tag.getLong("CreatedAt");

        if (tag.contains("owner")) this.graveOwner = NbtHelper.toGameProfile(tag.getCompound("owner"));
        if (tag.contains("CustomName")) this.customName = tag.getString("CustomName");
        if (tag.contains("killer")) this.killer = tag.getUuid("killer");

        if (tag.contains("ModdedInventoryItems")) {
            int modInvSize = tag.getCompound("ModdedInventoryItems").getList("Items", 10).size();
            this.moddedInventories = DefaultedList.ofSize(modInvSize, ItemStack.EMPTY);

            Inventories.readNbt(tag.getCompound("ModdedInventoryItems"), this.moddedInventories);
        }
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    @Override
    public void tick() {
        YigdConfig.GraveDeletion deletion = YigdConfig.getConfig().graveSettings.graveDeletion;
        if (!deletion.canDelete) return;

        if (world == null) return;
        boolean timeHasPassed = (int) (world.getTimeOfDay() - createdAt) > deletion.afterTime * deletion.timeType.tickFactor();

        if (timeHasPassed) world.removeBlock(this.getPos(), false);
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
    public void setModdedInventories(DefaultedList<ItemStack> inventories) {
        this.moddedInventories = inventories;
    }
    public void setKiller(UUID killerId) {
        this.killer = killerId;
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
    public DefaultedList<ItemStack> getModdedInventories() {
        return moddedInventories;
    }
    public UUID getKiller() {
        return this.killer;
    }
    public long getCreationTime() {
        return createdAt;
    }
}
