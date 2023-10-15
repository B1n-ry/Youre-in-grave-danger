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
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class GraveBlockEntity extends BlockEntity {
    @Nullable
    private GraveComponent component = null;
    @Nullable
    private UUID graveId = null;
    @Nullable
    private GameProfile graveOwner = null;
    @Nullable
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
    public void setPreviousState(@Nullable BlockState previousState) {
        this.previousState = previousState;
    }

    public @Nullable UUID getGraveId() {
        return this.graveId;
    }
    public @Nullable GameProfile getGraveOwner() {
        return this.graveOwner;
    }
    public @Nullable GraveComponent getComponent() {
        return this.component;
    }
    public @Nullable BlockState getPreviousState() {
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

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        if (this.component == null) return;
        nbt.putUuid("graveId", this.graveId);
        if (this.previousState != null) nbt.put("previousState", NbtHelper.fromBlockState(this.previousState));
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        if (nbt.contains("owner"))  // Only for the client
            this.graveOwner = NbtHelper.toGameProfile(nbt.getCompound("owner"));

        if (this.world != null && this.world.isClient) return;

        if (nbt.contains("graveId")) {
            this.graveId = nbt.getUuid("graveId");
            if (this.component == null) {
                DeathInfoManager.INSTANCE.getGrave(this.graveId).ifPresent(this::setComponent);
            }
        }

        if (nbt.contains("previousState")) {
            RegistryWrapper<Block> registryEntryLookup = this.world != null ? this.world.createCommandRegistryWrapper(RegistryKeys.BLOCK) : Registries.BLOCK.getReadOnlyWrapper();
            this.previousState = NbtHelper.toBlockState(registryEntryLookup, nbt.getCompound("previousState"));
        }
    }

    public static void tick(World world, BlockPos blockPos, BlockState ignoredState, GraveBlockEntity be) {
        if (world.isClient) return;

        if (be.component == null) return;
        if (world.getTime() % 2400 == 0) cachedConfig = YigdConfig.getConfig();  // Reloads the config every 60 seconds

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
