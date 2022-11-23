package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.ScrollTypeConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.b1n_ry.yigd.core.DeathInfoManager;
import com.b1n_ry.yigd.core.GraveHelper;
import com.b1n_ry.yigd.core.ServerPlayerEntityImpl;
import com.b1n_ry.yigd.item.KeyItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements ServerPlayerEntityImpl {
    private Vec3d groundPos = null;

    @Shadow
    public abstract ServerWorld getWorld();

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        if (this.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return;

        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        Vec3d pos = player.getPos();
        ServerWorld playerWorld = player.getWorld();

        if (YigdConfig.getConfig().debugConfig.createGraveBeforeDeathMessage && !player.isSpectator()) {
            Yigd.NEXT_TICK.add(() -> GraveHelper.onDeath(player, playerWorld, pos, source));
        }
    }

    @Inject(method = "copyFrom", at = @At(value = "TAIL"))
    private void onRespawn(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        if (alive || oldPlayer.isSpectator()) return;

        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        UUID userId = player.getUuid();

        if (player.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return;

        YigdConfig yigdConfig = YigdConfig.getConfig();
        if (yigdConfig.debugConfig.clearInventoryOnRespawn) {
            // In case some items are by mistake placed in the inventory that should not be there
            player.getInventory().clear();

            for (YigdApi yigdApi : Yigd.apiMods) {
                yigdApi.dropAll(player);
            }
        }

        Map<String, Object> modSoulbounds = DeadPlayerData.Soulbound.getModdedSoulbound(userId);
        DefaultedList<ItemStack> soulboundItems = DeadPlayerData.Soulbound.getSoulboundInventory(userId);

        if (soulboundItems != null || modSoulbounds != null) {

            if (soulboundItems != null && soulboundItems.size() > 0) {
                PlayerInventory inventory = player.getInventory();

                int mainSize = inventory.main.size();
                int armorSize = inventory.armor.size();

                DefaultedList<ItemStack> curseBindingArmor = DefaultedList.of();

                for (int i = 0; i < soulboundItems.size(); i++) {
                    ItemStack stack = soulboundItems.get(i);
                    if (stack.isEmpty()) continue;

                    if (i >= mainSize && i < mainSize + armorSize) {
                        if (EnchantmentHelper.hasBindingCurse(stack)) {
                            curseBindingArmor.add(stack);
                            continue;
                        }
                    }

                    inventory.setStack(i, stack);
                }

                DefaultedList<ItemStack> overFlow = DefaultedList.of();
                for (ItemStack stack : curseBindingArmor) {
                    int i = inventory.getEmptySlot();
                    if (i < 0) {
                        overFlow.add(stack);
                    } else {
                        inventory.setStack(i, stack);
                    }
                }

                // In case player has an almost full inventory of soulbound items for some reason
                if (overFlow.size() > 0) {
                    ItemScatterer.spawn(player.world, player.getBlockPos(), curseBindingArmor);
                }
            }

            // Modded soulbound doesn't work without this because of cardinal components for some reason
            Yigd.NEXT_TICK.add(() -> {
                if (modSoulbounds != null && modSoulbounds.size() > 0) {
                    for (YigdApi yigdApi : Yigd.apiMods) {
                        String modName = yigdApi.getModName();

                        if (!yigdApi.applySoulbound() || !modSoulbounds.containsKey(modName)) continue;
                        Object modSoulbound = modSoulbounds.get(modName);

                        yigdApi.setInventory(modSoulbound, player);
                    }
                }
            });

            DeadPlayerData.Soulbound.dropModdedSoulbound(userId);
            DeadPlayerData.Soulbound.dropSoulbound(userId);
        }

        try {
            List<DeadPlayerData> deadPlayerData = DeathInfoManager.INSTANCE.data.get(userId);
            if (deadPlayerData != null) {
                List<DeadPlayerData> availableData = new ArrayList<>();
                for (DeadPlayerData data : deadPlayerData) {
                    if (data.availability != 1) continue;
                    availableData.add(data);
                }
                if (availableData.size() > 0) {
                    DeadPlayerData latestDeath = availableData.get(availableData.size() - 1);
                    BlockPos deathPos = latestDeath.gravePos;

                    giveScroll(player, latestDeath.id);

                    // Give grave key linked to the grave if it should give keys on respawn. Function will by itself fail if the item is disabled
                    if (yigdConfig.utilitySettings.graveKeySettings.retrieveOnRespawn) {
                        KeyItem.giveStackToPlayer(player, latestDeath.id);
                    }
                    if (yigdConfig.utilitySettings.graveCompassSettings.receiveOnDeath) {

                        ItemStack stack = Items.COMPASS.getDefaultStack();
                        NbtCompound nbt = new NbtCompound();
                        nbt.put("pos", NbtHelper.fromBlockPos(latestDeath.gravePos));

                        RegistryKey<World> worldKey = RegistryKey.of(Registry.WORLD_KEY, latestDeath.worldId);
                        World.CODEC.encodeStart(NbtOps.INSTANCE, worldKey).resultOrPartial(Yigd.LOGGER::error).ifPresent(nbtElement -> nbt.put("dimension", nbtElement));

                        stack.setSubNbt("pointTowards", nbt);
                        stack.setSubNbt("forGrave", NbtHelper.fromUuid(latestDeath.id));

                        stack.setCustomName(Text.translatable("item.yigd.grave_compass").styled(style -> style.withItalic(false)));
                        player.giveItemStack(stack);
                    }

                    if (deathPos != null && yigdConfig.graveSettings.tellDeathPos) {
                        player.sendMessage(Text.translatable("text.yigd.message.grave_location_info", deathPos.getX(), deathPos.getY(), deathPos.getZ(), latestDeath.dimensionName), false);
                    }
                }
            }
        }
        catch (Exception e) {
            Yigd.LOGGER.warn("Death data did not generate\n" + e);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void setGroundPos(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (player.isOnGround()) {
            this.groundPos = player.getPos();
        }
    }

    @Override
    public Vec3d getLastGroundPos() {
        return this.groundPos;
    }

    private void giveScroll(PlayerEntity player, UUID graveId) {
        YigdConfig.UtilitySettings utilityConfig = YigdConfig.getConfig().utilitySettings;
        if (utilityConfig.scrollItem.scrollType == ScrollTypeConfig.DISABLED || !utilityConfig.scrollItem.retrieveOnRespawn) return;
        ItemStack stack = Yigd.SCROLL_ITEM.getDefaultStack();

        NbtIntArray graveIdNbt = NbtHelper.fromUuid(graveId);
        NbtIntArray playerIdNbt = NbtHelper.fromUuid(player.getUuid());
        stack.setSubNbt("for_player", playerIdNbt);
        stack.setSubNbt("for_grave", graveIdNbt);

        player.giveItemStack(stack);
    }

    @Redirect(method = "createEndSpawnPlatform", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private boolean createEndSpawnPlatform(ServerWorld world, BlockPos blockPos, BlockState blockState) {
        BlockEntity be = world.getBlockEntity(blockPos);
        if (!(be instanceof GraveBlockEntity grave) || grave.getGraveOwner() == null) return world.setBlockState(blockPos, blockState);
        return false;
    }
}