package com.b1n_ry.yigd.data;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DeathInfoManager extends PersistentState {
    public static DeathInfoManager INSTANCE = new DeathInfoManager(null);

    private final Map<GameProfile, RespawnComponent> respawnEffects = new HashMap<>();
    private final Map<GameProfile, List<GraveComponent>> graveBackups = new HashMap<>();
    private final Map<UUID, GraveComponent> graveMap = new HashMap<>();

    private MinecraftServer server;

    public DeathInfoManager(MinecraftServer server) {
        this.server = server;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public void clear() {
        this.respawnEffects.clear();
        this.graveBackups.clear();
        this.graveMap.clear();
    }

    public void addRespawnComponent(GameProfile profile, RespawnComponent component) {
        this.respawnEffects.put(profile, component);
    }
    public RespawnComponent getRespawnComponent(GameProfile profile) {
        return this.respawnEffects.get(profile);
    }

    public void removeRespawnComponent(GameProfile profile) {
        this.respawnEffects.remove(profile);
    }

    public void addBackup(GameProfile profile, GraveComponent component) {
        this.graveBackups.get(profile).add(component);
        this.graveMap.put(component.getGraveId(), component);
    }
    public List<GraveComponent> getBackupData(GameProfile profile) {
        return this.graveBackups.get(profile);
    }
    public GraveComponent getComponent(UUID uuid) {
        return this.graveMap.get(uuid);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList respawnNbt = new NbtList();
        NbtList graveNbt = new NbtList();
        for (Map.Entry<GameProfile, RespawnComponent> entry : this.respawnEffects.entrySet()) {
            NbtCompound respawnCompound = new NbtCompound();

            respawnCompound.put("user", NbtHelper.writeGameProfile(new NbtCompound(), entry.getKey()));
            respawnCompound.put("component", entry.getValue().toNbt());

            respawnNbt.add(respawnCompound);
        }

        for (Map.Entry<GameProfile, List<GraveComponent>> entry : this.graveBackups.entrySet()) {
            NbtCompound graveCompound = new NbtCompound();
            graveCompound.put("user", NbtHelper.writeGameProfile(new NbtCompound(), entry.getKey()));

            NbtList graveNbtList = new NbtList();
            for (GraveComponent graveComponent : entry.getValue()) {
                graveNbtList.add(graveComponent.toNbt());
            }

            graveCompound.put("graves", graveNbtList);

            graveNbt.add(graveCompound);
        }

        nbt.put("respawns", respawnNbt);
        nbt.put("graves", graveNbt);
        return nbt;
    }

    public static PersistentState fromNbt(NbtCompound nbt, MinecraftServer server) {
        INSTANCE.clear();
        INSTANCE.setServer(server);

        NbtList respawnNbt = nbt.getList("respawns", NbtElement.COMPOUND_TYPE);
        NbtList graveNbt = nbt.getList("graves", NbtElement.COMPOUND_TYPE);
        for (NbtElement respawnElement : respawnNbt) {
            NbtCompound respawnCompound = (NbtCompound) respawnElement;
            INSTANCE.addRespawnComponent(NbtHelper.toGameProfile(respawnCompound.getCompound("user")), RespawnComponent.fromNbt(respawnCompound.getCompound("component")));
        }
        for (NbtElement graveElement : graveNbt) {
            NbtCompound graveCompound = (NbtCompound) graveElement;
            GameProfile user = NbtHelper.toGameProfile(graveCompound.getCompound("user"));
            NbtList gravesList = graveCompound.getList("graves", NbtElement.COMPOUND_TYPE);
            for (NbtElement grave : gravesList) {
                INSTANCE.addBackup(user, GraveComponent.fromNbt((NbtCompound) grave, server));
            }
        }

        return INSTANCE;
    }
}
