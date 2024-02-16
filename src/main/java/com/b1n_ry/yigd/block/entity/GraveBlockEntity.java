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
import net.minecraft.text.Text;
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
    private GameProfile graveSkull = null;
    @Nullable
    private Text graveText = null;
    @Nullable
    private BlockState previousState = null;

    private boolean claimed = false;

    private static YigdConfig cachedConfig = YigdConfig.getConfig();

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(Yigd.GRAVE_BLOCK_ENTITY, pos, state);
    }

    public void setComponent(GraveComponent component) {
        this.component = component;
        this.graveSkull = component.getOwner();
        this.graveId = component.getGraveId();
        // noinspection ConstantConditions
        this.graveText = Text.of(this.graveSkull.getName());  // graveSkull is never null in this case as it's not nullable in the grave component
        this.markDirty();
    }
    public void setPreviousState(@Nullable BlockState previousState) {
        this.previousState = previousState;
    }
    public void setGraveText(@Nullable Text text) {
        this.graveText = text;
    }

    public @Nullable UUID getGraveId() {
        return this.graveId;
    }
    public @Nullable GameProfile getGraveSkull() {
        return this.graveSkull;
    }
    public void setGraveSkull(@Nullable GameProfile skull) {
        this.graveSkull = skull;
    }
    public @Nullable GraveComponent getComponent() {
        return this.component;
    }
    public @Nullable BlockState getPreviousState() {
        return this.previousState;
    }
    public boolean isUnclaimed() {
        return !this.claimed;
    }
    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }
    public @Nullable Text getGraveText() {
        return this.graveText;
    }

    public void onBroken() {
        if (this.world == null || this.world.isClient) return;

        Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(this.graveId);
        component.ifPresent(grave -> {
            if (grave.getStatus() == GraveStatus.UNCLAIMED) {
                grave.setStatus(GraveStatus.DESTROYED);
            }
        });
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = this.createNbt();
        if (this.graveSkull != null)
            nbt.put("skull", NbtHelper.writeGameProfile(new NbtCompound(), this.graveSkull));
        if (this.graveText != null)
            nbt.putString("text", Text.Serializer.toJson(this.graveText));
        nbt.putBoolean("claimed", this.claimed);

        return nbt;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.putBoolean("claimed", this.claimed);
        if (this.graveText != null)
            nbt.putString("text", Text.Serializer.toJson(this.graveText));
        if (this.graveSkull != null)
            nbt.put("skull", NbtHelper.writeGameProfile(new NbtCompound(), this.graveSkull));
        if (this.graveId != null)
            nbt.putUuid("graveId", this.graveId);
        if (this.previousState != null)
            nbt.put("previousState", NbtHelper.fromBlockState(this.previousState));
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        if (nbt.contains("skull"))
            this.graveSkull = NbtHelper.toGameProfile(nbt.getCompound("skull"));

        if (nbt.contains("text"))
            this.graveText = Text.Serializer.fromJson(nbt.getString("text"));

        this.claimed = nbt.getBoolean("claimed");

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

        long timePassed = world.getTime() - be.component.getCreationTime().getTime();
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
