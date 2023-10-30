package com.b1n_ry.yigd.item;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.packets.ServerPacketHandler;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
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
    public void onCraft(ItemStack stack, World world, PlayerEntity player) {
        if (!world.isClient) {
            this.bindStackToLatestDeath((ServerPlayerEntity) player, stack);
        }
        super.onCraft(stack, world, player);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) return super.use(world, user, hand);

        YigdConfig.ExtraFeatures.ScrollConfig scrollConfig = YigdConfig.getConfig().extraFeatures.deathScroll;

        ServerPlayerEntity player = (ServerPlayerEntity) user;
        ItemStack scroll = player.getStackInHand(hand);
        if (scrollConfig.rebindable && player.isSneaking()) {
            if (this.bindStackToLatestDeath(player, scroll))
                return TypedActionResult.success(scroll, true);
        }

        TypedActionResult<ItemStack> res = switch (scrollConfig.clickFunction) {
            case VIEW_CONTENTS -> this.viewContent(scroll, player);
            case RESTORE_CONTENTS -> this.restoreContent(scroll, player);
            case TELEPORT_TO_LOCATION -> this.teleport(scroll, player);
        };
        if (res.getResult() == ActionResult.PASS)
            return res;

        return super.use(world, user, hand);
    }

    public boolean bindStackToLatestDeath(ServerPlayerEntity player, ItemStack scroll) {
        GameProfile playerProfile = player.getGameProfile();
        List<GraveComponent> graves = new ArrayList<>(DeathInfoManager.INSTANCE.getBackupData(playerProfile));
        graves.removeIf(component -> component.getStatus() != GraveStatus.UNCLAIMED);

        int size = graves.size();
        if (size >= 1) {
            GraveComponent component = graves.get(size - 1);
            scroll.setSubNbt("grave", NbtHelper.fromUuid(component.getGraveId()));
            return true;
        }
        return false;
    }

    private TypedActionResult<ItemStack> viewContent(ItemStack scroll, ServerPlayerEntity player) {
        NbtCompound scrollNbt = scroll.getNbt();
        if (scrollNbt == null) return TypedActionResult.pass(scroll);

        UUID graveId = scrollNbt.getUuid("grave");
        Optional<GraveComponent> optional = DeathInfoManager.INSTANCE.getGrave(graveId);
        if (optional.isPresent()) {
            GraveComponent component = optional.get();
            ServerPacketHandler.sendGraveOverviewPacket(player, component);
        }

        return TypedActionResult.success(scroll);
    }
    private TypedActionResult<ItemStack> restoreContent(ItemStack scroll, ServerPlayerEntity player) {
        NbtCompound scrollNbt = scroll.getNbt();
        if (scrollNbt == null) return TypedActionResult.pass(scroll);

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
        NbtCompound scrollNbt = scroll.getNbt();
        if (scrollNbt == null) return TypedActionResult.pass(scroll);

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
