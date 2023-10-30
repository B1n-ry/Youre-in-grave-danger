package com.b1n_ry.yigd.item;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class GraveKeyItem extends Item {
    public GraveKeyItem(Settings settings) {
        super(settings);
    }
    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player) {
        if (!world.isClient) {
            this.bindStackToLatestGrave(player, stack);
        }
        super.onCraft(stack, world, player);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) return super.use(world, user, hand);

        YigdConfig config = YigdConfig.getConfig();
        // TODO: Also check if key is rebindable
        if (config.extraFeatures.graveKeys.rebindable && user.isSneaking()) {
            ItemStack key = user.getStackInHand(hand);
            if (this.bindStackToLatestGrave(user, key))
                return TypedActionResult.success(key, true);
        }

        return super.use(world, user, hand);
    }

    public boolean bindStackToLatestGrave(PlayerEntity player, ItemStack key) {
        GameProfile playerProfile = player.getGameProfile();
        List<GraveComponent> graves = new ArrayList<>(DeathInfoManager.INSTANCE.getBackupData(playerProfile));
        graves.removeIf(component -> component.getStatus() != GraveStatus.UNCLAIMED);

        int size = graves.size();
        if (size >= 1) {
            GraveComponent component = graves.get(size - 1);
            key.setSubNbt("grave", NbtHelper.fromUuid(component.getGraveId()));
            key.setSubNbt("user", NbtHelper.writeGameProfile(new NbtCompound(), playerProfile));
            return true;
        }
        return false;
    }
}
