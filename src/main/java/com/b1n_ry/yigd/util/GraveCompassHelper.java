package com.b1n_ry.yigd.util;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public class GraveCompassHelper {
    public static void giveCompass(ServerPlayerEntity player, UUID graveId, BlockPos gravePos, RegistryKey<World> worldKey) {
        ItemStack compass = Items.COMPASS.getDefaultStack();
        NbtCompound compassNbt = new NbtCompound();

        compassNbt.putUuid("linked_grave", graveId);  // Speed up the process of identifying the grave server side

        // Make clients read the grave position
        compassNbt.put("grave_pos", NbtHelper.fromBlockPos(gravePos));
        World.CODEC.encodeStart(NbtOps.INSTANCE, worldKey).resultOrPartial(Yigd.LOGGER::error).ifPresent(worldNbt -> compassNbt.put("grave_dimension", worldNbt));

        compass.setNbt(compassNbt);
        compass.setCustomName(Text.translatable("item.yigd.grave_compass").styled(style -> style.withItalic(false)));
        player.giveItemStack(compass);
    }
}
