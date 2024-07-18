package com.b1n_ry.yigd.networking;

import net.minecraft.component.type.ProfileComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;

public record LightPlayerData(int graveCount, int unclaimedCount, int destroyedCount, ProfileComponent playerProfile) {
    public static LightPlayerData fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookupRegistry) {
        if (nbt == null)
            return new LightPlayerData(0, 0, 0, null);

        int graveCount = nbt.getInt("graveCount");
        int unclaimedCount = nbt.getInt("unclaimedCount");
        int destroyedCount = nbt.getInt("destroyedCount");
        ProfileComponent profile = ProfileComponent.CODEC.parse(NbtOps.INSTANCE, nbt.get("playerProfile")).result().orElseThrow();

        return new LightPlayerData(graveCount, unclaimedCount, destroyedCount, profile);
    }

    public NbtCompound toNbt(RegistryWrapper.WrapperLookup lookupRegistry) {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("graveCount", this.graveCount);
        nbt.putInt("unclaimedCount", this.unclaimedCount);
        nbt.putInt("destroyedCount", this.destroyedCount);
        ProfileComponent.CODEC.encodeStart(NbtOps.INSTANCE, this.playerProfile).result()
                .ifPresent(nbtElement -> nbt.put("playerProfile", nbtElement));

        return nbt;
    }
}
