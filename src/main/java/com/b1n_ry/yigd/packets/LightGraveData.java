package com.b1n_ry.yigd.packets;

import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.data.TranslatableDeathMessage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * This class is used to carry a representation from an overhaul of what's in a grave, not a detailed description
 * @param itemCount Amount of items in total on the player
 * @param pos {@link BlockPos} where the player died
 * @param xpPoints How much XP the player had
 * @param registryKey {@link RegistryKey<World>} of the world player died in
 * @param deathMessage {@link TranslatableDeathMessage} of the player's death message
 * @param id The grave {@link UUID}
 * @param status The availability status of the grave
 */
public record LightGraveData(int itemCount, BlockPos pos, int xpPoints, RegistryKey<World> registryKey,
                             TranslatableDeathMessage deathMessage, UUID id, GraveStatus status) {

    public static LightGraveData fromNbt(NbtCompound nbt) {
        int itemCount = nbt.getInt("itemCount");
        BlockPos pos = NbtHelper.toBlockPos(nbt.getCompound("pos"));
        int xpPoints = nbt.getInt("xpPoints");
        RegistryKey<World> registryKey = getRegistryKeyFromNbt(nbt.getCompound("worldKey"));
        TranslatableDeathMessage deathMessage = TranslatableDeathMessage.fromNbt(nbt.getCompound("deathMessage"));
        UUID id = nbt.getUuid("id");
        GraveStatus status = GraveStatus.valueOf(nbt.getString("status"));

        return new LightGraveData(itemCount, pos, xpPoints, registryKey, deathMessage, id, status);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("itemCount", this.itemCount);
        nbt.put("pos", NbtHelper.fromBlockPos(this.pos));
        nbt.putInt("xpPoints", this.xpPoints);
        nbt.put("worldKey", this.getWorldRegistryKeyNbt(this.registryKey));
        nbt.put("deathMessage", this.deathMessage.toNbt());
        nbt.putUuid("id", this.id);
        nbt.putString("status", this.status.toString());

        return nbt;
    }

    private NbtCompound getWorldRegistryKeyNbt(RegistryKey<?> key) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("registry", key.getRegistry().toString());
        nbt.putString("value", key.getValue().toString());

        return nbt;
    }

    private static RegistryKey<World> getRegistryKeyFromNbt(NbtCompound nbt) {
        String registry = nbt.getString("registry");
        String value = nbt.getString("value");

        RegistryKey<Registry<World>> r = RegistryKey.ofRegistry(new Identifier(registry));
        return RegistryKey.of(r, new Identifier(value));
    }
}
