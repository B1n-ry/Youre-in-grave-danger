package com.b1n_ry.yigd.data;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.util.GraveCompassHelper;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Class that will keep track of all backed up data (graves)
 * Will also keep track of a white/blacklist that can allow/disallow certain people from generating graves
 */
public class DeathInfoManager extends PersistentState {
    public static DeathInfoManager INSTANCE = new DeathInfoManager();

    private final Map<ProfileComponent, RespawnComponent> respawnEffects = new HashMap<>();
    private final Map<ProfileComponent, List<GraveComponent>> graveBackups = new HashMap<>();
    private final Map<UUID, GraveComponent> graveMap = new HashMap<>();

    private ListMode graveListMode = ListMode.BLACKLIST;
    private final Set<ProfileComponent> affectedPlayers = new HashSet<>();

    public void clear() {
        this.respawnEffects.clear();
        this.graveBackups.clear();
        this.graveMap.clear();

        this.affectedPlayers.clear();
    }

    public Set<ProfileComponent> getAffectedPlayers() {
        return affectedPlayers;
    }

    public static PersistentState.Type<DeathInfoManager> getPersistentStateType(MinecraftServer server) {
        return new PersistentState.Type<>(DeathInfoManager::new, (nbt, lookupRegistry) -> DeathInfoManager.fromNbt(nbt, lookupRegistry, server), null);
    }

    /**
     * Tries to delete a grave based on its grave ID
     * @param graveId the ID of the grave
     * @return FAIL if nothing were deleted. PASS if it wasn't completely deleted. SUCCESS if it was 100% deleted
     */
    public ActionResult delete(UUID graveId) {
        GraveComponent component = this.graveMap.get(graveId);
        if (component == null) return ActionResult.FAIL;

        ProfileComponent profile = component.getOwner();

        this.graveMap.remove(graveId);

        // Probably unnecessary, but if it would turn out it's required, people won't crash now
        if (!this.graveBackups.containsKey(profile)) return ActionResult.PASS;  // No more of the grave was found
        this.graveBackups.get(profile).remove(component);

        if (component.getStatus() != GraveStatus.UNCLAIMED) return ActionResult.SUCCESS;

        return component.removeGraveBlock() ? ActionResult.SUCCESS : ActionResult.PASS;
    }

    public void addRespawnComponent(ProfileComponent profile, RespawnComponent component) {
        this.respawnEffects.put(profile, component);
    }
    public Optional<RespawnComponent> getRespawnComponent(ProfileComponent profile) {
        return Optional.ofNullable(this.respawnEffects.get(profile));
    }
    public Map<ProfileComponent, List<GraveComponent>> getPlayerGraves() {
        return this.graveBackups;
    }

    public void removeRespawnComponent(ProfileComponent profile) {
        this.respawnEffects.remove(profile);
    }

    public void addBackup(ProfileComponent profile, GraveComponent component) {
        YigdConfig config = YigdConfig.getConfig();

        if (!this.graveBackups.containsKey(profile)) {
            this.graveBackups.put(profile, new ArrayList<>());
        }
        List<GraveComponent> playerGraves = this.graveBackups.get(profile);
        playerGraves.add(component);
        this.graveMap.put(component.getGraveId(), component);

        // If player have too many backed up graves
        if (playerGraves.size() > config.graveConfig.maxBackupsPerPerson) {
            GraveComponent toBeRemoved = playerGraves.getFirst();
            this.delete(toBeRemoved.getGraveId());
            if (toBeRemoved.getStatus() == GraveStatus.UNCLAIMED) {
                if (config.graveConfig.dropFromOldestWhenDeleted)
                    toBeRemoved.dropAll();
            }
        }

        if (config.extraFeatures.graveCompass.pointToClosest != YigdConfig.ExtraFeatures.GraveCompassConfig.CompassGraveTarget.DISABLED
                && component.getStatus() == GraveStatus.UNCLAIMED) {
            GraveCompassHelper.addGravePosition(component.getWorldRegistryKey(), component.getPos(), profile.id().orElse(null));
        }
    }
    public @NotNull List<GraveComponent> getBackupData(ProfileComponent profile) {
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
    public void addToList(ProfileComponent profile) {
        this.affectedPlayers.add(profile);
    }
    public boolean removeFromList(ProfileComponent profile) {
        return this.affectedPlayers.remove(profile);
    }
    public boolean isInList(ProfileComponent profile) {
        return this.affectedPlayers.contains(profile);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList respawnNbt = new NbtList();
        NbtList graveNbt = new NbtList();
        NbtCompound graveListNbt = new NbtCompound();
        for (Map.Entry<ProfileComponent, RespawnComponent> entry : this.respawnEffects.entrySet()) {
            NbtCompound respawnCompound = new NbtCompound();

            ProfileComponent.CODEC.encodeStart(NbtOps.INSTANCE, entry.getKey()).result()
                    .ifPresent(nbtElement -> respawnCompound.put("user", nbtElement));
            respawnCompound.put("component", entry.getValue().toNbt(registryLookup));

            respawnNbt.add(respawnCompound);
        }

        for (Map.Entry<ProfileComponent, List<GraveComponent>> entry : this.graveBackups.entrySet()) {
            NbtCompound graveCompound = new NbtCompound();
            ProfileComponent.CODEC.encodeStart(NbtOps.INSTANCE, entry.getKey()).result()
                    .ifPresent(nbtElement -> graveCompound.put("user", nbtElement));

            NbtList graveNbtList = new NbtList();
            for (GraveComponent graveComponent : entry.getValue()) {
                graveNbtList.add(graveComponent.toNbt(registryLookup));
            }

            graveCompound.put("graves", graveNbtList);

            graveNbt.add(graveCompound);
        }

        graveListNbt.putString("listMode", this.graveListMode.name());
        NbtList affectedPlayersNbt = new NbtList();
        for (ProfileComponent profile : this.affectedPlayers) {
            NbtElement profileNbt = ProfileComponent.CODEC.encodeStart(NbtOps.INSTANCE, profile).result().orElseThrow();
            affectedPlayersNbt.add(profileNbt);
        }
        graveListNbt.put("affectedPlayers", affectedPlayersNbt);

        nbt.put("respawns", respawnNbt);
        nbt.put("graves", graveNbt);
        nbt.put("whitelist", graveListNbt);
        return nbt;
    }

    public static DeathInfoManager fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookupRegistry, MinecraftServer server) {
        INSTANCE.clear();

        NbtList respawnNbt = nbt.getList("respawns", NbtElement.COMPOUND_TYPE);
        NbtList graveNbt = nbt.getList("graves", NbtElement.COMPOUND_TYPE);
        for (NbtElement respawnElement : respawnNbt) {
            NbtCompound respawnCompound = (NbtCompound) respawnElement;
            INSTANCE.addRespawnComponent(
                    ProfileComponent.CODEC.parse(NbtOps.INSTANCE, respawnCompound.get("user")).result().orElseThrow(),
                    RespawnComponent.fromNbt(respawnCompound.getCompound("component"), lookupRegistry));
        }
        for (NbtElement graveElement : graveNbt) {
            NbtCompound graveCompound = (NbtCompound) graveElement;
            ProfileComponent user = ProfileComponent.CODEC.parse(NbtOps.INSTANCE, graveCompound.get("user")).result().orElseThrow();
            NbtList gravesList = graveCompound.getList("graves", NbtElement.COMPOUND_TYPE);
            for (NbtElement grave : gravesList) {
                GraveComponent component = GraveComponent.fromNbt((NbtCompound) grave, lookupRegistry, server);
                INSTANCE.addBackup(user, component);

                // If the grave is still in the world, set the component
                ServerWorld world = component.getWorld();
                if (world != null && world.isChunkLoaded(new ChunkPos(component.getPos()).toLong())
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
            ProfileComponent profile = ProfileComponent.CODEC.parse(NbtOps.INSTANCE, e).result().orElseThrow();
            INSTANCE.addToList(profile);
        }

        return INSTANCE;
    }
}
