package com.b1n_ry.yigd.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

public class YigdConfig implements ConfigData {
    public static YigdConfig getConfig() {
        return AutoConfig.getConfigHolder(YigdConfig.class).getConfig();
    }

    @ConfigEntry.Gui.CollapsibleObject
    public InventoryConfig inventoryConfig = new InventoryConfig();

    @ConfigEntry.Gui.CollapsibleObject
    public ExpConfig expConfig = new ExpConfig();

    @ConfigEntry.Gui.CollapsibleObject
    public GraveConfig graveConfig = new GraveConfig();


    public static class InventoryConfig {
        public boolean dropPlayerHead = false;
        public ItemLossConfig itemLoss = new ItemLossConfig();
        // delete enchantments
        // soulbinding enchantments
        // loose soulbound level
        // void slots
        // keep slots

        public static class ItemLossConfig {
            public boolean enabled = false;
        }
    }

    public static class RespawnConfig {

    }

    public static class ExpConfig {
        @ConfigEntry.Gui.EnumHandler
        public ExpDropBehaviour dropBehaviour = ExpDropBehaviour.VANILLA;

        @ConfigEntry.BoundedDiscrete(max = 100)
        public int dropPercentage = 50;
    }

    public static class GraveConfig {
        public boolean enabled = true;
        // what should it store? XP or items, none or both?
        // require some item (customizable. Default EMPTY, but could be grave)
        // require shovel to open
        // retrieve method (list with enums)
        // merge existing with claimed stacks
        // drop in inventory or on ground
        // drop grave block?
        // generate empty graves
        // spawn protection rule override?
        // inventory priority
        // robbing
        // timeout/deletion/despawn timer
        // curse of binding compat
        // ignore death types
        // unlockable
        // spawn something when opened?
        // use last ground position
        // block replacement blacklist/whitelist settings
        // replace old block when claimed
        // Keep grave after it's looted
        // grave generation dimension blacklist
        // generate grave in the void? (which y level then)
        // block under grave?
        // tell people where someone's grave is when they logg off
        // rendering
        // max backups
    }

    public static class CompatConfigs {

    }
}
