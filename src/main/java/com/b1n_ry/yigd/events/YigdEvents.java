package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.util.GraveOverrideAreas;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

public class YigdEvents {
    public static class AdjustDropRuleEvent extends Event {
        private final InventoryComponent inventoryComponent;
        private final DeathContext deathContext;

        public AdjustDropRuleEvent(InventoryComponent inventoryComponent, DeathContext deathContext) {
            this.inventoryComponent = inventoryComponent;
            this.deathContext = deathContext;
        }

        public InventoryComponent getInventoryComponent() {
            return this.inventoryComponent;
        }
        public DeathContext getDeathContext() {
            return this.deathContext;
        }
    }

    public static class AllowBlockUnderGraveGenerationEvent extends Event {
        private final GraveComponent grave;
        private final BlockState blockUnder;
        private boolean allowPlacement = true;

        public AllowBlockUnderGraveGenerationEvent(GraveComponent grave, BlockState blockUnder) {
            this.grave = grave;
            this.blockUnder = blockUnder;
        }

        public GraveComponent getGrave() {
            return this.grave;
        }
        public BlockState getBlockUnder() {
            return this.blockUnder;
        }
        public boolean isPlacementAllowed() {
            return this.allowPlacement;
        }

        public void setAllowPlacement(boolean allowPlacement) {
            this.allowPlacement = allowPlacement;
        }
    }

    public static class AllowGraveGenerationEvent extends Event {
        private final DeathContext deathContext;
        private final GraveComponent grave;
        boolean allowGeneration = true;

        public AllowGraveGenerationEvent(DeathContext deathContext, GraveComponent grave) {
            this.deathContext = deathContext;
            this.grave = grave;
        }

        public DeathContext getDeathContext() {
            return this.deathContext;
        }
        public GraveComponent getGrave() {
            return this.grave;
        }
        public boolean isGenerationAllowed() {
            return this.allowGeneration;
        }

        public void setAllowGeneration(boolean allowGeneration) {
            this.allowGeneration = allowGeneration;
        }
    }

    public static class BeforeSoulboundEvent extends Event {
        private final ServerPlayer oldPlayer;
        private final ServerPlayer newPlayer;

        public BeforeSoulboundEvent(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
            this.oldPlayer = oldPlayer;
            this.newPlayer = newPlayer;
        }

        public ServerPlayer getOldPlayer() {
            return this.oldPlayer;
        }
        public ServerPlayer getNewPlayer() {
            return this.newPlayer;
        }
    }

    public static class DelayGraveGenerationEvent extends Event {
        private final GraveComponent grave;
        private final Direction direction;
        private final DeathContext deathContext;
        private final RespawnComponent respawnComponent;
        private final String caller;

        private boolean delayGeneration = false;

        public DelayGraveGenerationEvent(GraveComponent grave, Direction direction, DeathContext deathContext, RespawnComponent respawnComponent, String caller) {
            this.grave = grave;
            this.direction = direction;
            this.deathContext = deathContext;
            this.respawnComponent = respawnComponent;
            this.caller = caller;
        }

        public GraveComponent getGrave() {
            return this.grave;
        }
        public Direction getDirection() {
            return this.direction;
        }
        public DeathContext getDeathContext() {
            return this.deathContext;
        }
        public RespawnComponent getRespawnComponent() {
            return this.respawnComponent;
        }
        public String getCaller() {
            return this.caller;
        }
        public boolean generationIsDelayed() {
            return this.delayGeneration;
        }

        public void setDelayGeneration(boolean delayGeneration) {
            this.delayGeneration = delayGeneration;
        }
    }

    public static class DropItemEvent extends Event implements ICancellableEvent {
        private final ItemStack stack;
        private final double x;
        private final double y;
        private final double z;
        private final ServerLevel level;

        private boolean shouldDrop = true;

        public DropItemEvent(ItemStack stack, double x, double y, double z, ServerLevel level) {
            this.stack = stack;
            this.x = x;
            this.y = y;
            this.z = z;
            this.level = level;
        }

        public ItemStack getStack() {
            return this.stack;
        }
        public double getX() {
            return this.x;
        }
        public double getY() {
            return this.y;
        }
        public double getZ() {
            return this.z;
        }
        public ServerLevel getLevel() {
            return this.level;
        }
        public boolean shouldDrop() {
            return this.shouldDrop;
        }

        public void setShouldDrop(boolean shouldDrop) {
            this.shouldDrop = shouldDrop;
        }
    }

    public static class DropRuleEvent extends Event implements ICancellableEvent {
        private final ItemStack stack;
        private final int slot;
        @Nullable
        private final DeathContext deathContext;
        private final boolean modify;

        private DropRule dropRule = GraveOverrideAreas.INSTANCE.defaultDropRule;

        public DropRuleEvent(ItemStack stack, int slot, @Nullable DeathContext deathContext, boolean modify) {
            this.stack = stack;
            this.slot = slot;
            this.deathContext = deathContext;
            this.modify = modify;
        }

        public ItemStack getStack() {
            return this.stack;
        }
        public int getSlot() {
            return this.slot;
        }
        public @Nullable DeathContext getDeathContext() {
            return this.deathContext;
        }
        public boolean isModify() {
            return this.modify;
        }
        public DropRule getDropRule() {
            return this.dropRule;
        }

        public void setDropRule(DropRule dropRule) {
            this.dropRule = dropRule;
        }
    }

    public static class GraveClaimEvent extends Event implements ICancellableEvent {
        private final ServerPlayer player;
        private final ServerLevel level;
        private final BlockPos pos;
        private final GraveComponent grave;
        private final ItemStack tool;

        private boolean canClaim = false;

        public GraveClaimEvent(ServerPlayer player, ServerLevel level, BlockPos pos, GraveComponent grave, ItemStack tool) {
            this.player = player;
            this.level = level;
            this.pos = pos;
            this.grave = grave;
            this.tool = tool;
        }

        public ServerPlayer getPlayer() {
            return this.player;
        }
        public ServerLevel getLevel() {
            return this.level;
        }
        public BlockPos getPos() {
            return this.pos;
        }
        public GraveComponent getGrave() {
            return this.grave;
        }
        public ItemStack getTool() {
            return this.tool;
        }
        public boolean allowClaim() {
            return this.canClaim;
        }

        public void setCanClaim(boolean canClaim) {
            this.canClaim = canClaim;
        }
    }

    public static class GraveGenerationEvent extends Event {
        private final ServerLevel level;
        private final BlockPos pos;
        private final int nthTry;

        private boolean canGenerate = true;

        public GraveGenerationEvent(ServerLevel level, BlockPos pos, int nthTry) {
            this.level = level;
            this.pos = pos;
            this.nthTry = nthTry;
        }

        public ServerLevel getLevel() {
            return this.level;
        }
        public BlockPos getPos() {
            return this.pos;
        }
        public int getNthTry() {
            return this.nthTry;
        }
        public boolean canGenerate() {
            return this.canGenerate;
        }

        public void setCanGenerate(boolean canGenerate) {
            this.canGenerate = canGenerate;
        }
    }

    public static class RenderGlowingGraveEvent extends Event {
        private final GraveBlockEntity grave;
        private final LocalPlayer player;

        private boolean renderGlowing = false;

        public RenderGlowingGraveEvent(GraveBlockEntity grave, LocalPlayer player) {
            this.grave = grave;
            this.player = player;
        }

        public GraveBlockEntity getGrave() {
            return this.grave;
        }
        public LocalPlayer getPlayer() {
            return this.player;
        }
        public boolean isRenderGlowing() {
            return this.renderGlowing;
        }

        public void setRenderGlowing(boolean renderGlowing) {
            this.renderGlowing = renderGlowing;
        }
    }
}
