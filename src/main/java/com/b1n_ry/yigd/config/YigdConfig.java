package com.b1n_ry.yigd.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

@Config(name = "yigd")
public class YigdConfig implements ConfigData {
    @ConfigEntry.Gui.CollapsibleObject
    public GraveSettings graveSettings = new GraveSettings();

    @ConfigEntry.Gui.CollapsibleObject
    public UtilitySettings utilitySettings = new UtilitySettings();

    @ConfigEntry.Gui.CollapsibleObject
    public CommandToggles commandToggles = new CommandToggles();

    public static class GraveSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean generateGraves = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public RetrievalTypeConfig retrievalType = RetrievalTypeConfig.ON_USE;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public DropTypeConfig dropType = DropTypeConfig.IN_INVENTORY;

        @ConfigEntry.Gui.Tooltip
        public boolean dropPlayerHead = false;

        @ConfigEntry.Gui.Tooltip
        public boolean dropGraveBlock = false;
        @ConfigEntry.Gui.Tooltip
        public boolean requireGraveItem = false;

        @ConfigEntry.Gui.Tooltip
        public boolean generateEmptyGraves = false;

        @ConfigEntry.Gui.Tooltip
        public boolean ignoreSpawnProtection = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public PriorityInventoryConfig priority = PriorityInventoryConfig.GRAVE;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.CollapsibleObject
        public GraveRobbing graveRobbing = new GraveRobbing();

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.CollapsibleObject
        public GraveDeletion graveDeletion = new GraveDeletion();

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.CollapsibleObject
        public ItemLoss itemLoss = new ItemLoss();

        @ConfigEntry.Gui.Tooltip
        public List<String> deleteEnchantments = List.of("minecraft:vanishing:curse", "vanishing_curse"); // Apparently without the namespace/id vanilla will still make it work

        @ConfigEntry.Gui.Tooltip
        public List<String> soulboundEnchantments = List.of("yigd:soulbound");

        @ConfigEntry.Gui.Tooltip
        public boolean applyBindingCurse = true;

        @ConfigEntry.Gui.Tooltip
        public boolean trySoft = false;
        @ConfigEntry.Gui.Tooltip
        public boolean tryStrict = true;

        @ConfigEntry.Gui.Tooltip
        public boolean replaceWhenClaimed = true;

        @ConfigEntry.Gui.Tooltip
        public List<Integer> blacklistDimensions = new ArrayList<>();

        @ConfigEntry.Gui.Tooltip
        public boolean graveInVoid = true;

        @ConfigEntry.Gui.Tooltip
        public List<String> ignoreDeathTypes = new ArrayList<>();

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 255)
        public int graveSpawnHeight = 2;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public LastResortConfig lastResort = LastResortConfig.DROP_ITEMS;

        @ConfigEntry.Gui.Tooltip
        public boolean putXpInGrave = true;

        @ConfigEntry.Gui.Tooltip
        public boolean defaultXpDrop = false;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int xpDropPercent = 50;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.CollapsibleObject
        public BlockUnderGrave blockUnderGrave = new BlockUnderGrave();

        @ConfigEntry.Gui.Tooltip
        public boolean tellDeathPos = true;

        @ConfigEntry.Gui.Tooltip
        public GraveCompatConfig graveCompatConfig = new GraveCompatConfig();

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.CollapsibleObject
        public GraveRenderSettings graveRenderSettings = new GraveRenderSettings();

        @ConfigEntry.Gui.Tooltip
        public int maxGraveBackups = 50;
    }

    public static class GraveRobbing {
        @ConfigEntry.Gui.Tooltip
        public boolean enableRobbing = true;
        @ConfigEntry.Gui.Tooltip
        public boolean onlyMurderer = false;
        @ConfigEntry.Gui.Tooltip
        public int afterTime = 1;
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public TimeTypeConfig timeType = TimeTypeConfig.HOURS;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public PriorityInventoryConfig robPriority = PriorityInventoryConfig.INVENTORY;
    }

    public static class GraveDeletion {
        @ConfigEntry.Gui.Tooltip
        public boolean canDelete = false;
        @ConfigEntry.Gui.Tooltip
        public int afterTime = 1;
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public TimeTypeConfig timeType = TimeTypeConfig.HOURS;

        @ConfigEntry.Gui.Tooltip
        public boolean dropInventory = false;
    }

    public static class ItemLoss {
        @ConfigEntry.Gui.Tooltip
        public boolean enableLoss = false;
        @ConfigEntry.Gui.Tooltip
        public boolean ignoreSoulboundItems = true;
        @ConfigEntry.Gui.Tooltip
        public boolean affectStacks = false;
        @ConfigEntry.Gui.Tooltip
        public boolean usePercentRange = false;
        @ConfigEntry.Gui.Tooltip
        public int lossRangeFrom = 0;
        @ConfigEntry.Gui.Tooltip
        public int lossRangeTo = 5;
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int percentChanceOfLoss = 100;
    }

    public static class BlockUnderGrave {
        @ConfigEntry.Gui.Tooltip
        public boolean generateBlockUnder = true;

        @ConfigEntry.Gui.Tooltip
        public String inOverWorld = "minecraft:cobblestone";

        @ConfigEntry.Gui.Tooltip
        public String inNether = "minecraft:soul_soil";

        @ConfigEntry.Gui.Tooltip
        public String inTheEnd = "minecraft:end_stone";

        @ConfigEntry.Gui.Tooltip
        public String inCustom = "minecraft:dirt";
    }

    public static class GraveCompatConfig {
        @ConfigEntry.Gui.Tooltip
        public boolean levelzXpInGraves = true;
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int levelzXpDropPercent = 100;
    }

    public static class GraveRenderSettings {
        @ConfigEntry.Gui.Tooltip
        public boolean useRenderFeatures = true;

        public boolean renderGraveSkull = true;
        public boolean renderGraveOwner = true;
        @ConfigEntry.Gui.Tooltip
        public boolean textShadow = true;
        @ConfigEntry.Gui.Tooltip
        public boolean adaptRenderer = false;

        @ConfigEntry.Gui.Tooltip
        public boolean glowingGrave = true;
        @ConfigEntry.Gui.Tooltip
        public int glowMinDistance = 0;
        @ConfigEntry.Gui.Tooltip
        public int glowMaxDistance = 10;
    }

    public static class UtilitySettings {
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        public boolean soulboundEnchant = true;
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        public boolean deathSightEnchant = false;
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.CollapsibleObject
        public ScrollSettings scrollItem = new ScrollSettings();
    }

    public static class ScrollSettings {
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public ScrollTypeConfig scrollType = ScrollTypeConfig.DISABLED;
        @ConfigEntry.Gui.Tooltip
        public boolean retrieveOnRespawn = true;
    }

    public static class CommandToggles {
        public boolean retrieveGrave = true;
        public boolean robGrave = true;
        @ConfigEntry.Gui.Tooltip
        public boolean selfView = true;
        @ConfigEntry.Gui.Tooltip
        public boolean adminView = true;
        public boolean moderateGraves = true;
        @ConfigEntry.Gui.Tooltip
        public boolean clearGraveBackups = true;
    }

    public static YigdConfig getConfig() {
        return AutoConfig.getConfigHolder(YigdConfig.class).getConfig();
    }
}
