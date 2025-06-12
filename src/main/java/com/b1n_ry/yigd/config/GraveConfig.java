package com.b1n_ry.yigd.config;

import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GraveConfig {
    public boolean enabled = true;
    public boolean storeItems = true;
    public boolean storeXp = true;
    @Comment("Inform player where the grave generated when respawning")
    public boolean informGraveLocation = true;
    @Comment("If true, you HAVE to have `requiredItemCount` number of `requiredItem` for a grave to generate. That many of that item will then be consumed")
    public boolean requireItem = false;
    public String requiredItem = "yigd:grave";
    public int requiredItemCount = 1;
    // require shovel to open
    public boolean requireShovelToLoot = false;
    // retrieve method (list with enums)
    @ConfigEntry.Gui.CollapsibleObject
    public RetrieveMethods retrieveMethods = new RetrieveMethods();
    // merge existing with claimed stacks for player
    public boolean mergeStacksOnRetrieve = true;
    // drop in inventory or on ground
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public DropType dropOnRetrieve = DropType.IN_INVENTORY;
    // drop grave block?
    public boolean dropGraveBlock = false;
    // No head. Because that's in the inventory module
    // generate empty graves
    public boolean generateEmptyGraves = false;
    // spawn protection rule override?
    @Comment("Allows everyone to bypass spawn protection for grave blocks")
    public boolean overrideSpawnProtection = true;
    // inventory priority
    @Comment("Which of the layout in the grave or in your inventory should be prioritized")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public ClaimPriority claimPriority = ClaimPriority.GRAVE;
    // robbing
    @ConfigEntry.Gui.CollapsibleObject
    public GraveRobbing graveRobbing = new GraveRobbing();
    // timeout/deletion/despawn timer
    @ConfigEntry.Gui.CollapsibleObject
    public GraveTimeout graveTimeout = new GraveTimeout();
    // curse of binding compat
    @Comment("If false, layout prioritizing doesn't care if armor is cursed with binding")
    public boolean treatBindingCurse = true;
    // generate grave in the void? (which y level then)
    public boolean generateGraveInVoid = true;
    @Comment("Minimum y-level a grave can spawn in a dimension")
    public List<MapEntryConfig.IntType> minimumGraveYLevel = new ArrayList<>() {{
        add(new MapEntryConfig.IntType("minecraft:overworld", -60));
        add(new MapEntryConfig.IntType("minecraft:the_nether", 3));
        add(new MapEntryConfig.IntType("minecraft:the_end", 3));
        add(new MapEntryConfig.IntType("misc", 3));
    }};
    // Weather or not the grave can generate outside the world border
    public boolean generateOnlyWithinBorder = true;
    // ignore death types
    public List<String> ignoredDeathTypes = new ArrayList<>();
    // unlockable
    @Comment("Allow players to unlock their graves through GUI")
    public boolean unlockable = true;
    // spawn something when opened?
    @ConfigEntry.Gui.CollapsibleObject
    public RandomSpawn randomSpawn = new RandomSpawn();
    // use last ground position
    public boolean generateOnLastGroundPos = false;
    public boolean tryGenerateOnGround = false;
    // How far in X, Y, and Z the grave can generate from where you died
    @ConfigEntry.Gui.CollapsibleObject
    public Range generationMaxDistance = new Range();
    // block replacement blacklist/whitelist settings
    public boolean useSoftBlockWhitelist = false;
    public boolean useStrictBlockBlacklist = true;
    // replace old block when claimed
    public boolean replaceOldWhenClaimed = true;
    public boolean dropItemsIfDestroyed = false;
    public boolean notifyOwnerIfDestroyed = true;
    // Keep grave after it's looted
    @Comment("If true, graves will persist when claiming them, and right clicking on them after that will let you know when and how they died. Can also then be mined")
    @ConfigEntry.Gui.CollapsibleObject
    public PersistentGraves persistentGraves = new PersistentGraves();
    // grave generation dimension blacklist
    public List<String> dimensionBlacklist = new ArrayList<>();
    // block under grave?
    @ConfigEntry.Gui.CollapsibleObject
    public BlockUnderGrave blockUnderGrave = new BlockUnderGrave();
    // tell people where someone's grave is when they logg off
    @Comment("When people leave, should the game let everyone know where they have a grave?")
    public boolean sellOutOfflinePeople = false;
    // max backups
    @Comment("Max amount of backed up graves")
    public int maxBackupsPerPerson = 50;
    public boolean dropFromOldestWhenDeleted = true;

    public static class RetrieveMethods {
        public boolean onClick = true;
        public boolean onBreak = false;
        public boolean onSneak = false;
        public boolean onStand = false;
    }

    public static class GraveRobbing {
        public boolean enabled = true;
        public boolean killerSkipWaitTime = false;
        public int afterTime = 1;
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public TimeUnit timeUnit = TimeUnit.HOURS;
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public ClaimPriority robPriority = ClaimPriority.INVENTORY;
        public boolean notifyWhenRobbed = true;
        public boolean tellWhoRobbed = true;
    }

    public static class GraveTimeout {
        public boolean enabled = false;
        public int afterTime = 5;
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public TimeUnit timeUnit = TimeUnit.HOURS;
        public boolean dropContentsOnTimeout = true;
    }

    public static class PersistentGraves {
        public boolean enabled = false;
        public boolean showDeathDay = true;
        public boolean showDeathIrlTime = true;
        public boolean useAmPm = true;
    }

    public static class RandomSpawn {
        public int percentSpawnChance = 0;
        public String spawnEntity = "minecraft:zombie";
        public String spawnNbt = "{ArmorItems:[{},{},{},{id:\"minecraft:player_head\",tag:{SkullOwner:\"${owner.name}\"},Count:1b}]}";
    }

    public static class Range {
        public int x = 5;
        public int y = 5;
        public int z = 5;
    }

    public static class BlockUnderGrave {
        public boolean enabled = true;
        public List<MapEntryConfig.StringType> blockInDimensions = new ArrayList<>() {{
                add(new MapEntryConfig.StringType("minecraft:overworld", "minecraft:cobblestone"));
                add(new MapEntryConfig.StringType("minecraft:the_nether", "minecraft:soul_soil"));
                add(new MapEntryConfig.StringType("minecraft:the_end", "minecraft:end_stone"));
                add(new MapEntryConfig.StringType("misc", "minecraft:dirt"));
        }};
        @Comment("Defines whether the block under grave can be generated in claims where the player can NOT place blocks if protection api compat is enabled")
        public boolean generateOnProtectedLand = false;
        @Comment("Defines whether the block under grave can be generated in claims where the player CAN place blocks if protection api compat is enabled")
        public boolean generateInOwnClaim = true;
    }
}
