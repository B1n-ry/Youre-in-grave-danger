package com.b1n_ry.yigd.networking;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.component.ResolvableProfile;

public record LightPlayerData(int graveCount, int unclaimedCount, int destroyedCount, ResolvableProfile playerProfile) {
    public static LightPlayerData fromNbt(CompoundTag nbt, HolderLookup.Provider lookupRegistry) {
        if (nbt == null)
            return new LightPlayerData(0, 0, 0, null);

        int graveCount = nbt.getInt("graveCount");
        int unclaimedCount = nbt.getInt("unclaimedCount");
        int destroyedCount = nbt.getInt("destroyedCount");
        ResolvableProfile profile = ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, nbt.get("playerProfile")).result().orElseThrow();

        return new LightPlayerData(graveCount, unclaimedCount, destroyedCount, profile);
    }

    public CompoundTag toNbt(HolderLookup.Provider lookupRegistry) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("graveCount", this.graveCount);
        nbt.putInt("unclaimedCount", this.unclaimedCount);
        nbt.putInt("destroyedCount", this.destroyedCount);
        ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, this.playerProfile).result()
                .ifPresent(nbtElement -> nbt.put("playerProfile", nbtElement));

        return nbt;
    }
}
