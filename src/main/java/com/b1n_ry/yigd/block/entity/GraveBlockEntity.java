package com.b1n_ry.yigd.block.entity;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
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
    private ProfileComponent graveSkull = null;
    @Nullable
    private Text graveText = null;
    @Nullable
    private BlockState previousState = null;

    private boolean claimed = true;

    private static YigdConfig cachedConfig = YigdConfig.getConfig();

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(Yigd.GRAVE_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void addComponents(ComponentMap.Builder componentMapBuilder) {
        super.addComponents(componentMapBuilder);
        componentMapBuilder.add(DataComponentTypes.PROFILE, this.graveSkull);
        componentMapBuilder.add(DataComponentTypes.CUSTOM_NAME, this.graveText);
        componentMapBuilder.add(GraveComponent.GRAVE_ID, this.graveId);
    }

    @Override
    protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
        this.setGraveSkull(components.get(DataComponentTypes.PROFILE));
        this.setGraveText(components.get(DataComponentTypes.CUSTOM_NAME));
        this.graveId = components.get(GraveComponent.GRAVE_ID);
    }

    public void setComponent(GraveComponent component) {
        this.component = component;
        this.setClaimed(component.getStatus() == GraveStatus.CLAIMED);
        this.graveSkull = component.getOwner();
        this.graveId = component.getGraveId();
        this.graveSkull.name().ifPresent(name -> GraveBlockEntity.this.graveText = Text.of(name));
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
    public @Nullable ProfileComponent getGraveSkull() {
        return this.graveSkull;
    }
    public void setGraveSkull(@Nullable ProfileComponent skull) {
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

        Yigd.END_OF_TICK.add(() -> {
            Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(this.graveId);
            component.ifPresent(grave -> {
                if (grave.getStatus() == GraveStatus.UNCLAIMED) {
                    grave.onDestroyed();
                }
            });
        });
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound nbt = this.createNbt(registryLookup);
        if (this.graveSkull != null) {
            ProfileComponent.CODEC.encodeStart(NbtOps.INSTANCE, this.graveSkull).resultOrPartial()
                    .ifPresent(nbtElement -> nbt.put("skull", nbtElement));
        }
        if (this.graveText != null)
            nbt.putString("text", Text.Serialization.toJsonString(this.graveText, registryLookup));
        nbt.putBoolean("claimed", this.claimed);

        return nbt;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putBoolean("claimed", this.claimed);
        if (this.graveText != null)
            nbt.putString("text", Text.Serialization.toJsonString(this.graveText, registryLookup));
        if (this.graveSkull != null)
            nbt.put("skull", ProfileComponent.CODEC.encodeStart(NbtOps.INSTANCE, this.graveSkull).getOrThrow());
        if (this.graveId != null)
            nbt.putUuid("graveId", this.graveId);
        if (this.previousState != null)
            nbt.put("previousState", NbtHelper.fromBlockState(this.previousState));
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        if (nbt.contains("skull"))
            ProfileComponent.CODEC.parse(NbtOps.INSTANCE, nbt.get("skull"))
                    .resultOrPartial(s -> Yigd.LOGGER.error("Failed to load grave skull"))
                    .ifPresent(this::setGraveSkull);

        if (nbt.contains("text"))
            this.graveText = Text.Serialization.fromJson(nbt.getString("text"), registryLookup);

        this.claimed = nbt.getBoolean("claimed");

        if (nbt.contains("graveId")) {
            this.graveId = nbt.getUuid("graveId");
            if (this.component == null && this.world != null && !this.world.isClient) {
                DeathInfoManager.INSTANCE.getGrave(this.graveId).ifPresent(this::setComponent);
            }
        }

        if (nbt.contains("previousState", NbtElement.COMPOUND_TYPE)) {
            RegistryWrapper<Block> registryEntryLookup = this.world != null ? this.world.createCommandRegistryWrapper(RegistryKeys.BLOCK) : Registries.BLOCK.getReadOnlyWrapper();
            this.previousState = NbtHelper.toBlockState(registryEntryLookup, nbt.getCompound("previousState"));
        }
    }

    public static void tick(World world, BlockPos ignoredPos, BlockState ignoredState, GraveBlockEntity be) {
        if (world.isClient) return;

        if (be.component == null) return;
        if (world.getTime() % 2400 == 0) cachedConfig = YigdConfig.getConfig();  // Reloads the config every 60 seconds

        YigdConfig.GraveConfig.GraveTimeout timeoutConfig = cachedConfig.graveConfig.graveTimeout;

        if (!timeoutConfig.enabled || be.component.getStatus() != GraveStatus.UNCLAIMED) return;

        long timePassed = world.getTime() - be.component.getCreationTime().getTime();
        final int ticksPerSecond = 20;
        if (timeoutConfig.timeUnit.toSeconds(timeoutConfig.afterTime) * ticksPerSecond <= timePassed) {
            // Not technically destroyed, but a status has to be set to not trigger the "onDestroyed" grave component method
            be.component.setStatus(GraveStatus.DESTROYED);


            BlockState newState = Blocks.AIR.getDefaultState();
            BlockState previousState = be.getPreviousState();
            if (YigdConfig.getConfig().graveConfig.replaceOldWhenClaimed && previousState != null) {
                newState = previousState;
            }
            be.component.replaceWithOld(newState);

            if (timeoutConfig.dropContentsOnTimeout) {
                be.component.dropAll();
            }
        }
    }
}
