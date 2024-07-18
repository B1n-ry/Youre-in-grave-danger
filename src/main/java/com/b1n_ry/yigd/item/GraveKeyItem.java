package com.b1n_ry.yigd.item;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GraveKeyItem extends Item {
    public GraveKeyItem(Settings settings) {
        super(settings);
    }
    @Override
    public void onCraftByPlayer(ItemStack stack, World world, PlayerEntity player) {
        if (!world.isClient) {
            this.bindStackToLatestGrave(player, stack);
        }
        super.onCraftByPlayer(stack, world, player);
    }

    @Override
    public boolean isEnabled(FeatureSet enabledFeatures) {
        return YigdConfig.getConfig().extraFeatures.graveKeys.enabled;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) return super.use(world, user, hand);

        YigdConfig config = YigdConfig.getConfig();

        if (config.extraFeatures.graveKeys.rebindable && user.isSneaking()) {
            ItemStack key = user.getStackInHand(hand);
            if (this.bindStackToLatestGrave(user, key))
                return TypedActionResult.success(key, true);
        }

        return super.use(world, user, hand);
    }

    public boolean bindStackToLatestGrave(PlayerEntity player, ItemStack key) {
        ProfileComponent playerProfile = new ProfileComponent(player.getGameProfile());
        List<GraveComponent> graves = new ArrayList<>(DeathInfoManager.INSTANCE.getBackupData(playerProfile));
        graves.removeIf(component -> component.getStatus() != GraveStatus.UNCLAIMED);

        int size = graves.size();
        if (size >= 1) {
            GraveComponent component = graves.get(size - 1);
            this.bindStackToGrave(component.getGraveId(), playerProfile, key);
            return true;
        }
        return false;
    }
    public void bindStackToGrave(UUID graveId, ProfileComponent playerProfile, ItemStack key) {
        key.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, comp -> comp.apply(nbtCompound -> {
            nbtCompound.put("grave", NbtHelper.fromUuid(graveId));
            nbtCompound.put("user", ProfileComponent.CODEC.encodeStart(NbtOps.INSTANCE, playerProfile).getOrThrow());
        }));
    }
}
