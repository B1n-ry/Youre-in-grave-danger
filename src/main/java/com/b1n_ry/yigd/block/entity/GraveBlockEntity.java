package com.b1n_ry.yigd.block.entity;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.components.GraveComponent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class GraveBlockEntity extends BlockEntity {
    private GraveComponent component = null;
    private UUID graveId = null;
    private BlockState previousState = null;

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(Yigd.GRAVE_BLOCK_ENTITY, pos, state);
    }

    public void setComponent(GraveComponent component) {
        this.component = component;
        this.graveId = component.getGraveId();
        this.markDirty();
    }
    public void setPreviousState(BlockState previousState) {
        this.previousState = previousState;
    }

    public GraveComponent getComponent() {
        return this.component;
    }
    public BlockState getPreviousState() {
        return this.previousState;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        if (this.component == null) return;
        nbt.putUuid("graveId", this.graveId);
        if (this.previousState != null) nbt.put("previousState", NbtHelper.fromBlockState(this.previousState));
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.graveId = nbt.getUuid("graveId");

        if (nbt.contains("previousState")) {
            RegistryWrapper<Block> registryEntryLookup = this.world != null ? this.world.createCommandRegistryWrapper(RegistryKeys.BLOCK) : Registries.BLOCK.getReadOnlyWrapper();
            this.previousState = NbtHelper.toBlockState(registryEntryLookup, nbt.getCompound("previousState"));
        }
    }
}
