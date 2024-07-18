package com.b1n_ry.yigd.item;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.config.YigdConfig.ExtraFeatures.ScrollConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.networking.ServerPacketHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DeathScrollItem extends Item {
    public DeathScrollItem(Settings settings) {
        super(settings);
    }

    @Override
    public void onCraftByPlayer(ItemStack stack, World world, PlayerEntity player) {
        if (!world.isClient) {
            this.bindStackToLatestDeath((ServerPlayerEntity) player, stack);
        }
        super.onCraftByPlayer(stack, world, player);
    }

    @Override
    public boolean isEnabled(FeatureSet enabledFeatures) {
        return YigdConfig.getConfig().extraFeatures.deathScroll.enabled;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) return super.use(world, user, hand);

        ScrollConfig scrollConfig = YigdConfig.getConfig().extraFeatures.deathScroll;

        ServerPlayerEntity player = (ServerPlayerEntity) user;
        ItemStack scroll = player.getStackInHand(hand);
        NbtComponent scrollNbtComponent = scroll.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound scrollNbt = scrollNbtComponent != null ? scrollNbtComponent.copyNbt() : null;
        // Rebind if the player is sneaking (and it can be rebound), or if the scroll is unbound
        if ((scrollConfig.rebindable && player.isSneaking()) || scrollNbt == null || scrollNbt.getUuid("grave") == null) {
            if (this.bindStackToLatestDeath(player, scroll))
                return TypedActionResult.success(scroll, true);
        }

        ScrollConfig.ClickFunction clickFunction = scrollConfig.clickFunction;
        if (scrollNbt != null && scrollNbt.contains("clickFunction") && !scrollNbt.getString("clickFunction").equals("default")) {
            clickFunction = ScrollConfig.ClickFunction.valueOf(scrollNbt.getString("clickFunction"));
        }

        TypedActionResult<ItemStack> res = switch (clickFunction) {
            case VIEW_CONTENTS -> this.viewContent(scroll, player);
            case RESTORE_CONTENTS -> this.restoreContent(scroll, player);
            case TELEPORT_TO_LOCATION -> this.teleport(scroll, player);
        };
        if (res.getResult() != ActionResult.PASS) {  // If the action was successful/failed or something other than 'standard'
            if (YigdConfig.getConfig().extraFeatures.deathScroll.consumeOnUse && res.getResult() != ActionResult.CONSUME)
                scroll.decrement(1);
            return res;
        }

        return super.use(world, user, hand);
    }

    public boolean bindStackToLatestDeath(ServerPlayerEntity player, ItemStack scroll) {
        if (player == null) return false;  // Idk how some mods do auto-crafting, but this could fix some issues if they just pass null

        ProfileComponent playerProfile = new ProfileComponent(player.getGameProfile());
        List<GraveComponent> graves = new ArrayList<>(DeathInfoManager.INSTANCE.getBackupData(playerProfile));
        graves.removeIf(component -> component.getStatus() != GraveStatus.UNCLAIMED);

        int size = graves.size();
        if (size >= 1) {
            GraveComponent component = graves.get(size - 1);
            scroll.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, comp -> comp.apply(nbtCompound -> {
                nbtCompound.putUuid("grave", component.getGraveId());
                nbtCompound.putString("clickFunction", "default");
            }));
            return true;
        }
        return false;
    }

    private TypedActionResult<ItemStack> viewContent(ItemStack scroll, ServerPlayerEntity player) {
        NbtComponent scrollNbtComponent = scroll.get(DataComponentTypes.CUSTOM_DATA);
        if (scrollNbtComponent == null) return TypedActionResult.pass(scroll);

        NbtCompound scrollNbt = scrollNbtComponent.copyNbt();

        UUID graveId = scrollNbt.getUuid("grave");
        Optional<GraveComponent> optional = DeathInfoManager.INSTANCE.getGrave(graveId);
        if (optional.isPresent()) {
            GraveComponent component = optional.get();
            ServerPacketHandler.sendGraveOverviewPacket(player, component);
        }

        return TypedActionResult.success(scroll);
    }
    private TypedActionResult<ItemStack> restoreContent(ItemStack scroll, ServerPlayerEntity player) {
        NbtComponent scrollNbtComponent = scroll.get(DataComponentTypes.CUSTOM_DATA);
        if (scrollNbtComponent == null) return TypedActionResult.pass(scroll);

        NbtCompound scrollNbt = scrollNbtComponent.copyNbt();

        UUID graveId = scrollNbt.getUuid("grave");
        Optional<GraveComponent> optional = DeathInfoManager.INSTANCE.getGrave(graveId);
        if (optional.isPresent()) {
            GraveComponent component = optional.get();
            ActionResult res = component.claim(player, player.getServerWorld(), null, component.getPos(), scroll);
            return new TypedActionResult<>(res, scroll);
        }
        return TypedActionResult.pass(scroll);
    }
    private TypedActionResult<ItemStack> teleport(ItemStack scroll, ServerPlayerEntity player) {
        NbtComponent scrollNbtComponent = scroll.get(DataComponentTypes.CUSTOM_DATA);
        if (scrollNbtComponent == null) return TypedActionResult.pass(scroll);

        NbtCompound scrollNbt = scrollNbtComponent.copyNbt();

        UUID graveId = scrollNbt.getUuid("grave");
        Optional<GraveComponent> optional = DeathInfoManager.INSTANCE.getGrave(graveId);
        if (optional.isPresent()) {
            GraveComponent component = optional.get();
            BlockPos gravePos = component.getPos();
            player.teleport(component.getWorld(), gravePos.getX(), gravePos.getY(), gravePos.getZ(), player.getYaw(), player.getPitch());
        }

        return TypedActionResult.success(scroll);
    }
}
