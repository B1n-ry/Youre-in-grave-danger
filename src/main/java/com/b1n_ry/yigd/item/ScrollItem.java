package com.b1n_ry.yigd.item;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.b1n_ry.yigd.core.DeathInfoManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public class ScrollItem extends Item {
    public ScrollItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) return super.use(world, user, hand);

        ItemStack scroll;
        if (hand == Hand.MAIN_HAND) {
            scroll = user.getMainHandStack();
        } else {
            scroll = user.getOffHandStack();
        }
        if (scroll.getItem() != Yigd.SCROLL_ITEM) {
            user.sendMessage(new TranslatableText("text.yigd.message.scroll_error" + scroll.getItem().getName().asString()), true);
            return super.use(world, user, hand);
        }

        UUID userId = user.getUuid();
        if (!DeathInfoManager.INSTANCE.data.containsKey(userId)) {
            user.sendMessage(new TranslatableText("text.yigd.message.you_have_no_graves"), true);
            return super.use(world, user, hand);
        }

        List<DeadPlayerData> graves = DeathInfoManager.INSTANCE.data.get(userId);

        if (graves.size() <= 0) {
            user.sendMessage(new TranslatableText("text.yigd.message.you_have_no_graves"), true);
            return super.use(world, user, hand);
        }

        DeadPlayerData grave = graves.get(graves.size() - 1);

        if (grave.gravePos == null || grave.worldId != world.getRegistryKey().getValue()) {
            if (grave.gravePos == null) {
                user.sendMessage(new TranslatableText("text.yigd.message.missing_grave_location"), true);
            } else {
                user.sendMessage(new TranslatableText("text.yigd.message.cross_dim_error"), true);
            }
            return super.use(world, user, hand);
        }
        user.teleport(grave.gravePos.getX() + 0.5, grave.gravePos.getY() + 0.5, grave.gravePos.getZ() + 0.5);
        scroll.decrement(1);

        return super.use(world, user, hand);
    }
}
