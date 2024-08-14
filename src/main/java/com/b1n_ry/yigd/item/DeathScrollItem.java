package com.b1n_ry.yigd.item;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.config.YigdConfig.ExtraFeatures.ScrollConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.networking.packets.GraveOverviewS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DeathScrollItem extends Item {
    public DeathScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player) {
        if (!level.isClientSide) {
            this.bindStackToLatestDeath((ServerPlayer) player, stack);
        }
        super.onCraftedBy(stack, level, player);
    }

    @Override
    public boolean isEnabled(@NotNull FeatureFlagSet enabledFeatures) {
        return YigdConfig.getConfig().extraFeatures.deathScroll.enabled;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level world, @NotNull Player user, @NotNull InteractionHand hand) {
        if (world.isClientSide) return super.use(world, user, hand);

        ScrollConfig scrollConfig = YigdConfig.getConfig().extraFeatures.deathScroll;

        ServerPlayer player = (ServerPlayer) user;
        ItemStack scroll = player.getItemInHand(hand);
        CustomData scrollNbtComponent = scroll.get(DataComponents.CUSTOM_DATA);
        CompoundTag scrollNbt = scrollNbtComponent != null ? scrollNbtComponent.copyTag() : null;
        // Rebind if the player is sneaking (and it can be rebound), or if the scroll is unbound
        if ((scrollConfig.rebindable && player.isShiftKeyDown()) || scrollNbt == null || scrollNbt.getUUID("grave") == null) {
            if (this.bindStackToLatestDeath(player, scroll))
                return InteractionResultHolder.sidedSuccess(scroll, true);
        }

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

        return super.use(world, user, hand);
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
            YigdConfig config = YigdConfig.getConfig();
            YigdConfig.CommandConfig commandConfig = config.commandConfig;
            PacketDistributor.sendToPlayer(player, new GraveOverviewS2CPacket(component,
                    player.hasPermissions(commandConfig.restorePermissionLevel),
                    player.hasPermissions(commandConfig.robPermissionLevel),
                    player.hasPermissions(commandConfig.deletePermissionLevel),
                    player.hasPermissions(commandConfig.unlockPermissionLevel) && config.graveConfig.unlockable,
                    config.extraFeatures.graveKeys.enabled && config.extraFeatures.graveKeys.obtainableFromGui,
                    config.extraFeatures.graveCompass.cloneRecoveryCompassWithGUI && player.getInventory().countItem(Items.RECOVERY_COMPASS) > 0));
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
            if (component.getWorld() != null)
                player.teleportTo(component.getWorld(), gravePos.getX(), gravePos.getY(), gravePos.getZ(), player.getYRot(), player.getXRot());
        }

        return InteractionResultHolder.success(scroll);
    }
}
