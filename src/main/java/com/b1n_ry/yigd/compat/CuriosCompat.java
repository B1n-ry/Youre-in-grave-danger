package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.b1n_ry.yigd.core.GraveHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.component.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.*;

@SuppressWarnings("unchecked")
public class CuriosCompat implements YigdApi {
    @Override
    public String getModName() {
        return "curios";
    }

    @Override
    public Object getInventory(PlayerEntity player) {
        return this.getInventory(player, false);
    }

    @Override
    public Object getInventory(PlayerEntity player, boolean onDeath) {
        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosHelper().getCuriosHandler(player);

        Map<String, Map<String, DefaultedList<ItemStack>>> inventory = new HashMap<>();

        List<String> soulboundEnchantments = YigdConfig.getConfig().graveSettings.soulboundEnchantments;
        List<String> deleteEnchantments = YigdConfig.getConfig().graveSettings.deleteEnchantments;
        Map<String, Map<String, DefaultedList<ItemStack>>> soulbound = new HashMap<>();

        optional.ifPresent(handler -> handler.getCurios().forEach((s, stacksHandler) -> {
            Map<String, DefaultedList<ItemStack>> itemStacks = new HashMap<>();
            Map<String, DefaultedList<ItemStack>> soulboundStacks = new HashMap<>();

            DefaultedList<ItemStack> stacks = DefaultedList.of();
            DefaultedList<ItemStack> cosmeticStacks = DefaultedList.of();

            DefaultedList<ItemStack> soulboundNormal = DefaultedList.of();
            DefaultedList<ItemStack> soulboundCosmetic = DefaultedList.of();

            for (int i = 0; i < stacksHandler.getSlots(); i++) {
                ItemStack stack = stacksHandler.getStacks().getStack(i);

                if (onDeath) {
                    if (GraveHelper.hasEnchantments(soulboundEnchantments, stack) || isDefaultSoulbound(player, stack.getItem())) {
                        soulboundNormal.add(stack);
                    } else if (!GraveHelper.hasEnchantments(deleteEnchantments, stack)) {
                        stacks.add(stack);
                    }
                } else {
                    stacks.add(stack);
                }
            }
            for (int i = 0; i < stacksHandler.getSlots(); i++) {
                ItemStack stack = stacksHandler.getCosmeticStacks().getStack(i);

                if (onDeath) {
                    if (GraveHelper.hasEnchantments(soulboundEnchantments, stack) || isDefaultSoulbound(player, stack.getItem())) {
                        soulboundCosmetic.add(stack);
                    } else if (!GraveHelper.hasEnchantments(deleteEnchantments, stack)) {
                        cosmeticStacks.add(stack);
                    }
                } else {
                    cosmeticStacks.add(stack);
                }
            }

            itemStacks.put("normal", stacks);
            itemStacks.put("cosmetic", cosmeticStacks);

            soulboundStacks.put("normal", soulboundNormal);
            soulboundStacks.put("cosmetic", soulboundCosmetic);

            inventory.put(s, itemStacks);
            soulbound.put(s, soulboundStacks);
        }));
        if (onDeath) DeadPlayerData.Soulbound.addModdedSoulbound(player.getUuid(), soulbound);
        return inventory;
    }

    @Override
    public DefaultedList<ItemStack> setInventory(Object inventory, PlayerEntity player) {
        DefaultedList<ItemStack> extraItems = DefaultedList.of();
        if (!(inventory instanceof Map)) return extraItems;
        Map<String, Map<String, DefaultedList<ItemStack>>> modInv = (Map<String, Map<String, DefaultedList<ItemStack>>>) inventory;

        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosHelper().getCuriosHandler(player);
        optional.ifPresent(handler -> modInv.forEach((s, itemMap) -> {
            Optional<ICurioStacksHandler> stacksHandlerOptional = handler.getStacksHandler(s);
            stacksHandlerOptional.ifPresent(stacksHandler -> {
                DefaultedList<ItemStack> normalItems = itemMap.get("normal");
                for (int i = 0; i < normalItems.size(); i++) {
                    ItemStack stack = normalItems.get(i);
                    if (stack.isEmpty()) continue;

                    ItemStack present = stacksHandler.getStacks().getStack(i);
                    if (present.isEmpty()) {
                        stacksHandler.getStacks().setStack(i, stack);
                    } else {
                        extraItems.add(stack);
                    }
                }

                DefaultedList<ItemStack> cosmeticItems = itemMap.get("cosmetic");
                for (int i = 0; i < cosmeticItems.size(); i++) {
                    ItemStack stack = cosmeticItems.get(i);
                    if (stack.isEmpty()) continue;

                    ItemStack present = stacksHandler.getCosmeticStacks().getStack(i);
                    if (present.isEmpty()) {
                        stacksHandler.getCosmeticStacks().setStack(i, stack);
                    } else {
                        extraItems.add(stack);
                    }
                }
            });
        }));

        return extraItems;
    }

    @Override
    public int getInventorySize(Object inventory) {
        if (!(inventory instanceof Map)) return 0;
        List<ItemStack> actualItems = new ArrayList<>();
        Map<String, Map<String, DefaultedList<ItemStack>>> modInv = (Map<String, Map<String, DefaultedList<ItemStack>>>) inventory;

        modInv.forEach((slotName, inv) -> inv.forEach((s, itemStacks) -> {
            for (ItemStack stack : itemStacks) {
                if (stack != null && !stack.isEmpty()) actualItems.add(stack);
            }
        }));
        return actualItems.size();
    }

    @Override
    public void dropAll(PlayerEntity player) {
        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosHelper().getCuriosHandler(player);
        optional.ifPresent(handler -> handler.getCurios().forEach((s, stacksHandler) -> {
            for (int i = 0; i < stacksHandler.getSlots(); i++) {
                stacksHandler.getStacks().setStack(i, ItemStack.EMPTY);
            }
            for (int i = 0; i < stacksHandler.getSlots(); i++) {
                stacksHandler.getCosmeticStacks().setStack(i, ItemStack.EMPTY);
            }
        }));
    }

    @Override
    public List<ItemStack> toStackList(Object inventory) {
        List<ItemStack> stacks = new ArrayList<>();
        if (!(inventory instanceof Map)) return stacks;
        Map<String, Map<String, DefaultedList<ItemStack>>> modInv = (Map<String, Map<String, DefaultedList<ItemStack>>>) inventory;
        modInv.forEach((s, slot) -> slot.forEach((stack, itemStacks) -> stacks.addAll(itemStacks)));
        return stacks;
    }

    @Override
    public NbtCompound writeNbt(Object o) {
        NbtCompound nbt = new NbtCompound();
        if (!(o instanceof Map)) return nbt;
        Map<String, Map<String, DefaultedList<ItemStack>>> modInv = (Map<String, Map<String, DefaultedList<ItemStack>>>) o;
        modInv.forEach((slotName, slot) -> {
            NbtCompound slotNbt = new NbtCompound();
            slot.forEach((type, stacks) -> {
                NbtCompound stacksNbt = Inventories.writeNbt(new NbtCompound(), stacks);
                stacksNbt.putInt("size", stacks.size());

                slotNbt.put(type, stacksNbt);
            });

            nbt.put(slotName, slotNbt);
        });
        return nbt;
    }

    @Override
    public Object readNbt(NbtCompound nbt) {
        Map<String, Map<String, DefaultedList<ItemStack>>> inventory = new HashMap<>();
        Set<String> slotNames = nbt.getKeys();
        for (String slotName : slotNames) {
            Map<String, DefaultedList<ItemStack>> slotsMap = new HashMap<>();
            NbtCompound slotNbt = nbt.getCompound(slotName);
            for (String slotType : slotNbt.getKeys()) {
                NbtCompound itemsNbt = slotNbt.getCompound(slotType);
                int stacksSize = itemsNbt.getInt("size");
                DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(stacksSize, ItemStack.EMPTY);

                Inventories.readNbt(itemsNbt, itemStacks);

                slotsMap.put(slotType, itemStacks);
            }
            inventory.put(slotName, slotsMap);
        }
        return inventory;
    }

    private boolean isDefaultSoulbound(PlayerEntity player, Item item) {
        Identifier tagId = new Identifier("yigd", "soulbound_item");
        Tag<Item> tag = player.world.getTagManager().getItems().getTag(tagId);
        return tag != null && tag.contains(item);
    }
}
