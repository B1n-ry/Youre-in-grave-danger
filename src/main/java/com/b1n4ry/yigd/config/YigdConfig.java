package com.b1n4ry.yigd.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.*;

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
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public PriorityInventoryConfig priority = PriorityInventoryConfig.GRAVE;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.CollapsibleObject
        public GraveRobbing graveRobbing = new GraveRobbing();

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.CollapsibleObject
        public GraveDeletion graveDeletion = new GraveDeletion();

        @ConfigEntry.Gui.Tooltip
        public List<String> deleteEnchantments = Collections.singletonList("minecraft:vanishing_curse");

        @ConfigEntry.Gui.Tooltip
        public List<String> soulboundEnchantments = Collections.singletonList("yigd:soulbound");

        @ConfigEntry.Gui.Tooltip
        public boolean trySoft = true;
        @ConfigEntry.Gui.Tooltip
        public boolean tryStrict = true;

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
        @ConfigEntry.Gui.CollapsibleObject
        public GraveRenderSettings graveRenderSettings = new GraveRenderSettings();
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
        public boolean glowingGrave = false;
        @ConfigEntry.Gui.Tooltip
        public int glowMinDistance = 10;
    }

    public static class UtilitySettings {
        @ConfigEntry.Gui.Tooltip
        public boolean soulboundEnchant = true;
        @ConfigEntry.Gui.Tooltip
        public boolean teleportScroll = false;
    }

    public static class CommandToggles {
        public boolean retrieveGrave = true;
        public boolean robGrave = true;
        @ConfigEntry.Gui.Tooltip
        public boolean selfView = true;
        @ConfigEntry.Gui.Tooltip
        public boolean adminView = true;
        public boolean moderateGraves = true;
    }

    public static YigdConfig getConfig() {
        return AutoConfig.getConfigHolder(YigdConfig.class).getConfig();
    }
}
