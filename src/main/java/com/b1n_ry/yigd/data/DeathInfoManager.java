package com.b1n_ry.yigd.data;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.util.GraveCompassHelper;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Class that will keep track of all backed up data (graves)
 * Will also keep track of a white/blacklist that can allow/disallow certain people from generating graves
 */
public class DeathInfoManager extends SavedData {
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

    public static SavedData.Factory<DeathInfoManager> getPersistentStateType(MinecraftServer server) {
        return new SavedData.Factory<>(DeathInfoManager::new, (nbt, lookupRegistry) -> DeathInfoManager.load(nbt, lookupRegistry, server), null);
    }

    /**
     * Tries to delete a grave based on its grave ID
     * @param graveId the ID of the grave
     * @return FAIL if nothing were deleted. PASS if it wasn't completely deleted. SUCCESS if it was 100% deleted
     */
    public InteractionResult delete(UUID graveId) {
        GraveComponent component = this.graveMap.get(graveId);
        if (component == null) return InteractionResult.FAIL;

        GameProfile profile = component.getOwner().gameProfile();

        this.graveMap.remove(graveId);

        // Probably unnecessary, but if it would turn out it's required, people won't crash now
        if (!this.graveBackups.containsKey(profile)) return InteractionResult.PASS;  // No more of the grave was found
        this.graveBackups.get(profile).remove(component);

        if (component.getStatus() != GraveStatus.UNCLAIMED) return InteractionResult.SUCCESS;

        return component.removeGraveBlock() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    public void addRespawnComponent(ResolvableProfile profile, RespawnComponent component) {
        this.respawnEffects.put(profile.gameProfile(), component);
    }
    public Optional<RespawnComponent> getRespawnComponent(ResolvableProfile profile) {
        return Optional.ofNullable(this.respawnEffects.get(profile.gameProfile()));
    }
    public Map<GameProfile, List<GraveComponent>> getPlayerGraves() {
        return this.graveBackups;
    }

    public void removeRespawnComponent(ResolvableProfile profile) {
        this.respawnEffects.remove(profile.gameProfile());
    }

    public void addBackup(ResolvableProfile p, GraveComponent component) {
        YigdConfig config = YigdConfig.getConfig();

        GameProfile profile = p.gameProfile();
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
            GraveCompassHelper.addGravePosition(component.getWorldRegistryKey(), component.getPos(), profile.getId());
        }
    }
    public @NotNull List<GraveComponent> getBackupData(ResolvableProfile profile) {
        return this.graveBackups.computeIfAbsent(profile.gameProfile(), k -> new ArrayList<>());
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
    public void addToList(ResolvableProfile profile) {
        this.affectedPlayers.add(profile.gameProfile());
    }
    public boolean removeFromList(ResolvableProfile profile) {
        return this.affectedPlayers.remove(profile.gameProfile());
    }
    public boolean isInList(ResolvableProfile profile) {
        return this.affectedPlayers.contains(profile.gameProfile());
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider registryLookup) {
        ListTag respawnNbt = new ListTag();
        ListTag graveNbt = new ListTag();
        CompoundTag graveListNbt = new CompoundTag();
        for (Map.Entry<GameProfile, RespawnComponent> entry : this.respawnEffects.entrySet()) {
            CompoundTag respawnCompound = new CompoundTag();

            ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, new ResolvableProfile(entry.getKey())).result()
                    .ifPresent(nbtElement -> respawnCompound.put("user", nbtElement));
            respawnCompound.put("component", entry.getValue().toNbt(registryLookup));

            respawnNbt.add(respawnCompound);
        }

        for (Map.Entry<GameProfile, List<GraveComponent>> entry : this.graveBackups.entrySet()) {
            CompoundTag graveCompound = new CompoundTag();
            ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, new ResolvableProfile(entry.getKey())).result()
                    .ifPresent(nbtElement -> graveCompound.put("user", nbtElement));

            ListTag graveNbtList = new ListTag();
            for (GraveComponent graveComponent : entry.getValue()) {
                graveNbtList.add(graveComponent.toNbt(registryLookup));
            }

            graveCompound.put("graves", graveNbtList);

            graveNbt.add(graveCompound);
        }

        graveListNbt.putString("listMode", this.graveListMode.name());
        ListTag affectedPlayersNbt = new ListTag();
        for (GameProfile profile : this.affectedPlayers) {
            Tag profileNbt = ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, new ResolvableProfile(profile)).result().orElseThrow();
            affectedPlayersNbt.add(profileNbt);
        }
        graveListNbt.put("affectedPlayers", affectedPlayersNbt);

        nbt.put("respawns", respawnNbt);
        nbt.put("graves", graveNbt);
        nbt.put("whitelist", graveListNbt);
        return nbt;
    }

    public static DeathInfoManager load(CompoundTag nbt, HolderLookup.Provider lookupRegistry, MinecraftServer server) {
        INSTANCE.clear();

        ListTag respawnNbt = nbt.getList("respawns", Tag.TAG_COMPOUND);
        ListTag graveNbt = nbt.getList("graves", Tag.TAG_COMPOUND);
        for (Tag respawnElement : respawnNbt) {
            CompoundTag respawnCompound = (CompoundTag) respawnElement;
            INSTANCE.addRespawnComponent(
                    ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, respawnCompound.get("user")).result().orElseThrow(),
                    RespawnComponent.fromNbt(respawnCompound.getCompound("component"), lookupRegistry));
        }
        for (Tag graveElement : graveNbt) {
            CompoundTag graveCompound = (CompoundTag) graveElement;
            ResolvableProfile user = ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, graveCompound.get("user")).result().orElseThrow();
            ListTag gravesList = graveCompound.getList("graves", Tag.TAG_COMPOUND);
            for (Tag grave : gravesList) {
                GraveComponent component = GraveComponent.fromNbt((CompoundTag) grave, lookupRegistry, server);
                INSTANCE.addBackup(user, component);

                // If the grave is still in the world, set the component
                ServerLevel world = component.getWorld();
                if (world != null && world.areEntitiesLoaded(new ChunkPos(component.getPos()).toLong())
                        && world.getBlockEntity(component.getPos()) instanceof GraveBlockEntity be
                        && be.getGraveId() != null
                        && be.getGraveId().equals(component.getGraveId())) {
                    be.setComponent(component);
                }
            }
        }

        CompoundTag graveListNbt = nbt.getCompound("whitelist");
        ListMode listMode = ListMode.valueOf(graveListNbt.getString("listMode"));
        INSTANCE.setGraveListMode(listMode);
        ListTag affectedPlayersNbt = graveListNbt.getList("affectedPlayers", Tag.TAG_LIST);
        for (Tag e : affectedPlayersNbt) {
            ResolvableProfile profile = ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, e).result().orElseThrow();
            INSTANCE.addToList(profile);
        }

        return INSTANCE;
    }
}
