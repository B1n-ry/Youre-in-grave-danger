package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.ClaimPriority;
import com.b1n_ry.yigd.config.DropType;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.*;
import com.b1n_ry.yigd.events.*;
import com.b1n_ry.yigd.networking.LightGraveData;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.util.GraveCompassHelper;
import com.b1n_ry.yigd.util.GraveOverrideAreas;
import com.b1n_ry.yigd.util.YigdTags;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GraveComponent {
    private final ResolvableProfile owner;
    private InventoryComponent inventoryComponent;
    private ExpComponent expComponent;
    /**
     * world should never be null while on server, but only on client.
     * If this is compromised, the mod might crash
     */
    @Nullable
    private ServerLevel world;
    private ResourceKey<Level> worldRegistryKey;
    private BlockPos pos;
    private final Component deathMessage;
    private final UUID graveId;
    private GraveStatus status;
    private boolean locked;
    private final TimePoint creationTime;
    private final UUID killerId;

    public static GraveyardData graveyardData = null;

    public GraveComponent(ResolvableProfile owner, InventoryComponent inventoryComponent, ExpComponent expComponent, ServerLevel world, Vec3 pos, Component deathMessage, UUID killerId) {
        this(owner, inventoryComponent, expComponent, world, BlockPos.containing(pos), deathMessage, UUID.randomUUID(), GraveStatus.UNCLAIMED, true, new TimePoint(world), killerId);
    }
    public GraveComponent(ResolvableProfile owner, InventoryComponent inventoryComponent, ExpComponent expComponent, ServerLevel world,
                          BlockPos pos, Component deathMessage, UUID graveId, GraveStatus status, boolean locked, TimePoint creationTime, UUID killerId) {
        this.owner = owner;
        this.inventoryComponent = inventoryComponent;
        this.expComponent = expComponent;
        this.world = world;
        this.worldRegistryKey = world.dimension();
        this.pos = pos;
        this.deathMessage = deathMessage;
        this.graveId = graveId;
        this.status = status;
        this.locked = locked;
        this.creationTime = creationTime;
        this.killerId = killerId;
    }
    public GraveComponent(ResolvableProfile owner, InventoryComponent inventoryComponent, ExpComponent expComponent, ResourceKey<Level> worldKey,
                          BlockPos pos, Component deathMessage, UUID graveId, GraveStatus status, boolean locked, TimePoint creationTime, UUID killerId) {
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

    public ResolvableProfile getOwner() {
        return this.owner;
    }

    public InventoryComponent getInventoryComponent() {
        return this.inventoryComponent;
    }
    public void setInventoryComponent(InventoryComponent inventoryComponent) {
        this.inventoryComponent = inventoryComponent;
        DeathInfoManager.INSTANCE.setDirty();
    }

    public ExpComponent getExpComponent() {
        return this.expComponent;
    }
    public void setExpComponent(ExpComponent expComponent) {
        this.expComponent = expComponent;
        DeathInfoManager.INSTANCE.setDirty();
    }
    /**
     * While on server, this will never return null
     * @return the world the component belongs to. Null if on client
     */
    public @Nullable ServerLevel getWorld() {
        return this.world;
    }
    public ResourceKey<Level> getWorldRegistryKey() {
        return this.worldRegistryKey;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public Component getDeathMessage() {
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
        DeathInfoManager.INSTANCE.setDirty();
    }
    public void setPos(BlockPos pos) {
        this.pos = pos;
        DeathInfoManager.INSTANCE.setDirty();
    }
    public void setWorld(ServerLevel world) {
        this.world = world;
        this.worldRegistryKey = world.dimension();
    }
    public void setStatus(GraveStatus status) {
        if (this.status == GraveStatus.UNCLAIMED
                && YigdConfig.getConfig().extraFeatures.graveCompass.pointToClosest != YigdConfig.ExtraFeatures.GraveCompassConfig.CompassGraveTarget.DISABLED) {
            GraveCompassHelper.setClaimed(this.worldRegistryKey, this.pos);
        }
        this.status = status;
        DeathInfoManager.INSTANCE.setDirty();
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
            for (BlockPos iPos : BlockPos.withinManhattan(this.pos, generationMaxDistance.x, generationMaxDistance.y, generationMaxDistance.z)) {
                YigdEvents.GraveGenerationEvent event = NeoForge.EVENT_BUS.post(new YigdEvents.GraveGenerationEvent(this.world, iPos, i));
                if (event.canGenerate()) {
                    this.pos = iPos;
                    DeathInfoManager.INSTANCE.setDirty();
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
        ServerLevel graveyardWorld = server.getLevel(ResourceKey.create(Registries.DIMENSION, graveyardData.dimensionId));
        if (graveyardWorld == null) {
            graveyardWorld = server.overworld();
        }
        DirectionalPos closest = null;
        for (GraveyardData.GraveLocation location : graveyardData.graveLocations) {
            Optional<String> maybeName = this.owner.name();
            if (location.forPlayer != null && !location.forPlayer.equalsIgnoreCase(maybeName.orElse("")))
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
        int lowerAcceptableY = config.lowestGraveY + this.world.getMinBuildHeight();
        if (config.generateGraveInVoid && attemptedPos.getY() <= lowerAcceptableY) {
            y = lowerAcceptableY;
        }
        int topY = this.world.getMaxBuildHeight() - 1;  // Can't actually place blocks at top Y
        if (y > topY) {
            y = topY;
        }

        int x = attemptedPos.getX();
        int z = attemptedPos.getZ();
        if (config.generateOnlyWithinBorder) {
            WorldBorder border = this.world.getWorldBorder();
            if (!border.isWithinBounds(x, z)) {
                x = (int) Math.max(x, border.getMinX());
                x = (int) Math.min(x, border.getMaxX());

                z = (int) Math.max(z, border.getMinZ());
                z = (int) Math.min(z, border.getMaxZ());
            }
        }

        this.pos = new BlockPos(x, y, z);

        // Makes sure the grave is not broken/replaced by portal, or the dragon egg
        if (this.world.dimension().equals(Level.END)) {
            if (Math.abs(attemptedPos.getX()) + Math.abs(attemptedPos.getZ()) < 25 && this.world.getBlockState(attemptedPos.below()).is(Blocks.BEDROCK))
                this.pos = this.pos.above();
        }

        DeathInfoManager.INSTANCE.setDirty();  // The "this" object is (at least should be) located inside DeathInfoManager.INSTANCE

        this.placeBlockUnder();
        return this.world.setBlockAndUpdate(this.pos, state);
    }

    public void placeAndLoad(Direction direction, DeathContext context, BlockPos pos, RespawnComponent respawnComponent) {
        YigdConfig config = YigdConfig.getConfig();

        ServerLevel world = context.world();
        Vec3 deathPos = context.deathPos();

        // Check storage options first, in case that will lead to empty graves
        if (!config.graveConfig.storeItems) {
            this.inventoryComponent.dropGraveItems(world, deathPos);
        }
        if (!config.graveConfig.storeXp) {
            this.expComponent.dropAll(world, deathPos);
            this.getExpComponent().clear();
        }

        boolean waterlogged = world.getFluidState(pos).is(Fluids.WATER);  // Grave generated in full water block (submerged)
        BlockState graveBlock = Yigd.GRAVE.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, direction)
                .setValue(BlockStateProperties.WATERLOGGED, waterlogged);

        // At this point is where the END_OF_TICK would be implemented, unless it wasn't already so
        Yigd.END_OF_TICK.add(() -> {
            BlockState previousState = world.getBlockState(pos);

            boolean placed = this.tryPlaceGraveAt(pos, graveBlock);
            BlockPos placedPos = this.getPos();

            if (!placed) {
                Yigd.LOGGER.error("Failed to generate grave at X: {}, Y: {}, Z: {}, {}. Grave block placement failed",
                        placedPos.getX(), placedPos.getY(), placedPos.getZ(), world.dimension().location());
                Yigd.LOGGER.info("Dropping items on ground instead of in grave");

                context.player().sendSystemMessage(Component.translatable("text.yigd.message.grave_generation_error"));
                this.getInventoryComponent().dropGraveItems(world, Vec3.atLowerCornerOf(placedPos));
                this.getExpComponent().dropAll(world, Vec3.atLowerCornerOf(placedPos));
                return;
            }

            respawnComponent.setGraveGenerated(true);  // Not guaranteed yet, but only errors can stop it from generating after this point
            DeathInfoManager.INSTANCE.setDirty();  // Make sure respawn component is updated

            GraveBlockEntity be = (GraveBlockEntity) world.getBlockEntity(placedPos);
            if (be == null) return;
            be.setPreviousState(previousState);
            be.setComponent(this);
        });
    }

    public void generateOrDrop(Direction playerDirection, DeathContext context, RespawnComponent respawnComponent) {
        ServerLevel world = context.world();
        Vec3 pos = context.deathPos();
        YigdEvents.AllowGraveGenerationEvent event = NeoForge.EVENT_BUS.post(new YigdEvents.AllowGraveGenerationEvent(context, this));
        if (!event.isGenerationAllowed()) {
            this.inventoryComponent.dropGraveItems(world, pos);
            this.expComponent.dropAll(world, pos);
        } else {
            DirectionalPos dirGravePos = this.findGravePos(playerDirection);
            BlockPos gravePos = dirGravePos.pos();
            Direction direction = dirGravePos.dir();

            ServerLevel graveWorld = this.getWorld();
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

        BlockState currentUnder = this.world.getBlockState(this.pos.below());
        YigdEvents.AllowBlockUnderGraveGenerationEvent event = NeoForge.EVENT_BUS.post(new YigdEvents.AllowBlockUnderGraveGenerationEvent(this, currentUnder));
        if (!event.isPlacementAllowed()) return;

        Map<String, String> blockInDimMap = new HashMap<>();
        for (YigdConfig.MapEntry pair : config.blockInDimensions) {
            blockInDimMap.put(pair.key, pair.value);
        }

        String dimName = this.worldRegistryKey.location().toString();
        if (!blockInDimMap.containsKey(dimName)) dimName = "misc";

        String blockName = blockInDimMap.get(dimName);
        if (blockName == null) {
            Yigd.LOGGER.warn("Didn't place supporting block under grave in %s, at %d, %d, %d. Couldn't find dimension key in config"
                    .formatted(dimName, this.pos.getX(), this.pos.getY(), this.pos.getZ()));
            return;
        }

        Block blockUnder = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockName));
        boolean placed = this.world.setBlockAndUpdate(this.pos.below(), blockUnder.defaultBlockState());
        if (!placed) {
            Yigd.LOGGER.warn("Didn't place supporting block under grave in %s, at %d, %d, %d. Block placement failed"
                    .formatted(dimName, this.pos.getX(), this.pos.getY(), this.pos.getZ()));
        }
    }

    /**
     * Replaces the grave with the block that was there before the grave was placed (or air if feature is disabled)
     * @param newState The block that should be placed instead of the grave (previous state)
     * @return Weather or not the block was replaced
     */
    public boolean replaceWithOld(BlockState newState) {
        if (this.world == null) return false;
        if (newState.is(YigdTags.REPLACE_GRAVE_BLACKLIST)) return false;

        boolean placed = this.world.setBlockAndUpdate(this.pos, newState);  // Place the block
        // Although no player placed the block, we still need to update it in case the block is multipart
        newState.getBlock().setPlacedBy(this.world, this.pos, newState, null, ItemStack.EMPTY);

        return placed;
    }

    public void backUp() {
        DeathInfoManager.INSTANCE.addBackup(this.owner, this);
        DeathInfoManager.INSTANCE.setDirty();
    }

    public boolean hasExistedTicks(long time) {
        if (this.world == null) return false;

        return this.world.getGameTime() - this.creationTime.getTime() >= time;
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

        long timePassed = (this.creationTime.getTime() - this.world.getGameTime() + delay) / tps;
        long seconds = timePassed % 60;
        long minutes = (timePassed / 60) % 60;
        long hours = timePassed / 3600;
        return "%02d:%02d:%02d".formatted(hours, minutes, seconds);
    }

    public InteractionResult claim(ServerPlayer player, ServerLevel world, BlockState previousState, BlockPos pos, ItemStack tool) {
        YigdConfig config = YigdConfig.getConfig();

        if (this.status == GraveStatus.CLAIMED) return InteractionResult.FAIL;  // Otherwise runs twice when persistent graves is enabled
        YigdEvents.GraveClaimEvent event = NeoForge.EVENT_BUS.post(new YigdEvents.GraveClaimEvent(player, world, pos, this, tool));
        if (!event.allowClaim()) return InteractionResult.FAIL;

        this.handleRandomSpawn(config.graveConfig.randomSpawn, world, player.getGameProfile());

        boolean thisIsARobbery = !player.getUUID().equals(this.owner.id().orElse(null));

        ItemStack graveItem = new ItemStack(Yigd.GRAVE.asItem());
        boolean addGraveItem = config.graveConfig.dropGraveBlock;
        if (config.graveConfig.dropOnRetrieve == DropType.IN_INVENTORY) {
            this.applyToPlayer(player, world, pos.getCenter(), !thisIsARobbery);

            if (addGraveItem)
                player.addItem(graveItem);
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
                be.setChanged();
                world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            }
        }

        if (thisIsARobbery && config.graveConfig.graveRobbing.notifyWhenRobbed) {
            MinecraftServer server = world.getServer();
            String robberName = player.getGameProfile().getName();
            Optional<UUID> ownerId = this.owner.id();
            ServerPlayer robbedPlayer = ownerId.map(value -> server.getPlayerList().getPlayer(value)).orElse(null);
            if (robbedPlayer != null) {  // They are not offline. They are online
                if (config.graveConfig.graveRobbing.tellWhoRobbed) {
                    robbedPlayer.sendSystemMessage(Component.translatable("text.yigd.message.inform_robbery.with_details", player.getGameProfile().getName()));
                } else {
                    robbedPlayer.sendSystemMessage(Component.translatable("text.yigd.message.inform_robbery"));
                }
            } else {
                ownerId.ifPresent(value -> Yigd.NOT_NOTIFIED_ROBBERIES.computeIfAbsent(value, uuid -> new ArrayList<>()).add(robberName));
            }
        }

        Yigd.LOGGER.info("{} claimed a grave belonging to {} at {}, {}, {}, {}", player.getGameProfile().getName(),
                this.owner.name().orElse("PLAYER_NOT_FOUND"), this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.worldRegistryKey.location());
        return InteractionResult.SUCCESS;
    }

    /**
     * Will remove the grave block associated with the component (if it exists)<br>
     * <u>DO NOTE</u>: Unless status for the grave is changed from UNCLAIMED <i>before</i> called, status will be set to DESTROYED
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

    private void handleRandomSpawn(YigdConfig.GraveConfig.RandomSpawn config, ServerLevel world, GameProfile looter) {
        if (config.percentSpawnChance <= world.random.nextInt(100)) return;  // Using world's random (from world seed)
        IntArrayTag ownerIdNbt = this.owner.id().map(NbtUtils::createUUID).orElse(new IntArrayTag(new int[0]));
        IntArrayTag looterIdNbt = NbtUtils.createUUID(looter.getId());

        String summonNbt = config.spawnNbt
                .replaceAll("\\$\\{owner\\.name}", this.owner.name().orElse("Steve"))
                .replaceAll("\\$\\{owner\\.uuid}", ownerIdNbt.toString())
                .replaceAll("\\$\\{looter\\.name}", looter.getName())
                .replaceAll("\\$\\{looter\\.uuid}", looterIdNbt.toString());

        // While the nbt string has an item to add (text contains "${item[i]}")
        Matcher nbtMatcher;
        NonNullList<Tuple<ItemStack, DropRule>> items = this.inventoryComponent.getItems();
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
            ItemStack item = items.get(itemNumber).getA();
            CompoundTag itemNbt = (CompoundTag) item.save(world.registryAccess());

            boolean removeItem = summonNbt.contains("${!item[" + itemNumber + "]}"); // Contains ! -> remove item from list later

            summonNbt = summonNbt.replaceAll("\\$\\{!?item\\[" + itemNumber + "]}", itemNbt.toString());

            if (removeItem) items.set(itemNumber, new Tuple<>(ItemStack.EMPTY, GraveOverrideAreas.INSTANCE.defaultDropRule)); // Make sure item gets "used"
        } while (nbtMatcher.find());  // Loop until no more items should be inserted in NBT

        try {
            CompoundTag nbt = NbtUtils.snbtToStructure(summonNbt);
            nbt.putString("id", config.spawnEntity);
            Entity entity = EntityType.loadEntityRecursive(nbt, world, e -> {
                e.moveTo(this.pos, e.getYRot(), e.getXRot());  // Make sure the entity is in the right place
                return e;
            });

            if (entity == null) return;

            world.addFreshEntity(entity);
        } catch (CommandSyntaxException e) {
            Yigd.LOGGER.error("Failed spawning entity on grave", e);
        }
    }

    public void applyToPlayer(ServerPlayer player, ServerLevel world, Vec3 pos, boolean isGraveOwner) {
        this.applyToPlayer(player, world, pos, isGraveOwner, dropRule -> dropRule == DropRule.PUT_IN_GRAVE);
    }
    public void applyToPlayer(ServerPlayer player, ServerLevel world, Vec3 pos, boolean isGraveOwner, Predicate<DropRule> itemFilter) {
        YigdConfig config = YigdConfig.getConfig();

        this.expComponent.applyToPlayer(player);

        InventoryComponent currentPlayerInv = new InventoryComponent(player);
        InventoryComponent.clearPlayer(player);

        NonNullList<ItemStack> extraItems = NonNullList.create();

        UUID playerId = player.getUUID();
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
            if (player.addItem(stack))
                continue;
            double x = pos.x();
            double y = pos.y();
            double z = pos.z();
            InventoryComponent.dropItemIfToBeDropped(stack, x, y, z, world);
        }
    }

    public void dropAll() {
        this.inventoryComponent.dropAll(this.world, this.pos.getCenter());
        this.expComponent.dropAll(this.world, this.pos.getCenter());
    }

    public void onDestroyed() {
        this.setStatus(GraveStatus.DESTROYED);

        if (this.world == null) return;  // Should not be the case. But this is instead of an assert that could crash the game if another mod used this method incorrectly
        PlayerList playerManager = this.world.getServer().getPlayerList();
        ServerPlayer owner = this.owner.id().map(playerManager::getPlayer).orElse(null);
        if (owner == null) return;

        YigdConfig config = YigdConfig.getConfig();

        if (config.graveConfig.notifyOwnerIfDestroyed) {
            owner.sendSystemMessage(Component.translatable("text.yigd.message.grave_destroyed"));
        }

        if (YigdConfig.getConfig().graveConfig.dropItemsIfDestroyed) {
            this.dropAll();
        }
    }

    public LightGraveData toLightData() {
        return new LightGraveData(this.inventoryComponent.graveSize(), this.pos,
                this.expComponent.getStoredXp(), this.worldRegistryKey, this.deathMessage, this.graveId, this.status);
    }

    public CompoundTag toNbt(HolderLookup.Provider lookupRegistry) {
        CompoundTag nbt = new CompoundTag();
        nbt.put("owner", ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, this.owner).getOrThrow());
        nbt.put("inventory", this.inventoryComponent.toNbt(lookupRegistry));
        nbt.put("exp", this.expComponent.toNbt());

        nbt.put("world", this.getWorldRegistryKeyNbt(this.worldRegistryKey));
        nbt.put("pos", NbtUtils.writeBlockPos(this.pos));
        nbt.putString("deathMessage", Component.Serializer.toJson(this.deathMessage, lookupRegistry));
        nbt.putUUID("graveId", this.graveId);
        nbt.putString("status", this.status.toString());
        nbt.putBoolean("locked", this.locked);
        nbt.put("creationTime", this.creationTime.toNbt());
        if (this.killerId != null) nbt.putUUID("killerId", this.killerId);


        return nbt;
    }
    private CompoundTag getWorldRegistryKeyNbt(ResourceKey<?> key) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("registry", key.registry().toString());
        nbt.putString("value", key.location().toString());

        return nbt;
    }

    public static GraveComponent fromNbt(CompoundTag nbt, HolderLookup.Provider lookupRegistry, @Nullable MinecraftServer server) {
        if (nbt == null) {
            return null;
        }
        ResolvableProfile owner = ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, nbt.get("owner")).getOrThrow();
        InventoryComponent inventoryComponent = InventoryComponent.fromNbt(nbt.getCompound("inventory"), lookupRegistry);
        ExpComponent expComponent = ExpComponent.fromNbt(nbt.getCompound("exp"));
        ResourceKey<Level> worldKey = getRegistryKeyFromNbt(nbt.getCompound("world"));
        Optional<BlockPos> pos = NbtUtils.readBlockPos(nbt, "pos");
        Component deathMessage = Component.Serializer.fromJson(nbt.getString("deathMessage"), lookupRegistry);
        UUID graveId = nbt.getUUID("graveId");
        GraveStatus status = GraveStatus.valueOf(nbt.getString("status"));
        boolean locked = nbt.getBoolean("locked");
        TimePoint creationTime = TimePoint.fromNbt(nbt.getCompound("creationTime"));
        UUID killerId = nbt.contains("killerId") ? nbt.getUUID("killerId") : null;

        if (server != null) {
            ServerLevel world = server.getLevel(worldKey);
            if (world != null) {
                return new GraveComponent(owner, inventoryComponent, expComponent, world, pos.orElse(BlockPos.ZERO), deathMessage, graveId, status, locked, creationTime, killerId);
            }
        }
        return new GraveComponent(owner, inventoryComponent, expComponent, worldKey, pos.orElse(BlockPos.ZERO), deathMessage, graveId, status, locked, creationTime, killerId);
    }
    private static ResourceKey<Level> getRegistryKeyFromNbt(CompoundTag nbt) {
        String registry = nbt.getString("registry");
        String value = nbt.getString("value");

        ResourceKey<Registry<Level>> r = ResourceKey.createRegistryKey(ResourceLocation.parse(registry));
        return ResourceKey.create(r, ResourceLocation.parse(value));
    }
}
