package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            ServerWorld world = player.getServerWorld();
            for (ItemStack stack : extraItems) {
                InventoryComponent.dropItemIfToBeDropped(stack, x, y, z, world);
            }
        }

        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.ExtraFeatures extraFeaturesConfig = config.extraFeatures;
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
        if (extraFeaturesConfig.graveCompass.receiveOnRespawn) {
            ItemStack compass = Items.COMPASS.getDefaultStack();
            NbtCompound compassNbt = new NbtCompound();

            List<GraveComponent> playerGraves = DeathInfoManager.INSTANCE.getBackupData(player.getGameProfile());
            GraveComponent latestGrave = playerGraves.get(playerGraves.size() - 1);

            compassNbt.putUuid("linked_grave", latestGrave.getGraveId());  // Speed up the process of identifying the grave server side

            // Make clients read the grave position
            compassNbt.put("grave_pos", NbtHelper.fromBlockPos(latestGrave.getPos()));
            World.CODEC.encodeStart(NbtOps.INSTANCE, latestGrave.getWorldRegistryKey()).resultOrPartial(Yigd.LOGGER::error).ifPresent(worldNbt -> compassNbt.put("grave_dimension", worldNbt));

            compass.setNbt(compassNbt);
            player.giveItemStack(compass);
        }

        for (YigdConfig.RespawnConfig.ExtraItemDrop extraItemDrop : config.respawnConfig.extraItemDrops) {
            Item item = Registries.ITEM.get(new Identifier(extraItemDrop.itemId()));
            ItemStack stack = new ItemStack(item, extraItemDrop.count());
            try {
                stack.setNbt(NbtHelper.fromNbtProviderString(extraItemDrop.itemNbt()));

                player.giveItemStack(stack);
            }
            catch (CommandSyntaxException e) {
                Yigd.LOGGER.error("Could not give an item with NBT to player on respawn. Invalid NBT");
            }
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
