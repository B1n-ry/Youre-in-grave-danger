package com.b1n_ry.yigd.block.entity;

import com.b1n_ry.yigd.Yigd;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.collection.DefaultedList;

public class GraveBlockEntity extends BlockEntity implements BlockEntityClientSerializable {
    private GameProfile graveOwner;
    private int storedXp;
    private String customName;
    private DefaultedList<ItemStack> storedInventory;

    public GraveBlockEntity() {
        this(null);
    }
    public GraveBlockEntity(String customName) {
        super(Yigd.GRAVE_BLOCK_ENTITY);

        this.graveOwner = null;
        this.storedXp = 0;
        this.customName = customName;
        this.storedInventory = DefaultedList.ofSize(41, ItemStack.EMPTY);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        super.writeNbt(tag);

        tag.putInt("StoredXp", storedXp);
        tag.put("Items", Inventories.writeNbt(new NbtCompound(), this.storedInventory, true));
        tag.putInt("ItemCount", this.storedInventory.size());

        if (graveOwner != null) tag.put("owner", NbtHelper.writeGameProfile(new NbtCompound(), this.graveOwner));
        if (customName != null) tag.putString("CustomName", customName);

        return tag;
    }

    @Override
    public void fromTag(BlockState state, NbtCompound tag) {
        super.fromTag(state, tag);

        this.storedInventory = DefaultedList.ofSize(tag.getInt("ItemCount"), ItemStack.EMPTY);

        Inventories.readNbt(tag.getCompound("Items"), this.storedInventory);

        this.storedXp = tag.getInt("StoredXp");

        if(tag.contains("owner")) this.graveOwner = NbtHelper.toGameProfile(tag.getCompound("owner"));
        if(tag.contains("CustomName")) this.customName = tag.getString("CustomName");
    }

    @Override
    public void fromClientTag(NbtCompound tag) {
        if(tag.contains("owner")) this.graveOwner = NbtHelper.toGameProfile(tag.getCompound("owner"));
        if(tag.contains("CustomName")) this.customName = tag.getString("CustomName");
    }

    @Override
    public NbtCompound toClientTag(NbtCompound tag) {
        if (graveOwner != null) tag.put("owner", NbtHelper.writeGameProfile(new NbtCompound(), this.graveOwner));
        if (customName != null) tag.putString("CustomName", customName);
        return tag;
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


    public GameProfile getGraveOwner() {
        return this.graveOwner;
    }
    public String getCustomName() {
        return customName;
    }
    public DefaultedList<ItemStack> getStoredInventory() {
        return storedInventory;
    }
    public int getStoredXp () {
        return storedXp;
    }
}
