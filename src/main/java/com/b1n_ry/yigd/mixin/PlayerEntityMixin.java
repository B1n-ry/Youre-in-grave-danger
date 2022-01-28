package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.PlayerEntityExt;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements PlayerEntityExt {
    private DefaultedList<ItemStack> soulboundInventory;

    @Shadow @Final public PlayerInventory inventory;

    @Shadow public abstract boolean equip(int slot, ItemStack item);

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Redirect(method = "dropInventory", at = @At(value = "INVOKE", target = "net.minecraft.entity.player.PlayerInventory.dropAll()V"))
    private void dropAll(PlayerInventory inventory) {
        if (this.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return;

        PlayerEntity player = inventory.player;

        int xpPoints;
        YigdConfig.GraveSettings graveSettings = YigdConfig.getConfig().graveSettings;
        if (graveSettings.defaultXpDrop) {
            xpPoints = Math.min(7 * player.experienceLevel, 100);
        } else {
            int currentLevel = player.experienceLevel;
            int totalExperience = (int) (Math.pow(currentLevel, 2) + 6 * currentLevel + player.experienceProgress);
            xpPoints = (int) ((graveSettings.xpDropPercent / 100f) * totalExperience);
        }

        player.totalExperience = 0;
        player.experienceProgress = 0;
        player.experienceLevel = 0;

        Yigd.NEXT_TICK.add(() -> {
            DefaultedList<ItemStack> items = DefaultedList.of();
            items.addAll(inventory.main);
            items.addAll(inventory.armor);
            items.addAll(inventory.offHand);

            List<String> soulboundEnchantments = YigdConfig.getConfig().graveSettings.soulboundEnchantments; // Get a string array with all soulbound enchantment names
            soulboundInventory = Yigd.getEnchantedItems(items, soulboundEnchantments); // Get all soulbound enchanted items in inventory

            if (!YigdConfig.getConfig().graveSettings.generateGraves) {
                this.inventory.dropAll();
                int i = xpPoints;

                while (i > 0) {
                    int j = ExperienceOrbEntity.roundToOrbSize(i);
                    i -= j;
                    this.world.spawnEntity(new ExperienceOrbEntity(this.world, this.getX(), this.getY(), this.getZ(), j));
                }
                return;
            }

            int dimId = this.world.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).getRawId(this.world.getDimension());
            if (YigdConfig.getConfig().graveSettings.blacklistDimensions.contains(dimId)) {
                this.inventory.dropAll();
                int i = xpPoints;

                while (i > 0) {
                    int j = ExperienceOrbEntity.roundToOrbSize(i);
                    i -= j;
                    this.world.spawnEntity(new ExperienceOrbEntity(this.world, this.getX(), this.getY(), this.getZ(), j));
                }
                return;
            }

            Yigd.removeFromList(items, soulboundInventory); // Keep soulbound items from appearing in both player inventory and grave

            List<String> removeEnchantments = YigdConfig.getConfig().graveSettings.deleteEnchantments; // List with enchantments to delete
            DefaultedList<ItemStack> removeFromGrave = Yigd.getEnchantedItems(items, removeEnchantments); // Find all items to be removed
            Yigd.removeFromList(items, removeFromGrave); // Delete items with set enchantment

            this.inventory.clear(); // Make sure your items are in fact deleted, and not accidentally retrieved when you respawn

            List<ItemStack> syncHotbar = soulboundInventory.subList(0, 10);
            for (int i = 0; i < 10; i++) {
                this.equip(i, syncHotbar.get(i));
            }

            Yigd.placeDeathGrave(this.world, this.getPos(), this.inventory.player, items, xpPoints);
        });
    }

    public DefaultedList<ItemStack> getSoulboundInventory() {
        return soulboundInventory;
    }
}
