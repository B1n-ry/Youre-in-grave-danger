package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.ClaimPriority;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.data.TranslatableDeathMessage;
import com.b1n_ry.yigd.events.DropItemEvent;
import com.b1n_ry.yigd.events.GraveClaimEvent;
import com.b1n_ry.yigd.events.GraveGenerationEvent;
import com.b1n_ry.yigd.packets.LightGraveData;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GraveComponent {
    private final GameProfile owner;
    private final InventoryComponent inventoryComponent;
    private final ExpComponent expComponent;
    @Nullable
    private final ServerWorld world;
    private final RegistryKey<World> worldRegistryKey;
    private BlockPos pos;
    private final TranslatableDeathMessage deathMessage;
    private final UUID graveId;
    private GraveStatus status;
    private boolean locked;

    public GraveComponent(GameProfile owner, InventoryComponent inventoryComponent, ExpComponent expComponent, ServerWorld world, Vec3d pos, TranslatableDeathMessage deathMessage) {
        this(owner, inventoryComponent, expComponent, world, BlockPos.ofFloored(pos), deathMessage, UUID.randomUUID(), GraveStatus.UNCLAIMED, true);
    }
    public GraveComponent(GameProfile owner, InventoryComponent inventoryComponent, ExpComponent expComponent, ServerWorld world, BlockPos pos, TranslatableDeathMessage deathMessage, UUID graveId, GraveStatus status, boolean locked) {
        this.owner = owner;
        this.inventoryComponent = inventoryComponent;
        this.expComponent = expComponent;
        this.world = world;
        this.worldRegistryKey = world.getRegistryKey();
        this.pos = pos;
        this.deathMessage = deathMessage;
        this.graveId = graveId;
        this.status = status;
        this.locked = locked;
    }
    public GraveComponent(GameProfile owner, InventoryComponent inventoryComponent, ExpComponent expComponent, RegistryKey<World> worldKey, BlockPos pos, TranslatableDeathMessage deathMessage, UUID graveId, GraveStatus status, boolean locked) {
        this.owner = owner;
        this.inventoryComponent = inventoryComponent;
        this.expComponent = expComponent;
        this.world = null;
        this.worldRegistryKey = worldKey;
        this.pos = pos;
        this.deathMessage = deathMessage;
        this.graveId = graveId;
        this.status = status;
        this.locked = locked;
    }

    public GameProfile getOwner() {
        return this.owner;
    }

    public InventoryComponent getInventoryComponent() {
        return this.inventoryComponent;
    }

    public ExpComponent getExpComponent() {
        return this.expComponent;
    }

    public @Nullable ServerWorld getWorld() {
        return this.world;
    }
    public RegistryKey<World> getWorldRegistryKey() {
        return this.worldRegistryKey;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public TranslatableDeathMessage getDeathMessage() {
        return this.deathMessage;
    }

    public UUID getGraveId() {
        return this.graveId;
    }

    public GraveStatus getStatus() {
        return this.status;
    }
    public boolean isLocked() {
        return this.locked;
    }
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isEmpty() {
        return this.inventoryComponent.isEmpty() && this.expComponent.isEmpty();
    }

    /**
     * Determines weather or not the grave should generate based on conditions when
     * player has just died. Filters are not yet applied here.
     * @param config Config that method should take into consideration
     * @param deathSource How the player died. Used to filter out some death causes if
     *                    mod is configured to.
     * @return Weather or not the grave should generate. If false, grave contents are
     * instead dropped.
     */
    public boolean shouldGenerate(YigdConfig config, DamageSource deathSource) {
        YigdConfig.GraveConfig graveConfig = config.graveConfig;
        if (!graveConfig.enabled) return false;

        if (!graveConfig.generateEmptyGraves && this.isEmpty()) return false;

        if (graveConfig.dimensionBlacklist.contains(this.worldRegistryKey.getValue().toString())) return false;

        if (!graveConfig.generateGraveInVoid && this.pos.getY() < 0) return false;

        return !graveConfig.ignoredDeathTypes.contains(deathSource.getName());
    }

    /**
     * Will filter through filters and stuff. Should only be called from server
     * @return where a grave can be placed based on config
     */
    public BlockPos findGravePos() {
        if (this.world == null) {
            Yigd.LOGGER.error("GraveComponent's associated world is null. Failed to find suitable position");
            return this.pos;
        }

        YigdConfig config = YigdConfig.getConfig();
        Vec3i generationMaxDistance = config.graveConfig.generationMaxDistance;
        for (BlockPos iPos : BlockPos.iterateOutwards(this.pos, generationMaxDistance.getX(), generationMaxDistance.getY(), generationMaxDistance.getZ())) {
            if (GraveGenerationEvent.EVENT.invoker().canGenerateAt(this.world, iPos)) {
                this.pos = iPos;
                return iPos;
            }
        }
        return this.pos;
    }

    /**
     * Called to place down a grave block. Should only be called from server
     * @param newPos Where the grave should try to be placed
     * @param state Which block should be placed
     * @return Weather or not the grave was placed
     */
    public boolean tryPlaceGraveAt(BlockPos newPos, BlockState state) {
        this.pos = newPos;
        if (this.world == null) {
            Yigd.LOGGER.error("GraveComponent tried to place grave without knowing the ServerWorld");
            return false;
        }
        return this.world.setBlockState(newPos, state);
    }

    public void backUp() {
        DeathInfoManager.INSTANCE.addBackup(this.owner, this);
        DeathInfoManager.INSTANCE.markDirty();
    }

    public ActionResult claim(ServerPlayerEntity player, ServerWorld world, BlockState previousState, BlockPos pos, ItemStack tool) {
        YigdConfig config = YigdConfig.getConfig();

        if (!GraveClaimEvent.EVENT.invoker().canClaim(player, world, pos, this, tool)) return ActionResult.FAIL;

        this.applyToPlayer(player, world, pos, player.getUuid().equals(this.owner.getId()));

        if (config.graveConfig.replaceOldWhenClaimed && previousState != null) {
            world.setBlockState(pos, previousState);
        }

        this.status = GraveStatus.CLAIMED;
        return ActionResult.SUCCESS;
    }

    public void applyToPlayer(ServerPlayerEntity player, ServerWorld world, BlockPos pos, boolean isGraveOwner) {
        YigdConfig config = YigdConfig.getConfig();

        InventoryComponent currentPlayerInv = new InventoryComponent(player);
        InventoryComponent.clearPlayer(player);

        DefaultedList<ItemStack> extraItems = DefaultedList.of();
        ClaimPriority priority = isGraveOwner ? config.graveConfig.claimPriority : config.graveConfig.graveRobbing.robPriority;

        if (priority == ClaimPriority.GRAVE) {
            extraItems.addAll(this.inventoryComponent.merge(currentPlayerInv, true));
            extraItems.addAll(this.inventoryComponent.applyToPlayer(player));
        } else {
            extraItems.addAll(currentPlayerInv.merge(this.inventoryComponent, false));
            extraItems.addAll(currentPlayerInv.applyToPlayer(player));
        }

        for (ItemStack stack : extraItems) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            if (DropItemEvent.EVENT.invoker().shouldDropItem(stack, x, y, z, world))
                ItemScatterer.spawn(world, x, y, z, stack);
        }
    }

    public LightGraveData toLightData() {
        return new LightGraveData(this.inventoryComponent.size(), this.pos,
                this.expComponent.getStoredXp(), this.worldRegistryKey, this.deathMessage, this.graveId, this.status);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.put("owner", NbtHelper.writeGameProfile(new NbtCompound(), this.owner));
        nbt.put("inventory", this.inventoryComponent.toNbt());
        nbt.put("exp", this.expComponent.toNbt());

        nbt.put("world", this.getWorldRegistryKeyNbt(this.worldRegistryKey));
        nbt.put("pos", NbtHelper.fromBlockPos(this.pos));
        nbt.put("deathMessage", this.deathMessage.toNbt());
        nbt.putUuid("graveId", this.graveId);
        nbt.putString("status", this.status.toString());


        return nbt;
    }
    private NbtCompound getWorldRegistryKeyNbt(RegistryKey<?> key) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("registry", key.getRegistry().toString());
        nbt.putString("value", key.getValue().toString());

        return nbt;
    }

    public static GraveComponent fromNbt(NbtCompound nbt, @Nullable MinecraftServer server) {
        GameProfile owner = NbtHelper.toGameProfile(nbt.getCompound("owner"));
        InventoryComponent inventoryComponent = InventoryComponent.fromNbt(nbt.getCompound("inventory"));
        ExpComponent expComponent = ExpComponent.fromNbt(nbt.getCompound("exp"));
        RegistryKey<World> worldKey = getRegistryKeyFromNbt(nbt.getCompound("world"));
        BlockPos pos = NbtHelper.toBlockPos(nbt.getCompound("pos"));
        TranslatableDeathMessage deathMessage = TranslatableDeathMessage.fromNbt(nbt.getCompound("deathMessage"));
        UUID graveId = nbt.getUuid("graveId");
        GraveStatus status = GraveStatus.valueOf(nbt.getString("status"));
        boolean locked = nbt.getBoolean("locked");

        if (server != null) {
            ServerWorld world = server.getWorld(worldKey);
            if (world == null) {
                Yigd.LOGGER.error("World " + worldKey.toString() + " not recognized. Loading grave component without world");
            } else {
                return new GraveComponent(owner, inventoryComponent, expComponent, world, pos, deathMessage, graveId, status, locked);
            }
        }
        return new GraveComponent(owner, inventoryComponent, expComponent, worldKey, pos, deathMessage, graveId, status, locked);
    }
    private static RegistryKey<World> getRegistryKeyFromNbt(NbtCompound nbt) {
        String registry = nbt.getString("registry");
        String value = nbt.getString("value");

        RegistryKey<Registry<World>> r = RegistryKey.ofRegistry(new Identifier(registry));
        return RegistryKey.of(r, new Identifier(value));
    }
}
