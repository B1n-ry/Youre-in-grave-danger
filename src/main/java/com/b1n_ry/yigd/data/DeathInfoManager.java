package com.b1n_ry.yigd.data;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.util.GraveCompassHelper;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Class that will keep track of all backed up data (graves)
 * Will also keep track of a white/blacklist that can allow/disallow certain people from generating graves
 */
public class DeathInfoManager extends PersistentState {
    public static DeathInfoManager INSTANCE = new DeathInfoManager();

    private final Map<GameProfile, RespawnComponent> respawnEffects = new HashMap<>();
    private final Map<GameProfile, List<GraveComponent>> graveBackups = new HashMap<>();
    private final Map<UUID, GraveComponent> graveMap = new HashMap<>();

    private ListMode graveListMode = ListMode.BLACKLIST;
    private final Set<GameProfile> affectedPlayers = new HashSet<>();

    public void clear() {
        this.respawnEffects.clear();
        this.graveBackups.clear();
        this.graveMap.clear();

        this.affectedPlayers.clear();
    }

    public Set<GameProfile> getAffectedPlayers() {
        return affectedPlayers;
    }

    /**
     * Tries to delete a grave based on its grave ID
     * @param graveId the ID of the grave
     * @return FAIL if nothing were deleted. PASS if it wasn't completely deleted. SUCCESS if it was 100% deleted
     */
    public ActionResult delete(UUID graveId) {
        GraveComponent component = this.graveMap.get(graveId);
        if (component == null) return ActionResult.FAIL;

        GameProfile profile = component.getOwner();

        this.graveMap.remove(graveId);

        // Probably unnecessary, but if it would turn out it's required, people won't crash now
        if (!this.graveBackups.containsKey(profile)) return ActionResult.PASS;  // No more of the grave was found
        this.graveBackups.get(profile).remove(component);

        if (component.getStatus() != GraveStatus.UNCLAIMED) return ActionResult.SUCCESS;

        return component.removeGraveBlock() ? ActionResult.SUCCESS : ActionResult.PASS;
    }

    public void addRespawnComponent(GameProfile profile, RespawnComponent component) {
        this.respawnEffects.put(profile, component);
    }
    public Optional<RespawnComponent> getRespawnComponent(GameProfile profile) {
        return Optional.ofNullable(this.respawnEffects.get(profile));
    }
    public Map<GameProfile, List<GraveComponent>> getPlayerGraves() {
        return this.graveBackups;
    }

    public void removeRespawnComponent(GameProfile profile) {
        this.respawnEffects.remove(profile);
    }

    public void addBackup(GameProfile profile, GraveComponent component) {
        YigdConfig config = YigdConfig.getConfig();

        if (!this.graveBackups.containsKey(profile)) {
            this.graveBackups.put(profile, new ArrayList<>());
        }
        List<GraveComponent> playerGraves = this.graveBackups.get(profile);
        playerGraves.add(component);
        this.graveMap.put(component.getGraveId(), component);

        // If player have too many backed up graves
        if (playerGraves.size() > config.graveConfig.maxBackupsPerPerson) {
            GraveComponent toBeRemoved = playerGraves.get(0);
            this.delete(toBeRemoved.getGraveId());
            if (toBeRemoved.getStatus() == GraveStatus.UNCLAIMED) {
                if (config.graveConfig.dropFromOldestWhenDeleted)
                    toBeRemoved.dropAll();
            }
        }

        if (config.extraFeatures.graveCompass.pointToClosest != YigdConfig.ExtraFeatures.GraveCompassConfig.CompassGraveTarget.DISABLED
                && component.getStatus() == GraveStatus.UNCLAIMED) {
            GraveCompassHelper.addGravePosition(component.getWorldRegistryKey(), component.getPos(), profile.getId());
        }
    }
    public @NotNull List<GraveComponent> getBackupData(GameProfile profile) {
        return this.graveBackups.computeIfAbsent(profile, k -> new ArrayList<>());
    }
    public Optional<GraveComponent> getGrave(UUID graveId) {
        return Optional.ofNullable(this.graveMap.get(graveId));
    }

    public ListMode getGraveListMode() {
        return this.graveListMode;
    }
    public void setGraveListMode(ListMode listMode) {
        this.graveListMode = listMode;
    }
    public void addToList(GameProfile profile) {
        this.affectedPlayers.add(profile);
    }
    public boolean removeFromList(GameProfile profile) {
        return this.affectedPlayers.remove(profile);
    }
    public boolean isInList(GameProfile profile) {
        return this.affectedPlayers.contains(profile);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList respawnNbt = new NbtList();
        NbtList graveNbt = new NbtList();
        NbtCompound graveListNbt = new NbtCompound();
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

        graveListNbt.putString("listMode", this.graveListMode.name());
        NbtList affectedPlayersNbt = new NbtList();
        for (GameProfile profile : this.affectedPlayers) {
            affectedPlayersNbt.add(NbtHelper.writeGameProfile(new NbtCompound(), profile));
        }
        graveListNbt.put("affectedPlayers", affectedPlayersNbt);

        nbt.put("respawns", respawnNbt);
        nbt.put("graves", graveNbt);
        nbt.put("whitelist", graveListNbt);
        return nbt;
    }

    public static PersistentState fromNbt(NbtCompound nbt, MinecraftServer server) {
        INSTANCE.clear();

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
                GraveComponent component = GraveComponent.fromNbt((NbtCompound) grave, server);
                INSTANCE.addBackup(user, component);

                ServerWorld world = component.getWorld();
                if (component.getStatus() == GraveStatus.UNCLAIMED && world != null
                        && world.getBlockEntity(component.getPos()) instanceof GraveBlockEntity be
                        && be.getGraveId() != null
                        && be.getGraveId().equals(component.getGraveId())) {
                    be.setComponent(component);
                }
            }
        }

        NbtCompound graveListNbt = nbt.getCompound("whitelist");
        ListMode listMode = ListMode.valueOf(graveListNbt.getString("listMode"));
        INSTANCE.setGraveListMode(listMode);
        NbtList affectedPlayersNbt = graveListNbt.getList("affectedPlayers", NbtElement.LIST_TYPE);
        for (NbtElement e : affectedPlayersNbt) {
            GameProfile profile = NbtHelper.toGameProfile((NbtCompound) e);
            INSTANCE.addToList(profile);
        }

        return INSTANCE;
    }
}
