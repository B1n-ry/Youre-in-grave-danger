package com.b1n_ry.yigd.config;

import com.b1n_ry.yigd.Yigd;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Config(name = Yigd.MOD_ID)
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

    @ConfigEntry.Gui.CollapsibleObject
    public RespawnConfig respawnConfig = new RespawnConfig();

    @ConfigEntry.Gui.CollapsibleObject
    public CommandConfig commandConfig = new CommandConfig();

    // rendering
    @ConfigEntry.Gui.CollapsibleObject
    public GraveRendering graveRendering = new GraveRendering();


    public static class InventoryConfig {
        public boolean dropPlayerHead = false;
        @ConfigEntry.Gui.CollapsibleObject
        public ItemLossConfig itemLoss = new ItemLossConfig();
        // delete enchantments
        public List<String> vanishingEnchantments = List.of("minecraft:vanishing_curse");
        // soulbinding enchantments
        public List<String> soulboundEnchantments = List.of("yigd:soulbound");
        // loose soulbound level
        public boolean loseSoulboundLevelOnDeath = false;
        // void slots
        public List<Integer> vanishingSlots = new ArrayList<>();
        // keep slots
        public List<Integer> soulboundSlots = new ArrayList<>();

        public static class ItemLossConfig {
            public boolean enabled = false;
            public boolean affectStacks = false;
            public boolean usePercentRange = true;
            public int lossRangeFrom = 0;
            public int lossRangeTo = 100;

            public int percentChanceOfLoss = 50;
            public boolean canLoseSoulbound = false;
        }
    }

    public static class RespawnConfig {
        public List<EffectConfig> respawnEffects = new ArrayList<>();
        public int respawnHealth = 20;
        public boolean resetHunger = true;
        public int respawnHunger = 20;
        public boolean resetSaturation = true;
        public float respawnSaturation = 20f;

        public record EffectConfig(String effectName, int effectLevel, int effectTime, boolean showBubbles) {
            // Constructor required for the config gui to work
            @SuppressWarnings("unused")
            public EffectConfig() {
                this("", 0, 0, false);
            }
        }
    }

    public static class ExpConfig {
        @ConfigEntry.Gui.EnumHandler
        public ExpDropBehaviour dropBehaviour = ExpDropBehaviour.VANILLA;

        @ConfigEntry.BoundedDiscrete(max = 100)
        public int dropPercentage = 50;

        @ConfigEntry.BoundedDiscrete(max = 100)
        public int keepPercentage = 0;
    }

    public static class GraveConfig {
        public boolean enabled = true;
        // what should it store? XP or items, none or both?
        public boolean storeItems = true;
        public boolean storeXp = true;
        // Inform the player where the grave generated when respawning
        public boolean informGraveLocation = true;
        // require some item (customizable. Default EMPTY, but could be grave)
        public boolean requireItem = false;
        public String requiredItem = "yigd:grave";
        // require shovel to open
        public boolean requireShovelToLoot = false;
        // retrieve method (list with enums)
        @ConfigEntry.Gui.CollapsibleObject
        public RetrieveMethods retrieveMethods = new RetrieveMethods();
        // merge existing with claimed stacks for player
        public boolean mergeStacksOnRetrieve = true;
        // drop in inventory or on ground
        public DropType dropOnRetrieve = DropType.IN_INVENTORY;
        // drop grave block?
        public boolean dropGraveBlock = false;
        // No head. Because that's in the inventory module
        // generate empty graves
        public boolean generateEmptyGraves = false;
        // spawn protection rule override?
        public boolean overrideSpawnProtection = true;
        // inventory priority
        public ClaimPriority claimPriority = ClaimPriority.GRAVE;
        // robbing
        @ConfigEntry.Gui.CollapsibleObject
        public GraveRobbing graveRobbing = new GraveRobbing();
        // timeout/deletion/despawn timer
        @ConfigEntry.Gui.CollapsibleObject
        public GraveTimeout graveTimeout = new GraveTimeout();
        // curse of binding compat
        public boolean treatBindingCurse = true;
        // generate grave in the void? (which y level then)
        public boolean generateGraveInVoid = true;
        public int lowestGraveY = 3;
        // Weather or not the grave can generate outside the world border
        public boolean generateOnlyWithinBorder = true;
        // ignore death types
        public List<String> ignoredDeathTypes = new ArrayList<>();
        // unlockable
        public boolean unlockable = true;
        // spawn something when opened?
        @ConfigEntry.Gui.CollapsibleObject
        public RandomSpawn randomSpawn = new RandomSpawn();
        // use last ground position
        public boolean generateOnLastGroundPos = false;
        // How far in X, Y, and Z the grave can generate from where you died
        @ConfigEntry.Gui.CollapsibleObject
        public Range generationMaxDistance = new Range();
        // block replacement blacklist/whitelist settings
        public boolean useSoftBlockWhitelist = false;
        public boolean useStrictBlockBlacklist = true;
        // replace old block when claimed
        public boolean replaceOldWhenClaimed = true;
        // Keep grave after it's looted
        public boolean persistentGraves = false;
        // grave generation dimension blacklist
        public List<String> dimensionBlacklist = new ArrayList<>();
        // block under grave?
        @ConfigEntry.Gui.CollapsibleObject
        public  BlockUnderGrave blockUnderGrave = new BlockUnderGrave();
        // tell people where someone's grave is when they logg off
        public boolean sellOutOfflinePeople = false;
        // max backups
        public int maxBackupsPerPerson = 50;

        public static class RetrieveMethods {
            public boolean onClick = true;
            public boolean onBreak = false;
            public boolean onSneak = false;
            public boolean onStand = false;
        }

        public static class GraveRobbing {
            public boolean enabled = true;
            public boolean onlyMurderer = false;
            public int afterTime = 1;
            public TimeUnit timeUnit = TimeUnit.HOURS;
            public ClaimPriority robPriority = ClaimPriority.INVENTORY;
            public boolean notifyWhenRobbed = true;
            public boolean tellWhoRobbed = true;
        }

        public static class GraveTimeout {
            public boolean enabled = false;
            public int afterTime = 5;
            public TimeUnit timeUnit = TimeUnit.HOURS;
            public boolean dropContentsOnTimeout = true;
        }

        public static class RandomSpawn {
            public int percentSpawnChance = 0;
            public String spawnEntity = "minecraft:zombie";
            public String spawnNbt = "{ArmorItems:[{},{},{},{id:\"minecraft:player_head\",tag:{SkullOwner:{Name:\"${name}\",Id:\"${uuid}\"}},Count:1b}]}";
        }

        public static class Range {
            public int x = 5;
            public int y = 5;
            public int z = 5;
        }

        public static class BlockUnderGrave {
            public boolean enabled = true;
            public List<MapEntry> blockInDimensions = new ArrayList<>() {{
                add(new MapEntry("minecraft:overworld", "minecraft:cobblestone"));
                add(new MapEntry("minecraft:nether", "minecraft:soul_soil"));
                add(new MapEntry("minecraft:end", "minecraft:end_stone"));
                add(new MapEntry("misc", "minecraft:dirt"));
            }};
            public boolean generateOnProtectedLand = false;
        }
    }

    public static class CompatConfig {

    }

    public static class CommandConfig {
        public String mainCommand = "yigd";
    }

    public static class GraveRendering {

    }

    public static class MapEntry {
        public String key;
        public String value;

        @SuppressWarnings("unused")
        public MapEntry() {  // Required for cloth config GUI to work
            this.key = "";
            this.value = "";
        }
        public MapEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
