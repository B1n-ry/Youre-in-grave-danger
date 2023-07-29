package com.b1n_ry.yigd.packets;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;

public record LightPlayerData(int graveCount, int unclaimedCount, GameProfile playerProfile) {
    public static LightPlayerData fromNbt(NbtCompound nbt) {
        int graveCount = nbt.getInt("graveCount");
        int unclaimedCount = nbt.getInt("unclaimedCount");
        GameProfile profile = NbtHelper.toGameProfile(nbt.getCompound("playerProfile"));

        return new LightPlayerData(graveCount, unclaimedCount, profile);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("graveCount", this.graveCount);
        nbt.putInt("unclaimedCount", this.unclaimedCount);
        nbt.put("playerProfile", NbtHelper.writeGameProfile(new NbtCompound(), this.playerProfile));

        return nbt;
    }
}
