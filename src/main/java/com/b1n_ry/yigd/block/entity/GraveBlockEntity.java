package com.b1n_ry.yigd.block.entity;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class GraveBlockEntity extends BlockEntity {
    @Nullable
    private GraveComponent component = null;
    @Nullable
    private UUID graveId = null;
    @Nullable
    private ResolvableProfile graveSkull = null;
    @Nullable
    private Component graveText = null;
    @Nullable
    private BlockState previousState = null;

    private boolean claimed = true;

    private static YigdConfig cachedConfig = YigdConfig.getConfig();

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(Yigd.GRAVE_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder componentMapBuilder) {
        super.collectImplicitComponents(componentMapBuilder);
        componentMapBuilder.set(DataComponents.PROFILE, this.graveSkull);
        componentMapBuilder.set(DataComponents.CUSTOM_NAME, this.graveText);
        componentMapBuilder.set(GraveComponent.GRAVE_ID, this.graveId);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        this.setGraveSkull(components.get(DataComponents.PROFILE));
        this.setGraveText(components.get(DataComponents.CUSTOM_NAME));
        this.graveId = components.get(GraveComponent.GRAVE_ID);
    }

    public void setComponent(GraveComponent component) {
        this.component = component;
        this.setClaimed(component.getStatus() == GraveStatus.CLAIMED);
        this.graveSkull = component.getOwner();
        this.graveId = component.getGraveId();
        this.graveSkull.name().ifPresent(name -> GraveBlockEntity.this.graveText = Component.nullToEmpty(name));
        this.setChanged();
    }
    public void setPreviousState(@Nullable BlockState previousState) {
        this.previousState = previousState;
    }
    public void setGraveText(@Nullable Component text) {
        this.graveText = text;
    }

    public @Nullable UUID getGraveId() {
        return this.graveId;
    }
    public @Nullable ResolvableProfile getGraveSkull() {
        return this.graveSkull;
    }
    public void setGraveSkull(@Nullable ResolvableProfile skull) {
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
    public @Nullable Component getGraveText() {
        return this.graveText;
    }

    public void onBroken() {
        if (this.level == null || this.level.isClientSide) return;

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
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        CompoundTag nbt = this.saveWithoutMetadata(registryLookup);
        if (this.graveSkull != null) {
            ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, this.graveSkull).resultOrPartial()
                    .ifPresent(nbtElement -> nbt.put("skull", nbtElement));
        }
        if (this.graveText != null)
            nbt.putString("text", Component.Serializer.toJson(this.graveText, registryLookup));
        nbt.putBoolean("claimed", this.claimed);

        return nbt;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        nbt.putBoolean("claimed", this.claimed);
        if (this.graveText != null)
            nbt.putString("text", Component.Serializer.toJson(this.graveText, registryLookup));
        if (this.graveSkull != null)
            nbt.put("skull", ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, this.graveSkull).getOrThrow());
        if (this.graveId != null)
            nbt.putUUID("graveId", this.graveId);
        if (this.previousState != null)
            nbt.put("previousState", NbtUtils.writeBlockState(this.previousState));
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        if (nbt.contains("skull"))
            ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, nbt.get("skull"))
                    .resultOrPartial(s -> Yigd.LOGGER.error("Failed to load grave skull"))
                    .ifPresent(this::setGraveSkull);

        if (nbt.contains("text"))
            this.graveText = Component.Serializer.fromJson(nbt.getString("text"), registryLookup);

        this.claimed = nbt.getBoolean("claimed");

        if (nbt.contains("graveId")) {
            this.graveId = nbt.getUUID("graveId");
            if (this.component == null && this.level != null && !this.level.isClientSide) {
                DeathInfoManager.INSTANCE.getGrave(this.graveId).ifPresent(this::setComponent);
            }
        }

        if (nbt.contains("previousState", Tag.TAG_COMPOUND)) {
            HolderLookup<Block> registryEntryLookup = this.level != null ? this.level.holderLookup(Registries.BLOCK) : BuiltInRegistries.BLOCK.asLookup();
            this.previousState = NbtUtils.readBlockState(registryEntryLookup, nbt.getCompound("previousState"));
        }
    }



    public static void tick(Level world, BlockPos pos, BlockState ignoredState, GraveBlockEntity be) {
        if (world.isClientSide) return;

        if (be.component == null) {
            if (be.graveId == null) return;
            DeathInfoManager.INSTANCE.getGrave(be.graveId).ifPresent(be::setComponent);
            if (be.component == null) return;
        }
        if (world.getGameTime() % 2400 == 0) cachedConfig = YigdConfig.getConfig();  // Reloads the config every 60 seconds

        YigdConfig.GraveConfig.GraveTimeout timeoutConfig = cachedConfig.graveConfig.graveTimeout;

        if (!pos.equals(be.component.getPos())
                || (!be.component.getWorldRegistryKey().equals(world.dimension()))) {
            be.updatePosition((ServerLevel) world, pos);
        }

        if (!timeoutConfig.enabled || be.component.getStatus() != GraveStatus.UNCLAIMED) return;

        long timePassed = world.getGameTime() - be.component.getCreationTime().getTime();
        final int ticksPerSecond = 20;
        if (timeoutConfig.timeUnit.toSeconds(timeoutConfig.afterTime) * ticksPerSecond <= timePassed) {
            // Not technically destroyed, but a status has to be set to not trigger the "onDestroyed" grave component method
            be.component.setStatus(GraveStatus.DESTROYED);


            BlockState newState = Blocks.AIR.defaultBlockState();
            BlockState previousState = be.getPreviousState();
            if (YigdConfig.getConfig().graveConfig.replaceOldWhenClaimed && previousState != null) {
                newState = previousState;
            }
            be.component.replaceWithOld(newState);

            if (timeoutConfig.dropContentsOnTimeout) {
                be.component.dropAllGraveItems();
            }
        }
    }

    private void updatePosition(ServerLevel world, BlockPos pos) {
        if (this.component == null) return;

        this.component.setPos(pos);
        this.component.setWorld(world);
        if (this.component.getStatus() == GraveStatus.DESTROYED || !this.claimed) {
            this.component.setStatus(GraveStatus.UNCLAIMED);
            PlayerList playerManager = world.getServer().getPlayerList();
            ResolvableProfile owner = this.component.getOwner();
            ServerPlayer player = owner.id().isPresent() ? playerManager.getPlayer(owner.id().get()) : playerManager.getPlayerByName(owner.name().orElse(null));
            if (player != null) {
                player.sendSystemMessage(Component.translatable("text.yigd.message.grave_relocated", pos.getX(), pos.getY(), pos.getZ(), world.dimension().location().toString()));
            }
        }
    }
}
