package com.b1n_ry.yigd.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.*;

@Config(name = "yigd")
public class YigdConfig implements ConfigData {
    @ConfigEntry.Gui.CollapsibleObject
    public GraveSettings graveSettings = new GraveSettings();

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
        public List<String> deleteEnchantments = new ArrayList<>();

        @ConfigEntry.Gui.Tooltip
        public List<String> soulboundEnchantments = Collections.singletonList("yigd:soulbound");

        @ConfigEntry.Gui.Tooltip
        public List<String> blacklistBlocks = Collections.singletonList("minecraft:bedrock");

        @ConfigEntry.Gui.Tooltip
        public List<Integer> blacklistDimensions = new ArrayList<>();

        @ConfigEntry.Gui.Tooltip
        public boolean graveInVoid = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public LastResortConfig lastResort = LastResortConfig.DROP_ITEMS;

        @ConfigEntry.Gui.Tooltip
        public boolean defaultXpDrop = false;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int xpDropPercent = 50;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.CollapsibleObject
        public BlockUnderGrave blockUnderGrave = new BlockUnderGrave();

        public boolean renderGraveSkull = true;
        public boolean renderGraveOwner = true;
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

        @ConfigEntry.Gui.Tooltip
        public List<String> whiteListBlocks = Arrays.asList("minecraft:air", "minecraft:water", "minecraft:lava");
    }

    public static YigdConfig getConfig() {
        return AutoConfig.getConfigHolder(YigdConfig.class).getConfig();
    }
}
