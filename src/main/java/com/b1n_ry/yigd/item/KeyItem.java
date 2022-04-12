package com.b1n_ry.yigd.item;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.b1n_ry.yigd.core.DeathInfoManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public class KeyItem extends Item {
    public KeyItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player) {
        linkToPlayer(player.getUuid(), stack);
        super.onCraft(stack, world, player);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) return super.use(world, user, hand);
        YigdConfig.GraveKeySettings keySettings = YigdConfig.getConfig().utilitySettings.graveKeySettings;
        if (keySettings.rebindable && user.isSneaking()) {
            ItemStack stack = user.getStackInHand(hand);
            boolean setNbt = linkToPlayer(user.getUuid(), stack);

            if (setNbt) {
                return TypedActionResult.success(stack);
            }
        }
        return super.use(world, user, hand);
    }

    public static boolean linkToPlayer(UUID playerId, ItemStack stack) {
        List<DeadPlayerData> data = DeathInfoManager.INSTANCE.data.get(playerId);
        if (data == null || data.size() <= 0) return false;

        DeadPlayerData graveData;
        int i = data.size();
        do {
            graveData = data.get(--i);
        } while (graveData != null && graveData.availability != 1);

        if (graveData == null) return false;

        return linkToPlayer(playerId, stack, graveData.id);
    }
    public static boolean linkToPlayer(UUID playerId, ItemStack stack, UUID graveId) {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("user", playerId);
        nbt.putUuid("grave", graveId);

        NbtCompound stackNbt = stack.getNbt();
        if (stackNbt != null) {
            stackNbt.copyFrom(nbt);
        } else {
            stack.setNbt(nbt);
        }
        return true;
    }

    public static boolean isKeyForGrave(ItemStack keyStack, GraveBlockEntity grave) {
        boolean matching;
        YigdConfig.GraveKeySettings graveKeySettings = YigdConfig.getConfig().utilitySettings.graveKeySettings;
        if (graveKeySettings.enableKeys && keyStack.getItem().equals(Yigd.KEY_ITEM)) {
            NbtCompound keyNbt = keyStack.getNbt();
            switch (graveKeySettings.graveKeySpecification) {
                case ALL -> matching = true;
                case PLAYER -> matching = (keyNbt != null && grave.getGraveOwner().getId().equals(keyNbt.getUuid("user")));
                case GRAVE -> matching = (keyNbt != null && grave.getGraveId().equals(keyNbt.getUuid("grave")));
                default -> matching = false;
            }
            if (matching) keyStack.decrement(1);
            return matching;
        }

        return false;
    }

    public static void giveStackToPlayer(PlayerEntity player, UUID graveId) {
        giveStackToPlayer(player, player.getUuid(), graveId);
    }
    public static void giveStackToPlayer(PlayerEntity player, UUID playerId, UUID graveId) {
        if (!YigdConfig.getConfig().utilitySettings.graveKeySettings.enableKeys) return;
        ItemStack stack = Yigd.KEY_ITEM.getDefaultStack();

        boolean linked = linkToPlayer(playerId, stack, graveId);
        if (linked) {
            player.giveItemStack(stack);
        }
    }
}
