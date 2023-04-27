package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.data.DeathInfoManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GraveComponent {
    private final GameProfile owner;
    private final InventoryComponent inventoryComponent;
    private final ExpComponent expComponent;
    private final ServerWorld serverWorld;
    private BlockPos pos;
    private final DamageSource damageSource;

    public GraveComponent(GameProfile owner, InventoryComponent inventoryComponent, ExpComponent expComponent, ServerWorld serverWorld, Vec3d pos, DamageSource damageSource) {
        this(owner, inventoryComponent, expComponent, serverWorld, BlockPos.ofFloored(pos), damageSource);
    }
    public GraveComponent(GameProfile owner, InventoryComponent inventoryComponent, ExpComponent expComponent, ServerWorld serverWorld, BlockPos pos, DamageSource damageSource) {
        this.owner = owner;
        this.inventoryComponent = inventoryComponent;
        this.expComponent = expComponent;
        this.serverWorld = serverWorld;
        this.pos = pos;
        this.damageSource = damageSource;
    }

    public InventoryComponent getInventoryComponent() {
        return this.inventoryComponent;
    }

    public ExpComponent getExpComponent() {
        return this.expComponent;
    }

    public ServerWorld getServerWorld() {
        return this.serverWorld;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public DamageSource getDamageSource() {
        return this.damageSource;
    }

    /**
     * Will filter through filters and stuff
     * @return where a grave can be placed based on config
     */
    public BlockPos findGravePos() {
        return null;
    }

    /**
     * Called to place down a grave block
     * @param newPos Where the grave should try to be placed
     * @param state Which block should be placed
     * @return Weather or not the grave was placed
     */
    public boolean tryPlaceGraveAt(BlockPos newPos, BlockState state) {
        this.pos = newPos;
        return this.serverWorld.setBlockState(newPos, state);
    }

    public void backUp(GameProfile profile) {
        DeathInfoManager.INSTANCE.addBackup(profile, this);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.put("owner", NbtHelper.writeGameProfile(new NbtCompound(), this.owner));
        nbt.put("inventory", this.inventoryComponent.toNbt());
        nbt.put("exp", this.expComponent.toNbt());

        nbt.put("world", this.getWorldRegistryKeyNbt(this.serverWorld));
        nbt.put("pos", NbtHelper.fromBlockPos(this.pos));

        return nbt;
    }

    public static GraveComponent fromNbt(NbtCompound nbt, MinecraftServer server) {
        GameProfile owner = NbtHelper.toGameProfile(nbt.getCompound("owner"));
        InventoryComponent inventoryComponent = InventoryComponent.fromNbt(nbt.getCompound("inventory"));
        ExpComponent expComponent = ExpComponent.fromNbt(nbt.getCompound("exp"));
        RegistryKey<World> worldKey = getRegistryKeyFromNbt(nbt.getCompound("world"));
        ServerWorld world = server.getWorld(worldKey);
        BlockPos pos = NbtHelper.toBlockPos(nbt.getCompound("pos"));



        return new GraveComponent(owner, inventoryComponent, expComponent, world, pos, null);
    }

    private NbtCompound getWorldRegistryKeyNbt(World world) {
        RegistryKey<World> key = world.getRegistryKey();
        NbtCompound nbt = new NbtCompound();
        nbt.putString("registry", key.getRegistry().toString());
        nbt.putString("value", key.getValue().toString());

        return nbt;
    }
    private static RegistryKey<World> getRegistryKeyFromNbt(NbtCompound nbt) {
        String registry = nbt.getString("registry");
        String value = nbt.getString("value");

        RegistryKey<Registry<World>> r = RegistryKey.ofRegistry(new Identifier(registry));
        return RegistryKey.of(r, new Identifier(value));
    }
}
