package com.b1n_ry.yigd.block.entity;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

public class GraveBlockEntity extends BlockEntity {
    private GraveComponent component = null;
    private UUID graveId = null;
    private GameProfile graveOwner = null;
    private BlockState previousState = null;

    private static YigdConfig cachedConfig = YigdConfig.getConfig();

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(Yigd.GRAVE_BLOCK_ENTITY, pos, state);
    }

    public void setComponent(GraveComponent component) {
        this.component = component;
        this.graveOwner = component.getOwner();
        this.graveId = component.getGraveId();
        this.markDirty();
    }
    public void setPreviousState(BlockState previousState) {
        this.previousState = previousState;
    }

    public UUID getGraveId() {
        return this.graveId;
    }
    public GameProfile getGraveOwner() {
        return this.graveOwner;
    }
    public GraveComponent getComponent() {
        return this.component;
    }
    public BlockState getPreviousState() {
        return this.previousState;
    }

    public void onBroken() {
        if (this.world == null || this.world.isClient) return;

        Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(this.graveId);
        component.ifPresent(grave -> {
            if (grave.getStatus() == GraveStatus.UNCLAIMED)
                grave.setStatus(GraveStatus.DESTROYED);
        });
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        if (this.component == null) return super.toInitialChunkDataNbt();
        NbtCompound nbt = this.createNbt();
        nbt.put("owner", NbtHelper.writeGameProfile(new NbtCompound(), this.component.getOwner()));
        return nbt;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        if (this.component == null) return;
        nbt.putUuid("graveId", this.graveId);
        if (this.previousState != null) nbt.put("previousState", NbtHelper.fromBlockState(this.previousState));
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        if (nbt.contains("owner"))  // Only on the client
            this.graveOwner = NbtHelper.toGameProfile(nbt.getCompound("owner"));

        if (this.world != null && this.world.isClient) return;

        this.graveId = nbt.getUuid("graveId");
        if (this.component == null && this.graveId != null) {
            DeathInfoManager.INSTANCE.getGrave(this.graveId).ifPresent(this::setComponent);
        }

        if (nbt.contains("previousState")) {
            RegistryWrapper<Block> registryEntryLookup = this.world != null ? this.world.createCommandRegistryWrapper(RegistryKeys.BLOCK) : Registries.BLOCK.getReadOnlyWrapper();
            this.previousState = NbtHelper.toBlockState(registryEntryLookup, nbt.getCompound("previousState"));
        }
    }

    public static void tick(World world, BlockPos blockPos, BlockState ignoredState, GraveBlockEntity be) {
        if (world.isClient) return;

        if (world.getTime() % 2400 == 0) cachedConfig = YigdConfig.getConfig();

        YigdConfig.GraveConfig.GraveTimeout timeoutConfig = cachedConfig.graveConfig.graveTimeout;

        if (!timeoutConfig.enabled || be.component.getStatus() != GraveStatus.UNCLAIMED) return;

        long timePassed = world.getTime() - be.component.getCreationTime();
        final int ticksPerSecond = 20;
        if (timeoutConfig.timeUnit.toSeconds(timeoutConfig.afterTime) * ticksPerSecond <= timePassed) {
            BlockState newState = Blocks.AIR.getDefaultState();
            if (cachedConfig.graveConfig.replaceOldWhenClaimed) {
                newState = be.previousState;
            }
            world.setBlockState(blockPos, newState);

            if (timeoutConfig.dropContentsOnTimeout) {
                be.component.dropAll();
            }
        }
    }
}
