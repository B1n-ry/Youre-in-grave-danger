package com.b1n_ry.yigd.item;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.ScrollTypeConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.b1n_ry.yigd.core.DeathInfoManager;
import com.b1n_ry.yigd.core.PacketIdentifiers;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public class ScrollItem extends Item {
    public ScrollItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = super.getDefaultStack();
        ScrollTypeConfig scrollTypeConfig = YigdConfig.getConfig().utilitySettings.scrollItem.scrollType;
        if (scrollTypeConfig == ScrollTypeConfig.SCROLL_OF_RETURN) stack.setCustomName(MutableText.of(new TranslatableTextContent("item.yigd.tp_scroll")));
        return stack;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) return super.use(world, user, hand);

        ScrollTypeConfig scrollType = YigdConfig.getConfig().utilitySettings.scrollItem.scrollType;
        if (scrollType == ScrollTypeConfig.SCROLL_OF_RETURN) teleport(world, user, hand);
        if (scrollType == ScrollTypeConfig.INFO_SCROLL) showInfo(user, hand);

        return super.use(world, user, hand);
    }

    private void showInfo(PlayerEntity user, Hand hand) {
        UUID userId = user.getUuid();
        if (user instanceof ServerPlayerEntity spe && DeathInfoManager.INSTANCE.data.containsKey(userId) && DeathInfoManager.INSTANCE.data.get(userId).size() > 0) {
            List<DeadPlayerData> deadPlayerData = DeathInfoManager.INSTANCE.data.get(userId);

            ItemStack scroll;
            if (hand == Hand.MAIN_HAND) {
                scroll = user.getMainHandStack();
            } else {
                scroll = user.getOffHandStack();
            }
            if (scroll.getItem() != Yigd.SCROLL_ITEM) {
                user.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.scroll_error", scroll.getItem().getName().getString())), true);
                return;
            }

            DeadPlayerData selectedGrave = null;
            NbtElement refNbt = scroll.getSubNbt("ref");
            if (refNbt != null) {
                UUID graveId = NbtHelper.toUuid(refNbt);
                for (DeadPlayerData data : deadPlayerData) {
                    if (data.id.equals(graveId)) {
                        selectedGrave = data;
                        break;
                    }
                }
            } else {
                if (deadPlayerData.size() <= 0) {
                    user.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.you_have_no_graves")), true);
                    return;
                }
                selectedGrave = deadPlayerData.get(deadPlayerData.size() - 1);
            }
            if (selectedGrave == null) {
                user.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.grave_now_gone")), true);
                return;
            }

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeNbt(selectedGrave.toNbt());

            YigdConfig config = YigdConfig.getConfig();
            YigdConfig.GraveKeySettings keySettings = config.utilitySettings.graveKeySettings;
            buf.writeBoolean(keySettings.enableKeys && keySettings.getFromGui);
            buf.writeBoolean(config.graveSettings.unlockableGraves);

            int unlockedGravesAmount = DeathInfoManager.INSTANCE.unlockedGraves.size();
            buf.writeInt(unlockedGravesAmount);
            for (int i = 0; i < unlockedGravesAmount; i++) {
                buf.writeUuid(DeathInfoManager.INSTANCE.unlockedGraves.get(i));
            }

            ServerPlayNetworking.send(spe, PacketIdentifiers.SINGLE_GRAVE_GUI, buf);
            Yigd.LOGGER.info("Sending packet to " + spe.getDisplayName().getString() + " with grave info");
        } else {
            user.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.you_have_no_graves")), true);
        }
    }

    private void teleport(World world, PlayerEntity user, Hand hand) {
        ItemStack scroll;
        if (hand == Hand.MAIN_HAND) {
            scroll = user.getMainHandStack();
        } else {
            scroll = user.getOffHandStack();
        }
        if (scroll.getItem() != Yigd.SCROLL_ITEM) {
            user.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.scroll_error", scroll.getItem().getName().getString())), true);
            return;
        }

        UUID userId = user.getUuid();
        if (!DeathInfoManager.INSTANCE.data.containsKey(userId)) {
            user.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.you_have_no_graves")), true);
            return;
        }

        List<DeadPlayerData> graves = DeathInfoManager.INSTANCE.data.get(userId);
        graves.removeIf(grave -> grave.availability != 1);

        if (graves.size() <= 0) {
            user.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.you_have_no_graves")), true);
            return;
        }

        DeadPlayerData selectedGrave = null;
        NbtElement refNbt = scroll.getSubNbt("ref");
        if (refNbt != null) {
            UUID graveId = NbtHelper.toUuid(refNbt);
            for (DeadPlayerData data : graves) {
                if (data.id.equals(graveId)) {
                    selectedGrave = data;
                    break;
                }
            }
        } else {
            selectedGrave = graves.get(graves.size() - 1);
        }
        if (selectedGrave == null) {
            user.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.grave_now_gone")), true);
            return;
        }

        DeadPlayerData grave = graves.get(graves.size() - 1);

        if (grave.gravePos == null || grave.worldId != world.getRegistryKey().getValue()) {
            if (grave.gravePos == null) {
                user.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.missing_grave_location")), true);
            } else {
                user.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.cross_dim_error")), true);
            }
            return;
        }
        user.teleport(grave.gravePos.getX() + 0.5, grave.gravePos.getY() + 0.5, grave.gravePos.getZ() + 0.5);
        scroll.decrement(1);
    }
}
