package com.b1n_ry.yigd.core;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.YigdApi;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.EntityDamageSource;
import net.minecraft.entity.damage.ProjectileDamageSource;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.minecraft.entity.damage.DamageSource.GENERIC;

public class DeadPlayerData {
    public DefaultedList<ItemStack> inventory;
    public List<Object> modInventories;
    public BlockPos gravePos;
    public GameProfile graveOwner;
    public int xp;
    public Identifier worldId;
    public String dimensionName;
    public DamageSource deathSource;

    public UUID id;
    public byte availability; // 0 = retrieved, 1 = available, -1 = broken/missing

    public static DeadPlayerData create(DefaultedList<ItemStack> inventory, List<Object> modInventories, BlockPos gravePos, GameProfile graveOwner , int xp, World world, DamageSource deathSource, UUID id) {
        Identifier dimId = world.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).getId(world.getDimension());
        String dimName = dimId != null ? dimId.getPath() : "void";
        return new DeadPlayerData(inventory, modInventories, gravePos, graveOwner, xp, world.getRegistryKey().getValue(), dimName, deathSource, (byte) 1, id);
    }
    public DeadPlayerData(DefaultedList<ItemStack> inventory, List<Object> modInventories, BlockPos gravePos, GameProfile graveOwner, int xp, Identifier worldId, String dimensionName, DamageSource deathSource, byte availability, UUID id) {
        this.inventory = inventory;
        this.modInventories = modInventories;
        this.gravePos = gravePos;
        this.graveOwner = graveOwner;
        this.xp = xp;
        this.worldId = worldId;
        this.deathSource = deathSource;
        this.dimensionName = dimensionName;
        this.availability = availability;
        this.id = id;
    }

    // Convert the DeadPlayerData object to NBT
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();

        NbtCompound invNbt = Inventories.writeNbt(new NbtCompound(), this.inventory);
        NbtCompound modNbt = new NbtCompound();
        for (int i = 0; i < modInventories.size(); i++) {
            YigdApi yigdApi = Yigd.apiMods.get(i);
            modNbt.put(yigdApi.getModName(), yigdApi.writeNbt(modInventories.get(i)));
        }
        NbtCompound posNbt = NbtHelper.fromBlockPos(this.gravePos);
        NbtCompound graveOwnerNbt = NbtHelper.writeGameProfile(new NbtCompound(), this.graveOwner);

        String worldId = this.worldId.toString();

        NbtCompound dsNbt = new NbtCompound();
        dsNbt.putString("name", this.deathSource.name);
        if (this.deathSource.getSource() != null) {
            dsNbt.putUuid("sourceUuid", deathSource.getSource().getUuid());
        }
        if (deathSource.getAttacker() != null) {
            dsNbt.putUuid("attackerUuid", deathSource.getAttacker().getUuid());
        }

        nbt.put("inventory", invNbt);
        nbt.putInt("inventorySize", this.inventory.size());
        nbt.put("modInventory", modNbt);
        nbt.put("gravePos", posNbt);
        nbt.put("owner", graveOwnerNbt);
        nbt.putInt("xp", this.xp);
        nbt.putString("world", worldId);
        nbt.putString("dimension", this.dimensionName);
        nbt.put("causeOfDeath", dsNbt);
        nbt.putByte("availability", this.availability);
        nbt.putUuid("id", this.id);

        return nbt;
    }

    // Create a new DeadPlayerData instance from NBT
    public static DeadPlayerData fromNbt(NbtCompound nbt) {
        return fromNbt(nbt, null);
    }
    public static DeadPlayerData fromNbt(NbtCompound nbt, @Nullable ServerWorld world) {
        NbtElement itemNbt = nbt.get("inventory");
        int invSize = nbt.getInt("inventorySize");
        DefaultedList<ItemStack> items = DefaultedList.ofSize(invSize, ItemStack.EMPTY);
        if (itemNbt instanceof NbtCompound itemNbtCompound) {
            Inventories.readNbt(itemNbtCompound, items);
        }
        NbtElement modNbt = nbt.get("modInventory");
        List<Object> modInventories = new ArrayList<>();
        if (modNbt instanceof NbtCompound modNbtCompound) {
            for (int i = 0; i < Yigd.apiMods.size(); i++) {
                YigdApi yigdApi = Yigd.apiMods.get(i);
                NbtElement e = modNbtCompound.get(yigdApi.getModName());
                if (!(e instanceof NbtCompound c)) continue;

                modInventories.add(yigdApi.readNbt(c));
            }
        }
        BlockPos pos;
        NbtElement blockPosNbt = nbt.get("gravePos");
        if (blockPosNbt instanceof NbtCompound c) {
            pos = NbtHelper.toBlockPos(c);
        } else {
            pos = null;
        }

        NbtElement ownerElement = nbt.get("owner");
        GameProfile graveOwner;
        if (ownerElement instanceof NbtCompound ownerNbt) {
            graveOwner = NbtHelper.toGameProfile(ownerNbt);
        } else {
            graveOwner = null;
        }

        int xp = nbt.getInt("xp");
        String worldId = nbt.getString("world");
        Identifier worldIdentifier = new Identifier(worldId);
        String dimName = nbt.getString("dimension");
        byte availability = nbt.contains("availability") ? nbt.getByte("availability") : 0;
        UUID id = nbt.contains("id") ? nbt.getUuid("id") : UUID.randomUUID();

        NbtElement damageSourceNbt = nbt.get("causeOfDeath");
        DamageSource deathSource;
        if (damageSourceNbt instanceof NbtCompound dsNbt) {
            String sourceName = dsNbt.getString("name");
            Entity source, attacker;
            if (world != null) {
                source = world.getEntity(dsNbt.getUuid("sourceUuid"));
                attacker = world.getEntity(dsNbt.getUuid("attackerUuid"));
            } else {
                source = null;
                attacker = null;
            }
            if (source != null) {
                if (source.equals(attacker)) { // Entity attacker and source
                    deathSource = new EntityDamageSource(sourceName, source);
                } else { // Projectile source and entity attacker
                    deathSource = new ProjectileDamageSource(sourceName, source, attacker);
                }
            } else {
                switch (sourceName) {
                    case "inFire" -> deathSource = DamageSource.IN_FIRE;
                    case "lightningBolt" -> deathSource = DamageSource.LIGHTNING_BOLT;
                    case "onFire" -> deathSource = DamageSource.ON_FIRE;
                    case "lava" -> deathSource = DamageSource.LAVA;
                    case "hotFloor" -> deathSource = DamageSource.HOT_FLOOR;
                    case "inWall" -> deathSource = DamageSource.IN_WALL;
                    case "cramming" -> deathSource = DamageSource.CRAMMING;
                    case "drown" -> deathSource = DamageSource.DROWN;
                    case "starve" -> deathSource = DamageSource.STARVE;
                    case "cactus" -> deathSource = DamageSource.CACTUS;
                    case "fall" -> deathSource = DamageSource.FALL;
                    case "flyIntoWall" -> deathSource = DamageSource.FLY_INTO_WALL;
                    case "outOfWorld" -> deathSource = DamageSource.OUT_OF_WORLD;
                    case "magic" -> deathSource = DamageSource.MAGIC;
                    case "wither" -> deathSource = DamageSource.WITHER;
                    case "anvil" -> deathSource = DamageSource.ANVIL;
                    case "fallingBlock" -> deathSource = DamageSource.FALLING_BLOCK;
                    case "dragonBreath" -> deathSource = DamageSource.DRAGON_BREATH;
                    case "dryout" -> deathSource = DamageSource.DRYOUT;
                    case "sweetBerryBush" -> deathSource = DamageSource.SWEET_BERRY_BUSH;
                    case "freeze" -> deathSource = DamageSource.FREEZE;
                    case "fallingStalactite" -> deathSource = DamageSource.FALLING_STALACTITE;
                    case "stalagmite" -> deathSource = DamageSource.STALAGMITE;
                    default -> deathSource = GENERIC;
                }
            }
        } else {
            deathSource = null;
        }

        return new DeadPlayerData(items, modInventories, pos, graveOwner, xp, worldIdentifier, dimName, deathSource, availability, id);
    }

    public static class Soulbound {
        private static final Map<UUID, DefaultedList<ItemStack>> soulboundInventories = new HashMap<>();
        private static final Map<UUID, List<Object>> moddedSoulbound = new HashMap<>();

        public static DefaultedList<ItemStack> getSoulboundInventory(UUID userId) {
            return soulboundInventories.get(userId);
        }
        public static List<Object> getModdedSoulbound(UUID userId) {
            return moddedSoulbound.get(userId);
        }
        public static void setSoulboundInventories(UUID userId, DefaultedList<ItemStack> soulboundItems) {
            dropSoulbound(userId);
            soulboundInventories.put(userId, soulboundItems);
            DeathInfoManager.INSTANCE.markDirty();
        }
        public static void addModdedSoulbound(UUID userId, Object modInventory) {
            if (!moddedSoulbound.containsKey(userId)) {
                moddedSoulbound.put(userId, new ArrayList<>());
            }
            moddedSoulbound.get(userId).add(modInventory);
            DeathInfoManager.INSTANCE.markDirty();
        }

        public static void dropSoulbound(UUID userId) {
            soulboundInventories.remove(userId);
            DeathInfoManager.INSTANCE.markDirty();
        }
        public static void dropModdedSoulbound(UUID userId) {
            moddedSoulbound.remove(userId);
            DeathInfoManager.INSTANCE.markDirty();
        }

        public static NbtCompound getNbt() {
            NbtCompound nbt = new NbtCompound();

            NbtList vanillaList = new NbtList();
            soulboundInventories.forEach((uuid, itemStacks) -> {
                NbtCompound playerNbt = Inventories.writeNbt(new NbtCompound(), itemStacks);
                playerNbt.putInt("inventorySize", itemStacks.size());
                playerNbt.putUuid("user", uuid);

                vanillaList.add(playerNbt);
            });
            NbtList modList = new NbtList();
            moddedSoulbound.forEach((uuid, objects) -> {
                NbtCompound playerNbt = new NbtCompound();
                NbtCompound inventories = new NbtCompound();
                for (int i = 0; i < objects.size(); i++) {
                    YigdApi yigdApi = Yigd.apiMods.get(i);
                    inventories.put(yigdApi.getModName(), yigdApi.writeNbt(objects.get(i)));
                }
                playerNbt.put("Inventories", inventories);
                playerNbt.putUuid("user", uuid);

                modList.add(playerNbt);
            });

            nbt.put("vanilla", vanillaList);
            nbt.put("mods", modList);

            return nbt;
        }

        public static void fromNbt(NbtCompound nbt) {
            soulboundInventories.clear();
            moddedSoulbound.clear();

            NbtList vanillaList = nbt.getList("vanilla", NbtElement.COMPOUND_TYPE);
            NbtList modList = nbt.getList("mods", NbtElement.COMPOUND_TYPE);

            for (NbtElement e : vanillaList) {
                if (!(e instanceof NbtCompound cVanilla)) continue;
                int itemSize = cVanilla.getInt("inventorySize");
                DefaultedList<ItemStack> items = DefaultedList.ofSize(itemSize, ItemStack.EMPTY);
                UUID userId = cVanilla.getUuid("user");
                Inventories.readNbt(cVanilla, items);

                soulboundInventories.put(userId, items);
            }
            for (NbtElement e : modList) {
                if (!(e instanceof NbtCompound cMods)) continue;

                NbtCompound modsNbt = cMods.getCompound("Inventories");
                UUID userId = cMods.getUuid("user");

                for (YigdApi yigdApi : Yigd.apiMods) {
                    NbtCompound modNbt = modsNbt.getCompound(yigdApi.getModName());
                    Object modInventory = yigdApi.readNbt(modNbt);

                    addModdedSoulbound(userId, modInventory);
                }
            }
        }
    }
}
