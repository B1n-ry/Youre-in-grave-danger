package com.b1n_ry.yigd.data;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.*;

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

        BlockPos pos = component.getPos();
        ServerWorld world = component.getWorld();
        if (world == null) return ActionResult.PASS;
        BlockEntity be = world.getBlockEntity(pos);

        if (!(be instanceof GraveBlockEntity grave)) return ActionResult.PASS;
        world.setBlockState(pos, grave.getPreviousState());

        return ActionResult.SUCCESS;
    }

    public void addRespawnComponent(GameProfile profile, RespawnComponent component) {
        this.respawnEffects.put(profile, component);
    }
    public Optional<RespawnComponent> getRespawnComponent(GameProfile profile) {
        return Optional.ofNullable(this.respawnEffects.get(profile));
    }

    public void removeRespawnComponent(GameProfile profile) {
        this.respawnEffects.remove(profile);
    }

    public void addBackup(GameProfile profile, GraveComponent component) {
        if (!this.graveBackups.containsKey(profile)) {
            this.graveBackups.put(profile, new ArrayList<>());
        }
        this.graveBackups.get(profile).add(component);
        this.graveMap.put(component.getGraveId(), component);
    }
    public List<GraveComponent> getBackupData(GameProfile profile) {
        return this.graveBackups.computeIfAbsent(profile, k -> new ArrayList<>());
    }
    public Optional<GraveComponent> getGrave(UUID graveId) {
        return Optional.ofNullable(this.graveMap.get(graveId));
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
                GraveComponent component = GraveComponent.fromNbt((NbtCompound) grave, server);
                INSTANCE.addBackup(user, component);

                ServerWorld world = component.getWorld();
                if (component.getStatus() == GraveStatus.UNCLAIMED && world != null && world.getBlockEntity(component.getPos()) instanceof GraveBlockEntity be) {
                    be.setComponent(component);
                }
            }
        }

        return INSTANCE;
    }
}
