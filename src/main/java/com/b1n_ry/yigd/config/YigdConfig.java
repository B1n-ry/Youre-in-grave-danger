package com.b1n_ry.yigd.config;

import com.b1n_ry.yigd.Yigd;
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
        public DeathEffectConfig deathInSpawnProtection = DeathEffectConfig.CREATE_GRAVE;

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
        public boolean unlockableGraves = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.CollapsibleObject
        public ItemLoss itemLoss = new ItemLoss();

        @ConfigEntry.Gui.Tooltip
        public List<String> deleteEnchantments = List.of("minecraft:vanishing_curse", "vanishing_curse"); // Apparently without the namespace/id vanilla will still make it work

        @ConfigEntry.Gui.Tooltip
        public List<String> soulboundEnchantments = List.of("yigd:soulbound");

        @ConfigEntry.Gui.Tooltip
        public List<Integer> voidSlots = new ArrayList<>();
        @ConfigEntry.Gui.Tooltip
        public List<Integer> soulboundSlots = new ArrayList<>();

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
        @ConfigEntry.Gui.CollapsibleObject
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

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public DeathEffectConfig claimRuleOverride = DeathEffectConfig.CREATE_GRAVE;

        @ConfigEntry.Gui.Tooltip
        public boolean prioritiseTheGraveyardGraves = false;
        @ConfigEntry.Gui.Tooltip
        public int graveyardSearchRadius = 10;
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
        @ConfigEntry.Gui.CollapsibleObject
        public EnchantmentConfig soulboundEnchant = new EnchantmentConfig(true, false, true, false);
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.CollapsibleObject
        public EnchantmentConfig deathSightEnchant = new EnchantmentConfig(false, true, true, true);
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.CollapsibleObject
        public ScrollSettings scrollItem = new ScrollSettings();
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.CollapsibleObject
        public GraveKeySettings graveKeySettings = new GraveKeySettings();
    }

    public static class ScrollSettings {
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public ScrollTypeConfig scrollType = ScrollTypeConfig.DISABLED;
        @ConfigEntry.Gui.Tooltip
        public boolean retrieveOnRespawn = true;
    }

    public static class GraveKeySettings {
        @ConfigEntry.Gui.RequiresRestart
        public boolean enableKeys = false;
        @ConfigEntry.Gui.Tooltip
        public boolean alwaysRequire = false;
        @ConfigEntry.Gui.Tooltip
        public boolean retrieveOnRespawn = true;
        @ConfigEntry.Gui.Tooltip
        public boolean getFromGui = false;
        @ConfigEntry.Gui.Tooltip
        public boolean rebindable = false;
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public GraveKeySpecificationConfig graveKeySpecification = GraveKeySpecificationConfig.PLAYER;
    }

    public static class CommandToggles {
        @ConfigEntry.Gui.RequiresRestart
        public String coreCommandName = "yigd";

        public boolean retrieveGrave = true;
        public boolean robGrave = true;
        @ConfigEntry.Gui.Tooltip
        public boolean selfView = true;
        @ConfigEntry.Gui.Tooltip
        public boolean adminView = true;
        public boolean moderateGraves = true;
        @ConfigEntry.Gui.Tooltip
        public boolean clearGraveBackups = true;
        @ConfigEntry.Gui.Tooltip
        public boolean whitelist = true;
        public boolean whitelistAdd = true;
        public boolean whitelistRemove = true;
        @ConfigEntry.Gui.Tooltip
        public boolean whitelistToggle = true;
    }

    public static class EnchantmentConfig {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled;
        @ConfigEntry.Gui.Tooltip
        public boolean isTreasure;
        @ConfigEntry.Gui.Tooltip
        public boolean villagerTrade;
        @ConfigEntry.Gui.Tooltip
        public boolean tableAndLoot;

        public EnchantmentConfig (boolean enabled, boolean isTreasure, boolean villagerTrade, boolean tableAndLoot) {
            this.enabled = enabled;
            this.isTreasure = isTreasure;
            this.villagerTrade = villagerTrade;
            this.tableAndLoot = tableAndLoot;
        }
    }

    public static YigdConfig getConfig() {
        return Yigd.defaultConfig == null ? AutoConfig.getConfigHolder(YigdConfig.class).getConfig() : Yigd.defaultConfig;
    }
}
