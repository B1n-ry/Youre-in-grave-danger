package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.events.DropItemEvent;
import com.mojang.authlib.GameProfile;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RespawnComponent {
    @Nullable
    private InventoryComponent soulboundInventory;
    @Nullable
    private ExpComponent soulboundExp;
    private final EffectComponent respawnEffects;

    public RespawnComponent(ServerPlayerEntity player) {
        this.respawnEffects = new EffectComponent(player);
    }
    private RespawnComponent(@Nullable InventoryComponent soulboundInventory, @Nullable ExpComponent expComponent, EffectComponent effectComponent) {
        this.soulboundInventory = soulboundInventory;
        this.respawnEffects = effectComponent;
        this.soulboundExp = expComponent;
    }

    public void setSoulboundInventory(@NotNull InventoryComponent component) {
        this.soulboundInventory = component;
    }
    public void setSoulboundExp(@NotNull ExpComponent component) {
        this.soulboundExp = component;
    }

    public void primeForRespawn(GameProfile profile) {
        DeathInfoManager.INSTANCE.addRespawnComponent(profile, this);
        DeathInfoManager.INSTANCE.markDirty();
    }

    public void apply(ServerPlayerEntity player) {
        if (this.soulboundInventory != null) {
            DefaultedList<ItemStack> extraItems = this.soulboundInventory.applyToPlayer(player);

            for (ItemStack stack : extraItems) {
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();
                ServerWorld world = player.getServerWorld();
                if (DropItemEvent.EVENT.invoker().shouldDropItem(stack, x, y, z, world))
                    ItemScatterer.spawn(world, x, y, z, stack);
            }
        }

        YigdConfig.ExtraFeatures extraFeaturesConfig = YigdConfig.getConfig().extraFeatures;
        if (extraFeaturesConfig.deathScroll.enabled && extraFeaturesConfig.deathScroll.receiveOnRespawn) {
            ItemStack scroll = Yigd.DEATH_SCROLL_ITEM.getDefaultStack();
            boolean turned = Yigd.DEATH_SCROLL_ITEM.bindStackToLatestDeath(player, scroll);
            if (turned)
                player.giveItemStack(scroll);
        }
        if (extraFeaturesConfig.graveKeys.enabled && extraFeaturesConfig.graveKeys.receiveOnRespawn) {
            ItemStack key = Yigd.GRAVE_KEY_ITEM.getDefaultStack();
            boolean turned = Yigd.GRAVE_KEY_ITEM.bindStackToLatestGrave(player, key);
            if (turned)
                player.giveItemStack(key);
        }

        if (this.soulboundExp != null)
            this.soulboundExp.applyToPlayer(player);

        this.respawnEffects.applyToPlayer(player);

        // If there is an issue, items don't get duped
        DeathInfoManager.INSTANCE.removeRespawnComponent(player.getGameProfile());
        DeathInfoManager.INSTANCE.markDirty();
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();

        if (this.soulboundInventory != null) nbt.put("inventory", this.soulboundInventory.toNbt());
        if (this.soulboundExp != null) nbt.put("exp", this.soulboundExp.toNbt());
        nbt.put("effects", this.respawnEffects.toNbt());

        return nbt;
    }

    public static RespawnComponent fromNbt(NbtCompound nbt) {
        InventoryComponent soulboundInventory = null;
        if (nbt.contains("inventory")) {
            NbtCompound inventoryNbt = nbt.getCompound("inventory");
            soulboundInventory = InventoryComponent.fromNbt(inventoryNbt);
        }

        ExpComponent expComponent = null;
        if (nbt.contains("exp")) {
            NbtCompound expNbt = nbt.getCompound("exp");
            expComponent = ExpComponent.fromNbt(expNbt);
        }

        NbtCompound effectsNbt = nbt.getCompound("effects");
        EffectComponent effectComponent = EffectComponent.fromNbt(effectsNbt);

        return new RespawnComponent(soulboundInventory, expComponent, effectComponent);
    }
}
