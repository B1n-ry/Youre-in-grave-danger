package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.data.DeathInfoManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public class RespawnComponent {
    private InventoryComponent soulboundInventory = null;
    private ExpComponent soulboundExp = null;

    public RespawnComponent() {
    }
    private RespawnComponent(InventoryComponent soulboundInventory) {
        this.soulboundInventory = soulboundInventory;
    }

    public void setSoulboundInventory(InventoryComponent component) {
        this.soulboundInventory = component;
    }
    public void setSoulboundExp(ExpComponent component) {
        this.soulboundExp = component;
    }

    public void primeForRespawn(GameProfile profile) {
        DeathInfoManager.INSTANCE.addRespawnComponent(profile, this);
        DeathInfoManager.INSTANCE.markDirty();
    }

    public void apply(ServerPlayerEntity player) {
        if (this.soulboundInventory != null)
            this.soulboundInventory.applyToPlayer(player);

        if (this.soulboundExp != null)
            this.soulboundExp.applyToPlayer(player);

        // If there is an issue, items don't get duped
        DeathInfoManager.INSTANCE.removeRespawnComponent(player.getGameProfile());
        DeathInfoManager.INSTANCE.markDirty();
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.put("inventory", this.soulboundInventory.toNbt());
        return nbt;
    }

    public static RespawnComponent fromNbt(NbtCompound nbt) {
        NbtCompound inventoryNbt = nbt.getCompound("inventory");
        InventoryComponent soulboundInventory = InventoryComponent.fromNbt(inventoryNbt);
        return new RespawnComponent(soulboundInventory);
    }
}
