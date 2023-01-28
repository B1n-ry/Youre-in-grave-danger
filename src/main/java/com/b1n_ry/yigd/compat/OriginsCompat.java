package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.DeathEffectConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.b1n_ry.yigd.core.GraveHelper;
import com.b1n_ry.yigd.core.ModTags;
import com.b1n_ry.yigd.mixin.OriginKeepInventoryPowerAccessor;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.Active.Key;
import io.github.apace100.apoli.power.InventoryPower;
import io.github.apace100.apoli.power.KeepInventoryPower;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class OriginsCompat implements YigdApi {
    @Override
    public String getModName() {
        return "apoli";
    }

    @Override
    public Object getInventory(PlayerEntity player, boolean onDeath, @Nullable DeathEffectConfig onDeathHandling) {
        YigdConfig config = YigdConfig.getConfig();

        Map<String, DefaultedList<ItemStack>> powerLists = new HashMap<>(); // Return value
        Map<String, DefaultedList<ItemStack>> soulboundLists = new HashMap<>(); // Soulbound items
        PowerHolderComponent.getPowers(player, InventoryPower.class).forEach(inventoryPower -> {
            Key key = inventoryPower.getKey(); // Key of the inventory power (definable)
            DefaultedList<ItemStack> soulboundInv = DefaultedList.ofSize(inventoryPower.size(), ItemStack.EMPTY);
            DefaultedList<ItemStack> fullInv = DefaultedList.ofSize(inventoryPower.size(), ItemStack.EMPTY);

            for (int i = 0; i < inventoryPower.size(); i++) {
                // Save gear if it shouldn't be dropped, else put in grave
                ItemStack currentStack = inventoryPower.getStack(i);
                if (onDeath) {
                    if (!inventoryPower.shouldDropOnDeath(currentStack) || currentStack.isIn(ModTags.SOULBOUND_ITEM) || onDeathHandling == DeathEffectConfig.KEEP_ITEMS || GraveHelper.hasBotaniaKeepIvy(currentStack, true)) {
                        soulboundInv.set(i, currentStack); // Add to soulbound if it should stay in inventory
                    } else if (GraveHelper.hasEnchantments(config.graveSettings.soulboundEnchantments, currentStack)) { // Soulbound enchantment and not default keep
                        if (config.graveSettings.loseSoulboundLevelOnDeath) {
                            GraveHelper.removeEnchantmentLevel(currentStack, config.graveSettings.soulboundEnchantments);
                        }
                        soulboundInv.set(i, currentStack);
                    } else if (!GraveHelper.hasEnchantments(config.graveSettings.deleteEnchantments, currentStack)) { // Not destroyed
                        fullInv.set(i, currentStack);
                    }
                } else {
                    fullInv.set(i, currentStack);
                }
            }

            soulboundLists.put(key.key, soulboundInv);
            powerLists.put(key.key, fullInv);
        });

        if (onDeath) DeadPlayerData.Soulbound.addModdedSoulbound(player.getUuid(), soulboundLists, this.getModName());
        return powerLists;
    }

    @Override
    public DefaultedList<ItemStack> setInventory(Object inventory, PlayerEntity player) {
        DefaultedList<ItemStack> extraItems = DefaultedList.of();
        if (!(inventory instanceof Map<?,?> allItems) || allItems.isEmpty()) return extraItems;

        PowerHolderComponent.getPowers(player, InventoryPower.class).forEach(inventoryPower -> {
            if (!(allItems.get(inventoryPower.getKey().key) instanceof DefaultedList<?> items)) return;
            for (int i = 0; i < items.size(); i++) {
                if (!(items.get(i) instanceof ItemStack item)) continue;

                if (i >= inventoryPower.size() || !inventoryPower.getStack(i).isEmpty()) {
                    extraItems.add(item);
                    continue;
                }
                inventoryPower.setStack(i, item);
            }
        });

        return extraItems;
    }

    @Override
    public int getInventorySize(Object inventory) {
        if (!(inventory instanceof Map<?,?> m) || m.isEmpty()) return 0;

        int totalItems = 0;
        for (Map.Entry<?, ?> entry : m.entrySet()) {
            if (!(entry.getValue() instanceof DefaultedList<?> items)) continue;
            for (Object o : items) {
                if (!(o instanceof ItemStack item)) continue;
                if (!item.isEmpty()) totalItems++;
            }
        }
        return totalItems;
    }

    @Override
    public void dropAll(PlayerEntity player) {
        PowerHolderComponent.getPowers(player, InventoryPower.class).forEach(InventoryPower::clear);
    }

    @Override
    public List<ItemStack> toStackList(Object inventory) {
        List<ItemStack> stacks = new ArrayList<>();
        if (!(inventory instanceof Map<?,?> m) || m.isEmpty()) return stacks;

        for (Map.Entry<?, ?> entry : m.entrySet()) {
            if (!(entry.getValue() instanceof DefaultedList<?> items)) continue;
            for (Object item : items) {
                if (!(item instanceof ItemStack stack)) continue;
                if (!stack.isEmpty()) stacks.add(stack);
            }
        }

        return stacks;
    }

    @SuppressWarnings("unchecked")
    @Override
    public NbtCompound writeNbt(Object o) {
        NbtCompound nbt = new NbtCompound();
        if (!(o instanceof Map<?,?> m) || m.isEmpty()) return nbt;

        for (Map.Entry<?, ?> entry : m.entrySet()) {
            if (!(entry.getValue() instanceof DefaultedList<?> items) || !(entry.getKey() instanceof Key key)) continue;
            NbtCompound inventoryNbt = Inventories.writeNbt(new NbtCompound(), (DefaultedList<ItemStack>) items);
            inventoryNbt.putInt("size", items.size());

            nbt.put(key.key, inventoryNbt);
        }
        return nbt;
    }

    @Override
    public Object readNbt(NbtCompound nbt) {
        Map<String, DefaultedList<ItemStack>> inventory = new HashMap<>();
        for (String key : nbt.getKeys()) {
            NbtCompound inventoryNbt = nbt.getCompound(key);
            if (inventoryNbt == null || inventoryNbt.isEmpty()) continue;

            int inventorySize = inventoryNbt.getInt("size");
            DefaultedList<ItemStack> items = DefaultedList.ofSize(inventorySize, ItemStack.EMPTY);
            Inventories.readNbt(inventoryNbt, items);

            inventory.put(key, items);
        }
        return inventory;
    }

    public static boolean shouldSaveSlot(PlayerEntity player, int slot) {
        for (KeepInventoryPower keepInventoryPower : PowerHolderComponent.getPowers(player, KeepInventoryPower.class)) {
            Set<Integer> slots = ((OriginKeepInventoryPowerAccessor) keepInventoryPower).getSlots();
            Predicate<ItemStack> keepItemCondition = ((OriginKeepInventoryPowerAccessor) keepInventoryPower).getKeepItemCondition();

            if (slots != null && !slots.contains(slot)) {
                return false;
            }
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isEmpty()) {
                if (keepItemCondition == null || keepItemCondition.test(stack)) {
                    return true;
                }
            }
        }
        return false;
    }
}
