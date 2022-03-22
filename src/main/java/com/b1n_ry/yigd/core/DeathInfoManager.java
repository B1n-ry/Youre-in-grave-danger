package com.b1n_ry.yigd.core;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.PersistentState;

import java.util.*;

public class DeathInfoManager extends PersistentState {
    public static DeathInfoManager INSTANCE = new DeathInfoManager();

    public Map<UUID, List<DeadPlayerData>> data = new HashMap<>();

    public static DeadPlayerData findUserGrave(UUID userId, UUID graveId) {
        if (INSTANCE.data == null || !INSTANCE.data.containsKey(userId)) return null;
        for (DeadPlayerData data : INSTANCE.data.get(userId)) {
            if (data.id.equals(graveId)) return data;
        }
        return null;
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

    public static PersistentState fromNbt(NbtCompound nbt) {
        DeathInfoManager.INSTANCE = new DeathInfoManager();

        Map<UUID, List<DeadPlayerData>> deadData = new HashMap<>();
        NbtList userList = nbt.getList("yigd_grave_data", NbtElement.COMPOUND_TYPE);
        for (NbtElement e : userList) {
            if (!(e instanceof NbtCompound c)) continue;
            UUID uuid = c.getUuid("UUID");
            NbtList graveList = c.getList("Deaths", NbtElement.COMPOUND_TYPE);

            List<DeadPlayerData> deathList = new ArrayList<>();
            for (NbtElement eDeath : graveList) {
                if (!(eDeath instanceof NbtCompound cDeath)) continue;
                DeadPlayerData deadPlayerData = DeadPlayerData.fromNbt(cDeath);

                deathList.add(deadPlayerData);
            }

            deadData.put(uuid, deathList);
        }
        NbtCompound soulboundNbt = nbt.getCompound("soulbound_items");
        DeadPlayerData.Soulbound.fromNbt(soulboundNbt);

        INSTANCE.data = deadData;

        return INSTANCE;
    }
}