package com.b1n_ry.yigd.core;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.PersistentState;

import java.util.*;

public class DeathInfoManager extends PersistentState {
    public static DeathInfoManager INSTANCE = new DeathInfoManager();

    public Map<UUID, List<DeadPlayerData>> data = new HashMap<>();

    public DeathInfoManager() {
        super("yigd");
    }

    @Override
    public void fromTag(NbtCompound tag) {
        DeathInfoManager.INSTANCE = new DeathInfoManager();

        Map<UUID, List<DeadPlayerData>> deadData = new HashMap<>();
        NbtList userList = tag.getList("yigd_grave_data", NbtType.COMPOUND);
        for (NbtElement e : userList) {
            if (!(e instanceof NbtCompound)) continue;
            NbtCompound c = (NbtCompound) e;
            UUID uuid = c.getUuid("UUID");
            NbtList graveList = c.getList("Deaths", NbtType.COMPOUND);

            List<DeadPlayerData> deathList = new ArrayList<>();
            for (NbtElement eDeath : graveList) {
                if (!(eDeath instanceof NbtCompound)) continue;
                NbtCompound cDeath = (NbtCompound) eDeath;
                DeadPlayerData deadPlayerData = DeadPlayerData.fromNbt(cDeath);

                deathList.add(deadPlayerData);
            }

            deadData.put(uuid, deathList);
        }
        NbtCompound soulboundNbt = tag.getCompound("soulbound_items");
        DeadPlayerData.Soulbound.fromNbt(soulboundNbt);

        INSTANCE.data = deadData;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound graveData = new NbtCompound();
        NbtList userList = new NbtList();

        INSTANCE.data.forEach((uuid, deadPlayerData) -> {
            NbtCompound userValue = new NbtCompound();

            NbtList deathList = new NbtList();
            for (DeadPlayerData data : deadPlayerData) {
                deathList.add(data.toNbt());
            }

            userValue.putUuid("UUID", uuid);
            userValue.put("Deaths", deathList);

            userList.add(userValue);
        });
        graveData.put("yigd_grave_data", userList);
        graveData.put("soulbound_items", DeadPlayerData.Soulbound.getNbt());
        return graveData;
    }
}