package com.b1n_ry.yigd.item;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.config.YigdConfig.ExtraFeatures.ScrollConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.networking.ServerPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DeathScrollItem extends Item {
    private static final int USE_TIME_MARGIN = 3;

    public DeathScrollItem(Properties settings) {
        super(settings);
    }


    @Override
    public void onCraftedBy(ItemStack stack, Level world, Player player) {
        if (!world.isClientSide) {
            this.bindStackToLatestDeath((ServerPlayer) player, stack);
        }
        super.onCraftedBy(stack, world, player);
    }

    @Override
    public boolean isEnabled(FeatureFlagSet enabledFeatures) {
        return YigdConfig.getConfig().extraFeatures.deathScroll.enabled;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return YigdConfig.getConfig().extraFeatures.deathScroll.useTime + USE_TIME_MARGIN;
    }
    @Override
    public @NotNull UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
    @Override
    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (remainingUseTicks < USE_TIME_MARGIN) {
            user.stopUsingItem();
        }
    }
    @Override
    public void releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
        float f = (float) (this.getUseDuration(stack, user) - remainingUseTicks) / (float) (this.getUseDuration(stack, user) - USE_TIME_MARGIN);
        if (f >= 1.0F && user instanceof Player player) {
            InteractionHand hand = player.getItemInHand(InteractionHand.MAIN_HAND).equals(stack) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            this.useAction(world, player, hand);
            return;
        }
        super.releaseUsing(stack, world, user, remainingUseTicks);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide) return super.use(world, user, hand);

        ScrollConfig scrollConfig = YigdConfig.getConfig().extraFeatures.deathScroll;

        ServerPlayer player = (ServerPlayer) user;
        ItemStack scroll = player.getItemInHand(hand);
        CustomData scrollNbtComponent = scroll.get(DataComponents.CUSTOM_DATA);
        CompoundTag scrollNbt = scrollNbtComponent != null ? scrollNbtComponent.copyTag() : null;
        // Rebind if the player is sneaking (and it can be rebound), or if the scroll is unbound
        if ((scrollConfig.rebindable && player.isShiftKeyDown()) || scrollNbt == null || !scrollNbt.contains("grave")) {
            if (this.bindStackToLatestDeath(player, scroll))
                return InteractionResultHolder.sidedSuccess(scroll, true);
        }

        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(scroll);
        if (YigdConfig.getConfig().extraFeatures.deathScroll.useTime > 0) {
            user.startUsingItem(hand);
        } else {
            return this.useAction(world, user, hand);
        }
        return InteractionResultHolder.consume(scroll);
    }
    private InteractionResultHolder<ItemStack> useAction(Level world, @NotNull Player user, @NotNull InteractionHand hand) {
        if (world.isClientSide) return super.use(world, user, hand);
        ScrollConfig scrollConfig = YigdConfig.getConfig().extraFeatures.deathScroll;
        ServerPlayer player = (ServerPlayer) user;
        ItemStack scroll = player.getItemInHand(hand);
        CustomData scrollNbtComponent = scroll.get(DataComponents.CUSTOM_DATA);
        CompoundTag scrollNbt = scrollNbtComponent != null ? scrollNbtComponent.copyTag() : null;

        ScrollConfig.ClickFunction clickFunction = scrollConfig.clickFunction;
        if (scrollNbt != null && scrollNbt.contains("clickFunction") && !scrollNbt.getString("clickFunction").equals("default")) {
            clickFunction = ScrollConfig.ClickFunction.valueOf(scrollNbt.getString("clickFunction"));
        }

        InteractionResultHolder<ItemStack> res = switch (clickFunction) {
            case VIEW_CONTENTS -> this.viewContent(scroll, player);
            case RESTORE_CONTENTS -> this.restoreContent(scroll, player);
            case TELEPORT_TO_LOCATION -> this.teleport(scroll, player);
        };
        if (res.getResult() != InteractionResult.PASS) {  // If the action was successful/failed or something other than 'standard'
            if (YigdConfig.getConfig().extraFeatures.deathScroll.consumeOnUse && res.getResult() != InteractionResult.CONSUME)
                scroll.shrink(1);
            return res;
        }

        player.getCooldowns().addCooldown(this, scrollConfig.useCooldown);
        return res;
    }

    public boolean bindStackToLatestDeath(ServerPlayer player, ItemStack scroll) {
        if (player == null) return false;  // Idk how some mods do auto-crafting, but this could fix some issues if they just pass null

        ResolvableProfile playerProfile = new ResolvableProfile(player.getGameProfile());
        List<GraveComponent> graves = new ArrayList<>(DeathInfoManager.INSTANCE.getBackupData(playerProfile));
        graves.removeIf(component -> component.getStatus() != GraveStatus.UNCLAIMED);

        int size = graves.size();
        if (size >= 1) {
            GraveComponent component = graves.get(size - 1);
            scroll.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, comp -> comp.update(nbtCompound -> {
                nbtCompound.putUUID("grave", component.getGraveId());
                nbtCompound.putString("clickFunction", "default");
            }));
            return true;
        }
        return false;
    }

    private InteractionResultHolder<ItemStack> viewContent(ItemStack scroll, ServerPlayer player) {
        CustomData scrollNbtComponent = scroll.get(DataComponents.CUSTOM_DATA);
        if (scrollNbtComponent == null) return InteractionResultHolder.pass(scroll);

        CompoundTag scrollNbt = scrollNbtComponent.copyTag();

        UUID graveId = scrollNbt.getUUID("grave");
        Optional<GraveComponent> optional = DeathInfoManager.INSTANCE.getGrave(graveId);
        if (optional.isPresent()) {
            GraveComponent component = optional.get();
            ServerPacketHandler.sendGraveOverviewPacket(player, component);
        }

        return InteractionResultHolder.success(scroll);
    }
    private InteractionResultHolder<ItemStack> restoreContent(ItemStack scroll, ServerPlayer player) {
        CustomData scrollNbtComponent = scroll.get(DataComponents.CUSTOM_DATA);
        if (scrollNbtComponent == null) return InteractionResultHolder.pass(scroll);

        CompoundTag scrollNbt = scrollNbtComponent.copyTag();

        UUID graveId = scrollNbt.getUUID("grave");
        Optional<GraveComponent> optional = DeathInfoManager.INSTANCE.getGrave(graveId);
        if (optional.isPresent()) {
            GraveComponent component = optional.get();
            InteractionResult res = component.claim(player, player.serverLevel(), null, component.getPos(), scroll);
            return new InteractionResultHolder<>(res, scroll);
        }
        return InteractionResultHolder.pass(scroll);
    }
    private InteractionResultHolder<ItemStack> teleport(ItemStack scroll, ServerPlayer player) {
        CustomData scrollNbtComponent = scroll.get(DataComponents.CUSTOM_DATA);
        if (scrollNbtComponent == null) return InteractionResultHolder.pass(scroll);

        CompoundTag scrollNbt = scrollNbtComponent.copyTag();

        UUID graveId = scrollNbt.getUUID("grave");
        Optional<GraveComponent> optional = DeathInfoManager.INSTANCE.getGrave(graveId);
        if (optional.isPresent()) {
            GraveComponent component = optional.get();
            BlockPos gravePos = component.getPos();
            player.teleportTo(component.getWorld(), gravePos.getX(), gravePos.getY(), gravePos.getZ(), player.getYRot(), player.getXRot());
        }

        return InteractionResultHolder.success(scroll);
    }
}
