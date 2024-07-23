package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.ClaimPriority;
import com.b1n_ry.yigd.config.DropType;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.*;
import com.b1n_ry.yigd.events.AllowBlockUnderGraveGenerationEvent;
import com.b1n_ry.yigd.events.AllowGraveGenerationEvent;
import com.b1n_ry.yigd.events.GraveClaimEvent;
import com.b1n_ry.yigd.events.GraveGenerationEvent;
import com.b1n_ry.yigd.packets.LightGraveData;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.util.GraveCompassHelper;
import com.b1n_ry.yigd.util.GraveOverrideAreas;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionTypes;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GraveComponent {
    private final GameProfile owner;
    private InventoryComponent inventoryComponent;
    private ExpComponent expComponent;
    /**
     * world should never be null while on server, but only on client.
     * If this is compromised, the mod might crash
     */
    @Nullable
    private ServerWorld world;
    private RegistryKey<World> worldRegistryKey;
    private BlockPos pos;
    private final TranslatableDeathMessage deathMessage;
    private final UUID graveId;
    private GraveStatus status;
    private boolean locked;
    private final TimePoint creationTime;
    private final UUID killerId;

    public static GraveyardData graveyardData = null;

    public GraveComponent(GameProfile owner, InventoryComponent inventoryComponent, ExpComponent expComponent, ServerWorld world, Vec3d pos, TranslatableDeathMessage deathMessage, UUID killerId) {
        this(owner, inventoryComponent, expComponent, world, BlockPos.ofFloored(pos), deathMessage, UUID.randomUUID(), GraveStatus.UNCLAIMED, true, new TimePoint(world), killerId);
    }
    public GraveComponent(GameProfile owner, InventoryComponent inventoryComponent, ExpComponent expComponent, ServerWorld world,
                          BlockPos pos, TranslatableDeathMessage deathMessage, UUID graveId, GraveStatus status, boolean locked, TimePoint creationTime, UUID killerId) {
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
        this.creationTime = creationTime;
        this.killerId = killerId;
    }
    public GraveComponent(GameProfile owner, InventoryComponent inventoryComponent, ExpComponent expComponent, RegistryKey<World> worldKey,
                          BlockPos pos, TranslatableDeathMessage deathMessage, UUID graveId, GraveStatus status, boolean locked, TimePoint creationTime, UUID killerId) {
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
        this.creationTime = creationTime;
        this.killerId = killerId;
    }

    public GameProfile getOwner() {
        return this.owner;
    }

    public InventoryComponent getInventoryComponent() {
        return this.inventoryComponent;
    }
    public void setInventoryComponent(InventoryComponent inventoryComponent) {
        this.inventoryComponent = inventoryComponent;
        DeathInfoManager.INSTANCE.markDirty();
    }

    public ExpComponent getExpComponent() {
        return this.expComponent;
    }
    public void setExpComponent(ExpComponent expComponent) {
        this.expComponent = expComponent;
        DeathInfoManager.INSTANCE.markDirty();
    }
    /**
     * While on server, this will never return null
     * @return the world the component belongs to. Null if on client
     */
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
    public TimePoint getCreationTime() {
        return this.creationTime;
    }
    public UUID getKillerId() {
        return this.killerId;
    }
    public void setLocked(boolean locked) {
        this.locked = locked;
        DeathInfoManager.INSTANCE.markDirty();
    }
    public void setPos(BlockPos pos) {
        this.pos = pos;
        DeathInfoManager.INSTANCE.markDirty();
    }
    public void setWorld(ServerWorld world) {
        this.world = world;
        this.worldRegistryKey = world.getRegistryKey();
    }
    public void setStatus(GraveStatus status) {
        if (this.status == GraveStatus.UNCLAIMED
                && YigdConfig.getConfig().extraFeatures.graveCompass.pointToClosest != YigdConfig.ExtraFeatures.GraveCompassConfig.CompassGraveTarget.DISABLED) {
            GraveCompassHelper.setClaimed(this.worldRegistryKey, this.pos);
        }
        this.status = status;
        DeathInfoManager.INSTANCE.markDirty();
    }

    public boolean isGraveEmpty() {
        return this.inventoryComponent.isGraveEmpty() && this.expComponent.isEmpty();
    }
    public boolean isEmpty() {
        return this.inventoryComponent.isEmpty() && this.expComponent.isEmpty();
    }

    /**
     * Will filter through filters and stuff. Should only be called from server
     * @return where a grave can be placed based on config
     */
    public DirectionalPos findGravePos(Direction defaultDirection) {
        if (this.world == null) {
            Yigd.LOGGER.error("GraveComponent's associated world is null. Failed to find suitable position");
            return new DirectionalPos(this.pos, defaultDirection);
        }

        DirectionalPos graveyardPos = this.findPosInGraveyard(defaultDirection);
        if (graveyardPos != null)
            return graveyardPos;

        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.GraveConfig.Range generationMaxDistance = config.graveConfig.generationMaxDistance;

        // Loop should ABSOLUTELY NOT loop 50 times, but in case some stupid ass person (maybe me lol) doesn't return true by default
        // in canGenerate when i reaches some value (maybe 4) there is a cap at least, so the loop won't continue forever and freeze the game
        for (int i = 0; i < 50; i++) {
            for (BlockPos iPos : BlockPos.iterateOutwards(this.pos, generationMaxDistance.x, generationMaxDistance.y, generationMaxDistance.z)) {
                if (GraveGenerationEvent.EVENT.invoker().canGenerateAt(this.world, iPos, i)) {
                    this.pos = iPos;
                    DeathInfoManager.INSTANCE.markDirty();
                    return new DirectionalPos(iPos, defaultDirection);
                }
            }
        }
        return new DirectionalPos(this.pos, defaultDirection);
    }

    private DirectionalPos findPosInGraveyard(Direction defaultDirection) {
        if (this.world == null) return null;
        if (graveyardData == null || graveyardData.graveLocations.isEmpty()) return null;

        MinecraftServer server = this.world.getServer();
        ServerWorld graveyardWorld = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, graveyardData.dimensionId));
        if (graveyardWorld == null) {
            graveyardWorld = server.getOverworld();
        }
        DirectionalPos closest = null;
        for (GraveyardData.GraveLocation location : graveyardData.graveLocations) {
            if (location.forPlayer != null && !location.forPlayer.equalsIgnoreCase(this.owner.getName()))
                continue;

            Direction direction = location.direction != null ? location.direction : defaultDirection;

            DirectionalPos maybePos = new DirectionalPos(location.x, location.y, location.z, direction);
            if (graveyardData.useClosest) {
                if (closest == null || maybePos.getSquaredDistance(this.pos) < closest.getSquaredDistance(this.pos))
                    closest = maybePos;
            } else {
                closest = maybePos;
                break;
            }
        }
        if (closest != null) {
            this.world = graveyardWorld;
            this.pos = closest.pos();
            return closest;
        }

        return null;
    }

    /**
     * Called to place down a grave block. Should only be called from server
     * @param attemptedPos Where the grave should try to be placed
     * @param state Which block should be placed
     * @return Weather or not the grave was placed
     */
    public boolean tryPlaceGraveAt(BlockPos attemptedPos, BlockState state) {
        if (this.world == null) {
            Yigd.LOGGER.error("GraveComponent tried to place grave without knowing the ServerWorld");
            return false;
        }

        YigdConfig.GraveConfig config = YigdConfig.getConfig().graveConfig;
        int y = attemptedPos.getY();
        int lowerAcceptableY = config.lowestGraveY + this.world.getBottomY();
        if (config.generateGraveInVoid && attemptedPos.getY() <= lowerAcceptableY) {
            y = lowerAcceptableY;
        }
        int topY = this.world.getTopY() - 1;
        if (y > topY) {
            y = topY;
        }

        int x = attemptedPos.getX();
        int z = attemptedPos.getZ();
        if (config.generateOnlyWithinBorder) {
            WorldBorder border = this.world.getWorldBorder();
            if (!border.contains(x, z)) {
                x = (int) Math.max(x, border.getBoundWest());
                x = (int) Math.min(x, border.getBoundEast());

                z = (int) Math.max(z, border.getBoundNorth());
                z = (int) Math.min(z, border.getBoundSouth());
            }
        }

        this.pos = new BlockPos(x, y, z);

        // Makes sure the grave is not broken/replaced by portal, or the dragon egg
        if (this.world.getDimensionKey().equals(DimensionTypes.THE_END)) {
            if (Math.abs(attemptedPos.getX()) + Math.abs(attemptedPos.getZ()) < 25 && this.world.getBlockState(attemptedPos.down()).isOf(Blocks.BEDROCK))
                this.pos = this.pos.up();
        }

        DeathInfoManager.INSTANCE.markDirty();  // The "this" object is (at least should be) located inside DeathInfoManager.INSTANCE

        this.placeBlockUnder();
        return this.world.setBlockState(this.pos, state);
    }

    public void placeAndLoad(Direction direction, DeathContext context, BlockPos pos, RespawnComponent respawnComponent) {
        YigdConfig config = YigdConfig.getConfig();

        ServerWorld world = context.world();
        Vec3d deathPos = context.deathPos();

        // Check storage options first, in case that will lead to empty graves
        if (!config.graveConfig.storeItems) {
            this.inventoryComponent.dropGraveItems(world, deathPos);
        }
        if (!config.graveConfig.storeXp) {
            this.expComponent.dropAll(world, deathPos);
            this.getExpComponent().clear();
        }

        boolean waterlogged = world.getFluidState(pos).isOf(Fluids.WATER);  // Grave generated in full water block (submerged)
        BlockState graveBlock = Yigd.GRAVE_BLOCK.getDefaultState()
                .with(net.minecraft.state.property.Properties.HORIZONTAL_FACING, direction)
                .with(Properties.WATERLOGGED, waterlogged);

        respawnComponent.setGraveGenerated(true);  // Not guaranteed yet, but only errors can stop it from generating after this point
        DeathInfoManager.INSTANCE.markDirty();  // Make sure respawn component is updated

        // At this point is where the END_OF_TICK would be implemented, unless it wasn't already so
        Yigd.END_OF_TICK.add(() -> {
            BlockState previousState = world.getBlockState(pos);

            boolean placed = this.tryPlaceGraveAt(pos, graveBlock);
            BlockPos placedPos = this.getPos();

            if (!placed) {
                Yigd.LOGGER.error("Failed to generate grave at X: %d, Y: %d, Z: %d, %s".formatted(
                        placedPos.getX(), placedPos.getY(), placedPos.getZ(), world.getRegistryKey().getValue()));
                Yigd.LOGGER.info("Dropping items on ground instead of in grave");
                this.getInventoryComponent().dropGraveItems(world, Vec3d.of(placedPos));
                this.getExpComponent().dropAll(world, Vec3d.of(placedPos));
                return;
            }

            GraveBlockEntity be = (GraveBlockEntity) world.getBlockEntity(placedPos);
            if (be == null) return;
            be.setPreviousState(previousState);
            be.setComponent(this);
        });
    }

    public void generateOrDrop(Direction playerDirection, DeathContext context, RespawnComponent respawnComponent) {
        ServerWorld world = context.world();
        Vec3d pos = context.deathPos();
        if (!AllowGraveGenerationEvent.EVENT.invoker().allowGeneration(context, this)) {
            this.inventoryComponent.dropGraveItems(world, pos);
            this.expComponent.dropAll(world, pos);
        } else {
            DirectionalPos dirGravePos = this.findGravePos(playerDirection);
            BlockPos gravePos = dirGravePos.pos();
            Direction direction = dirGravePos.dir();

            ServerWorld graveWorld = this.getWorld();
            assert graveWorld != null;  // Shouldn't use assert in production, but I want to avoid warnings. Since we're on server side, this always passes

            this.placeAndLoad(direction, context, gravePos, respawnComponent);
        }
    }

    private void placeBlockUnder() {
        if (this.world == null) {
            Yigd.LOGGER.error("Tried to place block under a grave but world was null");
            return;
        }
        YigdConfig.GraveConfig.BlockUnderGrave config = YigdConfig.getConfig().graveConfig.blockUnderGrave;
        if (!config.enabled) return;  // Not in an event because idk. I don't want to put this in an event I guess

        BlockState currentUnder = this.world.getBlockState(this.pos.down());
        if (!AllowBlockUnderGraveGenerationEvent.EVENT.invoker().allowBlockGeneration(this, currentUnder)) return;

        Map<String, String> blockInDimMap = new HashMap<>();
        for (YigdConfig.MapEntry pair : config.blockInDimensions) {
            blockInDimMap.put(pair.key, pair.value);
        }

        String dimName = this.worldRegistryKey.getValue().toString();
        if (!blockInDimMap.containsKey(dimName)) dimName = "misc";

        String blockName = blockInDimMap.get(dimName);
        if (blockName == null) {
            Yigd.LOGGER.warn("Didn't place supporting block under grave in %s, at %d, %d, %d. Couldn't find dimension key in config"
                    .formatted(this.worldRegistryKey.getValue().toString(), this.pos.getX(), this.pos.getY(), this.pos.getZ()));
            return;
        }

        Block blockUnder = Registries.BLOCK.get(new Identifier(blockName));
        boolean placed = this.world.setBlockState(this.pos.down(), blockUnder.getDefaultState());
        if (!placed) {
            Yigd.LOGGER.warn("Didn't place supporting block under grave in %s, at %d, %d, %d. Block placement failed"
                    .formatted(this.worldRegistryKey.getValue().toString(), this.pos.getX(), this.pos.getY(), this.pos.getZ()));
        }
    }

    /**
     * Replaces the grave with the block that was there before the grave was placed (or air if feature is disabled)
     * @param newState The block that should be placed instead of the grave (previous state)
     * @return Weather or not the block was replaced
     */
    public boolean replaceWithOld(BlockState newState) {
        if (this.world == null) return false;

        boolean placed = this.world.setBlockState(this.pos, newState);  // Place the block
        // Although no player placed the block, we still need to update it in case the block is multipart
        newState.getBlock().onPlaced(this.world, this.pos, newState, null, ItemStack.EMPTY);

        return placed;
    }

    public void backUp() {
        DeathInfoManager.INSTANCE.addBackup(this.owner, this);
        DeathInfoManager.INSTANCE.markDirty();
    }

    public boolean hasExistedTicks(long time) {
        if (this.world == null) return false;

        return this.world.getTime() - this.creationTime.getTime() >= time;
    }

    /**
     * Will return the time until the grave can be robbed. Will not check if grave can already be robbed, which might
     * cause the time to be negative
     * @return Time until the grave can be robbed represented as a string; hh:mm:ss
     */
    public String getTimeUntilRobbable() {
        if (this.world == null) return "0";
        final int tps = 20;
        YigdConfig.GraveConfig.GraveRobbing robConfig = YigdConfig.getConfig().graveConfig.graveRobbing;
        long delay = robConfig.timeUnit.toSeconds(robConfig.afterTime) * tps;

        long timePassed = (this.creationTime.getTime() - this.world.getTime() + delay) / tps;
        long seconds = timePassed % 60;
        long minutes = (timePassed / 60) % 60;
        long hours = timePassed / 3600;
        return "%02d:%02d:%02d".formatted(hours, minutes, seconds);
    }

    public ActionResult claim(ServerPlayerEntity player, ServerWorld world, BlockState previousState, BlockPos pos, ItemStack tool) {
        YigdConfig config = YigdConfig.getConfig();

        if (this.status == GraveStatus.CLAIMED) return ActionResult.FAIL;  // Otherwise runs twice when persistent graves is enabled
        if (!GraveClaimEvent.EVENT.invoker().canClaim(player, world, pos, this, tool)) return ActionResult.FAIL;

        this.handleRandomSpawn(config.graveConfig.randomSpawn, world, player.getGameProfile());

        boolean thisIsARobbery = !player.getUuid().equals(this.owner.getId());

        ItemStack graveItem = new ItemStack(Yigd.GRAVE_BLOCK.asItem());
        boolean addGraveItem = config.graveConfig.dropGraveBlock;
        if (config.graveConfig.dropOnRetrieve == DropType.IN_INVENTORY) {
            this.applyToPlayer(player, world, pos.toCenterPos(), !thisIsARobbery);

            if (addGraveItem)
                player.giveItemStack(graveItem);
        } else if (config.graveConfig.dropOnRetrieve == DropType.ON_GROUND) {
            this.dropAll();

            if (this.world != null && addGraveItem)
                InventoryComponent.dropItemIfToBeDropped(graveItem, this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.world);
        }

        this.setStatus(GraveStatus.CLAIMED);

        if (!config.graveConfig.persistentGraves.enabled) {
            if (config.graveConfig.replaceOldWhenClaimed && previousState != null) {
                this.replaceWithOld(previousState);
            } else {
                world.removeBlock(pos, false);
            }
        } else {
            GraveBlockEntity be = (GraveBlockEntity) world.getBlockEntity(pos);
            if (be != null) {
                BlockState state = world.getBlockState(pos);

                be.setClaimed(true);
                be.markDirty();
                world.updateListeners(pos, state, state, Block.NOTIFY_ALL);
            }
        }

        if (thisIsARobbery && config.graveConfig.graveRobbing.notifyWhenRobbed) {
            MinecraftServer server = world.getServer();
            String robberName = player.getGameProfile().getName();
            ServerPlayerEntity robbedPlayer = server.getPlayerManager().getPlayer(this.owner.getId());
            if (robbedPlayer != null) {  // They are not offline. They are online
                if (config.graveConfig.graveRobbing.tellWhoRobbed) {
                    robbedPlayer.sendMessage(Text.translatable("text.yigd.message.inform_robbery.with_details", player.getGameProfile().getName()));
                } else {
                    robbedPlayer.sendMessage(Text.translatable("text.yigd.message.inform_robbery"));
                }
            } else {
                Yigd.NOT_NOTIFIED_ROBBERIES.computeIfAbsent(this.owner.getId(), uuid -> new ArrayList<>()).add(robberName);
            }
        }

        Yigd.LOGGER.info("%s claimed a grave belonging to %s at %d, %d, %d, %s"
                .formatted(player.getGameProfile().getName(), this.owner.getName(), this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.worldRegistryKey.getValue()));
        return ActionResult.SUCCESS;
    }

    /**
     * Will remove the grave block associated with the component (if it exists)
     * __DO NOTE__: Unless status for the grave is changed from UNCLAIMED *before* called, status will be set to DESTROYED
     * @return Weather or not a grave block was removed
     */
    public boolean removeGraveBlock() {
        if (this.status == GraveStatus.UNCLAIMED)
            this.setStatus(GraveStatus.DESTROYED);

        if (this.world == null) return false;
        if (!(this.world.getBlockEntity(this.pos) instanceof GraveBlockEntity grave)) return false;

        BlockState previousState = grave.getPreviousState();
        if (previousState == null) {
            return this.world.removeBlock(this.pos, false);
        } else {
            return this.replaceWithOld(previousState);
        }
    }

    private void handleRandomSpawn(YigdConfig.GraveConfig.RandomSpawn config, ServerWorld world, GameProfile looter) {
        if (config.percentSpawnChance <= world.random.nextInt(100)) return;  // Using world's random (from world seed)
        NbtIntArray ownerIdNbt = NbtHelper.fromUuid(this.owner.getId());
        NbtIntArray looterIdNbt = NbtHelper.fromUuid(looter.getId());

        String summonNbt = config.spawnNbt
                .replaceAll("\\$\\{owner\\.name}", this.owner.getName())
                .replaceAll("\\$\\{owner\\.uuid}", ownerIdNbt.asString())
                .replaceAll("\\$\\{looter\\.name}", looter.getName())
                .replaceAll("\\$\\{looter\\.uuid}", looterIdNbt.asString());

        // While the nbt string has an item to add (text contains "${item[i]}")
        Matcher nbtMatcher;
        DefaultedList<Pair<ItemStack, DropRule>> items = this.inventoryComponent.getItems();
        do {
            // Find if there are any instances an item should be placed in the nbt
            Pattern nbtPattern = Pattern.compile("\\$\\{!?item\\[[0-9]+]}");
            nbtMatcher = nbtPattern.matcher(summonNbt);
            if (!nbtMatcher.find()) break;  // The next instance was not found

            // Get the integer of the item to replace with
            Pattern pattern = Pattern.compile("(?<=\\$\\{!?item\\[)[0-9]+(?=]})");
            Matcher matcher = pattern.matcher(summonNbt);
            if (!matcher.find()) break;  // No instance of item was found

            String res = matcher.group();
            // Following line is not really necessary, but used as a precaution in case I'm not as good at regex as I think I am
            if (!res.matches("[0-9]+")) break; // The string is not an integer -> break loop before error happens
            int itemNumber = Integer.parseInt(res);

            // Package item as NBT, and put inside NBT summon string
            ItemStack item = items.get(itemNumber).getLeft();
            NbtCompound itemNbt = item.getNbt();
            NbtCompound newNbt = new NbtCompound();
            newNbt.put("tag", itemNbt);
            newNbt.putString("id", Registries.ITEM.getId(item.getItem()).toString());
            newNbt.putInt("Count", item.getCount());

            boolean removeItem = summonNbt.contains("${!item[" + itemNumber + "]}"); // Contains ! -> remove item from list later

            summonNbt = summonNbt.replaceAll("\\$\\{!?item\\[" + itemNumber + "]}", newNbt.asString());

            if (removeItem) items.set(itemNumber, new Pair<>(ItemStack.EMPTY, GraveOverrideAreas.INSTANCE.defaultDropRule)); // Make sure item gets "used"
        } while (nbtMatcher.find());  // Loop until no more items should be inserted in NBT

        try {
            NbtCompound nbt = NbtHelper.fromNbtProviderString(summonNbt);
            nbt.putString("id", config.spawnEntity);
            Entity entity = EntityType.loadEntityWithPassengers(nbt, world, e -> {
                e.refreshPositionAndAngles(this.pos, e.getYaw(), e.getPitch());  // Make sure the entity is in the right place
                return e;
            });

            world.spawnEntity(entity);
        } catch (CommandSyntaxException e) {
            Yigd.LOGGER.error("Failed spawning entity on grave", e);
        }
    }

    public void applyToPlayer(ServerPlayerEntity player, ServerWorld world, Vec3d pos, boolean isGraveOwner) {
        this.applyToPlayer(player, world, pos, isGraveOwner, dropRule -> dropRule == DropRule.PUT_IN_GRAVE);
    }
    public void applyToPlayer(ServerPlayerEntity player, ServerWorld world, Vec3d pos, boolean isGraveOwner, Predicate<DropRule> itemFilter) {
        YigdConfig config = YigdConfig.getConfig();

        this.expComponent.applyToPlayer(player);

        InventoryComponent currentPlayerInv = new InventoryComponent(player);
        InventoryComponent.clearPlayer(player);

        DefaultedList<ItemStack> extraItems = DefaultedList.of();

        UUID playerId = player.getUuid();
        ClaimPriority claimPriority = Yigd.CLAIM_PRIORITIES.containsKey(playerId) ? Yigd.CLAIM_PRIORITIES.get(playerId) : config.graveConfig.claimPriority;
        ClaimPriority robPriority = Yigd.ROB_PRIORITIES.containsKey(playerId) ? Yigd.CLAIM_PRIORITIES.get(playerId) : config.graveConfig.graveRobbing.robPriority;

        ClaimPriority priority = isGraveOwner ? claimPriority : robPriority;

        InventoryComponent graveInv = this.inventoryComponent.filteredInv(itemFilter);

        // Move curse of binding items from equipped in grave, so they can't get stuck to the player even after death
        if (config.graveConfig.treatBindingCurse) {
            extraItems.addAll(graveInv.pullBindingCurseItems(player));
        }
        if (priority == ClaimPriority.GRAVE) {
            extraItems.addAll(graveInv.merge(currentPlayerInv, player));
            extraItems.addAll(graveInv.applyToPlayer(player));
        } else {
            extraItems.addAll(currentPlayerInv.merge(graveInv, player));
            extraItems.addAll(currentPlayerInv.applyToPlayer(player));
        }

        for (ItemStack stack : extraItems) {
            if (player.giveItemStack(stack))
                continue;
            double x = pos.getX();
            double y = pos.getY();
            double z = pos.getZ();
            InventoryComponent.dropItemIfToBeDropped(stack, x, y, z, world);
        }
    }

    public void dropAll() {
        this.inventoryComponent.dropAll(this.world, this.pos.toCenterPos());
        this.expComponent.dropAll(this.world, this.pos.toCenterPos());
    }

    public void onDestroyed() {
        this.setStatus(GraveStatus.DESTROYED);

        if (this.world == null) return;  // Should not be the case. But this is instead of an assert that could crash the game if another mod used this method incorrectly
        PlayerManager playerManager = this.world.getServer().getPlayerManager();
        ServerPlayerEntity owner = playerManager.getPlayer(this.owner.getId());
        if (owner == null) return;

        YigdConfig config = YigdConfig.getConfig();

        if (config.graveConfig.notifyOwnerIfDestroyed) {
            owner.sendMessage(Text.translatable("text.yigd.message.grave_destroyed"));
        }

        if (YigdConfig.getConfig().graveConfig.dropItemsIfDestroyed) {
            this.dropAll();
        }
    }

    public LightGraveData toLightData() {
        return new LightGraveData(this.inventoryComponent.graveSize(), this.pos,
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
        nbt.putBoolean("locked", this.locked);
        nbt.put("creationTime", this.creationTime.toNbt());
        if (this.killerId != null) nbt.putUuid("killerId", this.killerId);


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
        TimePoint creationTime = TimePoint.fromNbt(nbt.getCompound("creationTime"));
        UUID killerId = nbt.contains("killerId") ? nbt.getUuid("killerId") : null;

        if (server != null) {
            ServerWorld world = server.getWorld(worldKey);
            if (world != null) {
                return new GraveComponent(owner, inventoryComponent, expComponent, world, pos, deathMessage, graveId, status, locked, creationTime, killerId);
            }
        }
        return new GraveComponent(owner, inventoryComponent, expComponent, worldKey, pos, deathMessage, graveId, status, locked, creationTime, killerId);
    }
    private static RegistryKey<World> getRegistryKeyFromNbt(NbtCompound nbt) {
        String registry = nbt.getString("registry");
        String value = nbt.getString("value");

        RegistryKey<Registry<World>> r = RegistryKey.ofRegistry(new Identifier(registry));
        return RegistryKey.of(r, new Identifier(value));
    }
}
