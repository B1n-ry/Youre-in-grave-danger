package com.b1n_ry.yigd.networking;

import com.b1n_ry.yigd.data.GraveStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * This class is used to carry a representation from an overhaul of what's in a grave, not a detailed description
 * @param itemCount Amount of items in total on the player
 * @param pos {@link BlockPos} where the player died
 * @param xpPoints How much XP the player had
 * @param registryKey {@link ResourceKey<Level>} of the world player died in
 * @param deathMessage {@link Component} of the player's death message
 * @param id The grave {@link UUID}
 * @param status The availability status of the grave
 */
public record LightGraveData(int itemCount, BlockPos pos, int xpPoints, ResourceKey<Level> registryKey,
                             Component deathMessage, UUID id, GraveStatus status) {

    public static LightGraveData fromNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        if (nbt == null) {
            return new LightGraveData(0, BlockPos.ZERO, 0, Level.OVERWORLD, Component.empty(), UUID.randomUUID(), GraveStatus.CLAIMED);
        }
        int itemCount = nbt.getInt("itemCount");
        BlockPos pos = NbtUtils.readBlockPos(nbt, "pos").orElse(BlockPos.ZERO);
        int xpPoints = nbt.getInt("xpPoints");
        ResourceKey<Level> registryKey = getRegistryKeyFromNbt(nbt.getCompound("worldKey"));
        Component deathMessage = Component.Serializer.fromJson(nbt.getString("deathMessage"), registryLookup);
        UUID id = nbt.getUUID("id");
        GraveStatus status = GraveStatus.valueOf(nbt.getString("status"));

        return new LightGraveData(itemCount, pos, xpPoints, registryKey, deathMessage, id, status);
    }

    public CompoundTag toNbt(HolderLookup.Provider registryLookup) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("itemCount", this.itemCount);
        nbt.put("pos", NbtUtils.writeBlockPos(this.pos));
        nbt.putInt("xpPoints", this.xpPoints);
        nbt.put("worldKey", this.getWorldRegistryKeyNbt(this.registryKey));
        nbt.putString("deathMessage", Component.Serializer.toJson(this.deathMessage, registryLookup));
        nbt.putUUID("id", this.id);
        nbt.putString("status", this.status.toString());

        return nbt;
    }

    private CompoundTag getWorldRegistryKeyNbt(ResourceKey<?> key) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("registry", key.registry().toString());
        nbt.putString("value", key.location().toString());

        return nbt;
    }

    private static ResourceKey<Level> getRegistryKeyFromNbt(CompoundTag nbt) {
        String registry = nbt.getString("registry");
        String value = nbt.getString("value");

        ResourceKey<Registry<Level>> r = ResourceKey.createRegistryKey(ResourceLocation.parse(registry));
        return ResourceKey.create(r, ResourceLocation.parse(value));
    }
}
