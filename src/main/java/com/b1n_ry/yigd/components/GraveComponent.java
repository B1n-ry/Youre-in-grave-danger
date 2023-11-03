package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.ClaimPriority;
import com.b1n_ry.yigd.config.DropType;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.*;
import com.b1n_ry.yigd.events.AllowBlockUnderGraveGenerationEvent;
import com.b1n_ry.yigd.events.DropItemEvent;
import com.b1n_ry.yigd.events.GraveClaimEvent;
import com.b1n_ry.yigd.events.GraveGenerationEvent;
import com.b1n_ry.yigd.packets.LightGraveData;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionTypes;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GraveComponent {
    private final GameProfile owner;
    private final InventoryComponent inventoryComponent;
    private final ExpComponent expComponent;
    /**
     * world should never be null while on server, but only on client.
     * If this is compromised, the mod might crash
     */
    @Nullable
    private ServerWorld world;
    private final RegistryKey<World> worldRegistryKey;
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

    public ExpComponent getExpComponent() {
        return this.expComponent;
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
    public void setStatus(GraveStatus status) {
        this.status = status;
        DeathInfoManager.INSTANCE.markDirty();
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
            i++;
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
        int topY = this.world.getTopY();
        if (y > topY) {
            y = topY;
        }

        int x = attemptedPos.getX();
        int z = attemptedPos.getZ();
        if (config.generateOnlyWithinBorder) {
            WorldBorder border = this.world.getWorldBorder();
            if (!border.contains(x, z)) {
                x = (int) Math.max(x, border.getBoundEast());
                x = (int) Math.min(x, border.getBoundWest());

                z = (int) Math.max(z, border.getBoundSouth());
                z = (int) Math.min(z, border.getBoundNorth());
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

    void placeBlockUnder() {
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

    public void backUp() {
        DeathInfoManager.INSTANCE.addBackup(this.owner, this);
        DeathInfoManager.INSTANCE.markDirty();
    }

    public boolean hasExistedMs(long time) {
        if (this.world == null) return false;

        return this.world.getTime() - this.creationTime.getTime() < time;
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
            this.applyToPlayer(player, world, pos, thisIsARobbery);

            if (addGraveItem)
                player.giveItemStack(graveItem);
        } else if (config.graveConfig.dropOnRetrieve == DropType.ON_GROUND) {
            this.dropAll();

            if (this.world != null && addGraveItem)
                ItemScatterer.spawn(this.world, this.pos.getX(), this.pos.getY(), this.pos.getZ(), graveItem);
        }

        if (!config.graveConfig.persistentGraves.enabled) {
            if (config.graveConfig.replaceOldWhenClaimed && previousState != null) {
                world.setBlockState(pos, previousState);
            } else {
                world.removeBlock(pos, false);
            }
        } else {
            GraveBlockEntity be = (GraveBlockEntity) world.getBlockEntity(pos);
            if (be != null)
                be.setClaimed(true);
        }

        this.status = GraveStatus.CLAIMED;
        DeathInfoManager.INSTANCE.markDirty();

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
            return this.world.setBlockState(this.pos, previousState);
        }
    }

    private void handleRandomSpawn(YigdConfig.GraveConfig.RandomSpawn config, ServerWorld world, GameProfile looter) {
        if (config.percentSpawnChance <= world.random.nextInt(100)) return;  // Using world's random (from world seed)
        String summonNbt = config.spawnNbt
                .replaceAll("\\$\\{owner\\.name}", this.owner.getName())
                .replaceAll("\\$\\{owner\\.uuid}", this.owner.getId().toString())
                .replaceAll("\\$\\{looter\\.name}", looter.getName())
                .replaceAll("\\$\\{looter\\.uuid}", looter.getId().toString());

        // While the nbt string has an item to add (text contains "${item[i]}")
        Matcher nbtMatcher;
        DefaultedList<ItemStack> items = this.inventoryComponent.getItems();
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
            ItemStack item = items.get(itemNumber);
            NbtCompound itemNbt = item.getNbt();
            NbtCompound newNbt = new NbtCompound();
            newNbt.put("tag", itemNbt);
            newNbt.putString("id", Registries.ITEM.getId(item.getItem()).toString());
            newNbt.putInt("Count", item.getCount());

            boolean removeItem = summonNbt.contains("${!item[" + itemNumber + "]}"); // Contains ! -> remove item from list later

            summonNbt = summonNbt.replaceAll("\\$\\{!?item\\[" + itemNumber + "]}", newNbt.asString());

            if (removeItem) items.set(itemNumber, ItemStack.EMPTY); // Make sure item gets "used"
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

    public void applyToPlayer(ServerPlayerEntity player, ServerWorld world, BlockPos pos, boolean isGraveOwner) {
        YigdConfig config = YigdConfig.getConfig();

        this.expComponent.applyToPlayer(player);

        InventoryComponent currentPlayerInv = new InventoryComponent(player);
        InventoryComponent.clearPlayer(player);

        DefaultedList<ItemStack> extraItems = DefaultedList.of();

        UUID playerId = player.getUuid();
        ClaimPriority claimPriority = Yigd.CLAIM_PRIORITIES.containsKey(playerId) ? Yigd.CLAIM_PRIORITIES.get(playerId) : config.graveConfig.claimPriority;
        ClaimPriority robPriority = Yigd.ROB_PRIORITIES.containsKey(playerId) ? Yigd.CLAIM_PRIORITIES.get(playerId) : config.graveConfig.graveRobbing.robPriority;

        ClaimPriority priority = isGraveOwner ? claimPriority : robPriority;

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

    public void dropAll() {
        this.inventoryComponent.dropAll(this.world, this.pos.toCenterPos());
        this.expComponent.dropAll(this.world, this.pos.toCenterPos());
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
//        nbt.putLong("creationTime", this.creationTime);
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
            if (world == null) {
                Yigd.LOGGER.error("World " + worldKey.toString() + " not recognized. Loading grave component without world");
            } else {
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
