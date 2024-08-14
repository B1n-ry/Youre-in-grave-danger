package com.b1n_ry.yigd.item;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GraveKeyItem extends Item {
    public GraveKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, Level world, @NotNull Player player) {
        if (!world.isClientSide) {
            this.bindStackToLatestGrave(player, stack);
        }
        super.onCraftedBy(stack, world, player);
    }

    @Override
    public boolean isEnabled(@NotNull FeatureFlagSet enabledFeatures) {
        return YigdConfig.getConfig().extraFeatures.graveKeys.enabled;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player user, @NotNull InteractionHand hand) {
        if (level.isClientSide) return super.use(level, user, hand);

        YigdConfig config = YigdConfig.getConfig();

        if (config.extraFeatures.graveKeys.rebindable && user.isShiftKeyDown()) {
            ItemStack key = user.getItemInHand(hand);
            if (this.bindStackToLatestGrave(user, key))
                return InteractionResultHolder.sidedSuccess(key, true);
        }

        return super.use(level, user, hand);
    }

    public boolean bindStackToLatestGrave(Player player, ItemStack key) {
        ResolvableProfile playerProfile = new ResolvableProfile(player.getGameProfile());
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
    public void bindStackToGrave(UUID graveId, ResolvableProfile playerProfile, ItemStack key) {
        key.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, comp -> comp.update(nbtCompound -> {
            nbtCompound.put("grave", NbtUtils.createUUID(graveId));
            nbtCompound.put("user", ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, playerProfile).getOrThrow());
        }));
    }
}
