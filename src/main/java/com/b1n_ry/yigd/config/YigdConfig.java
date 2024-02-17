package com.b1n_ry.yigd.config;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.util.DropRule;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

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
    public CompatConfig compatConfig = new CompatConfig();

    @ConfigEntry.Gui.CollapsibleObject
    public CommandConfig commandConfig = new CommandConfig();

    @Comment("Client only config")
    @ConfigEntry.Gui.CollapsibleObject
    public GraveRendering graveRendering = new GraveRendering();

    @Comment("Toggleable custom features (registries)")
    @ConfigEntry.Gui.CollapsibleObject
    public ExtraFeatures extraFeatures = new ExtraFeatures();


    public static class InventoryConfig {
        public boolean dropPlayerHead = false;
        @ConfigEntry.Gui.CollapsibleObject
        public ItemLossConfig itemLoss = new ItemLossConfig();
        // delete enchantments
        public List<String> vanishingEnchantments = new ArrayList<>() {{ add("minecraft:vanishing_curse"); }};
        // soulbinding enchantments
        public List<String> soulboundEnchantments = new ArrayList<>() {{ add("yigd:soulbound"); }};
        // loose soulbound level
        public boolean loseSoulboundLevelOnDeath = false;
        // void slots
        public List<Integer> vanishingSlots = new ArrayList<>();
        // keep slots
        public List<Integer> soulboundSlots = new ArrayList<>();
        public List<Integer> dropOnGroundSlots = new ArrayList<>();

        public static class ItemLossConfig {
            public boolean enabled = false;
            public boolean affectStacks = false;
            public boolean usePercentRange = true;
            public int lossRangeFrom = 0;
            public int lossRangeTo = 100;

            @Comment("Chance of losing an item (iterated over every item picked up by lossRange)")
            public int percentChanceOfLoss = 50;
            @Comment("If true, you can lose soulbound items from the item loss feature")
            public boolean canLoseSoulbound = false;
        }
    }

    public static class RespawnConfig {
        @Comment("On respawn, all players will receive these effects")
        public List<EffectConfig> respawnEffects = new ArrayList<>();
        @Comment("HP given to player at respawn. If 0 or negative, default health will apply")
        public int respawnHealth = 20;
        @Comment("If false, player will respawn with the same hunger level as when they died")
        public boolean resetHunger = true;
        @Comment("Hunger given to player at respawn. If negative, default hunger will apply")
        public int respawnHunger = 20;
        @Comment("If false, player will respawn with the same saturation level as when they died")
        public boolean resetSaturation = true;
        @Comment("Saturation given to player at respawn. If negative, default saturation will apply")
        public float respawnSaturation = 20f;
        @Comment("Extra items that will be given to player once respawned")
        public List<ExtraItemDrop> extraItemDrops = new ArrayList<>();

        public record EffectConfig(String effectName, int effectLevel, int effectTime, boolean showBubbles) {
            // Constructor required for the config gui to work
            @SuppressWarnings("unused")
            public EffectConfig() {
                this("", 0, 0, true);
            }
        }
        public record ExtraItemDrop(String itemId, int count, String itemNbt) {
            // Constructor required for the config gui to work
            @SuppressWarnings("unused")
            public ExtraItemDrop() {
                this("", 0, "");
            }
        }
    }

    public static class ExpConfig {
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public ExpDropBehaviour dropBehaviour = ExpDropBehaviour.BEST_OF_BOTH;

        @Comment("Ignored if dropBehaviour is set to VANILLA")
        @ConfigEntry.BoundedDiscrete(max = 100)
        public int dropPercentage = 0;

        @ConfigEntry.BoundedDiscrete(max = 100)
        public int keepPercentage = 0;
    }

    public static class GraveConfig {
        public boolean enabled = true;
        public boolean storeItems = true;
        public boolean storeXp = true;
        @Comment("Inform player where the grave generated when respawning")
        public boolean informGraveLocation = true;
        @Comment("If true, you HAVE to have one of `requiredItem` for a grave to generate. One of that item will then be consumed")
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
        @Comment("Minimum amount of blocks above void a grave can spawn")
        public int lowestGraveY = 3;
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
        // How far in X, Y, and Z the grave can generate from where you died
        @ConfigEntry.Gui.CollapsibleObject
        public Range generationMaxDistance = new Range();
        // block replacement blacklist/whitelist settings
        public boolean useSoftBlockWhitelist = false;
        public boolean useStrictBlockBlacklist = true;
        // replace old block when claimed
        public boolean replaceOldWhenClaimed = true;
        public boolean dropItemsIfDestroyed = false;
        // Keep grave after it's looted
        @Comment("If true, graves will persist when claiming them, and right clicking on them after that will let you know when and how they died. Can also then be mined")
        @ConfigEntry.Gui.CollapsibleObject
        public PersistentGraves persistentGraves = new PersistentGraves();
        // grave generation dimension blacklist
        public List<String> dimensionBlacklist = new ArrayList<>();
        // block under grave?
        @ConfigEntry.Gui.CollapsibleObject
        public  BlockUnderGrave blockUnderGrave = new BlockUnderGrave();
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
            public boolean onlyMurderer = false;
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
            public String spawnNbt = "{ArmorItems:[{},{},{},{id:\"minecraft:player_head\",tag:{SkullOwner:{Name:\"${owner.name}\",Id:\"${owner.uuid}\"}},Count:1b}]}";
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
                    add(new MapEntry("minecraft:the_nether", "minecraft:soul_soil"));
                    add(new MapEntry("minecraft:the_end", "minecraft:end_stone"));
                    add(new MapEntry("misc", "minecraft:dirt"));
            }};
            public boolean generateOnProtectedLand = false;
        }
    }

    public static class CompatConfig {
        @Comment("While PUT_IN_GRAVE, other drop rules will be prioritized")
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public DropRule standardDropRuleInClaim = DropRule.PUT_IN_GRAVE;

        public boolean enableInventorioCompat = true;
        @Comment("While PUT_IN_GRAVE, other drop rules will be prioritized")
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public DropRule defaultInventorioDropRule = DropRule.PUT_IN_GRAVE;
        public boolean enableLevelzCompat = true;
        @Comment("While PUT_IN_GRAVE, other drop rules will be prioritized")
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public DropRule defaultLevelzDropRule = DropRule.PUT_IN_GRAVE;
        public boolean enableNumismaticOverhaulCompat = true;
        @Comment("While PUT_IN_GRAVE, other drop rules will be prioritized")
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public DropRule defaultNumismaticDropRule = DropRule.PUT_IN_GRAVE;
        public boolean enableOriginsInventoryCompat = true;
        @Comment("While PUT_IN_GRAVE, other drop rules will be prioritized")
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public DropRule defaultOriginsDropRule = DropRule.PUT_IN_GRAVE;
        public boolean enableTravelersBackpackCompat = true;
        @Comment("While PUT_IN_GRAVE, other drop rules will be prioritized")
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public DropRule defaultTravelersBackpackDropRule = DropRule.PUT_IN_GRAVE;
        public boolean enableTrinketsCompat = true;
        @Comment("While PUT_IN_GRAVE, other drop rules will be prioritized")
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public DropRule defaultTrinketsDropRule = DropRule.PUT_IN_GRAVE;
    }

    public static class CommandConfig {
        public String mainCommand = "yigd";
        public int basePermissionLevel = 0;
        public int viewLatestPermissionLevel = 0;
        public int viewSelfPermissionLevel = 0;
        public int viewUserPermissionLevel = 2;
        public int viewAllPermissionLevel = 2;
        public int restorePermissionLevel = 2;
        public int robPermissionLevel = 2;
        public int whitelistPermissionLevel = 3;
        public int deletePermissionLevel = 3;
        public int unlockPermissionLevel = 0;
    }

    public static class GraveRendering {
        public boolean useCustomFeatureRenderer = true;
        public boolean useSkullRenderer = true;
        public boolean useTextRenderer = true;
        @ConfigEntry.Gui.RequiresRestart
        public boolean adaptRenderer = false;
        public boolean useGlowingEffect = true;
        public int glowingDistance = 15;
    }

    public static class ExtraFeatures {
        @ConfigEntry.Gui.CollapsibleObject
        public EnchantmentConfig soulboundEnchant = new EnchantmentConfig(true, true, true, false);
        @ConfigEntry.Gui.CollapsibleObject
        public DeathSightConfig deathSightEnchant = new DeathSightConfig();
        @ConfigEntry.Gui.CollapsibleObject
        public GraveKeyConfig graveKeys = new GraveKeyConfig();
        @ConfigEntry.Gui.CollapsibleObject
        public ScrollConfig deathScroll = new ScrollConfig();
        @ConfigEntry.Gui.CollapsibleObject
        public GraveCompassConfig graveCompass = new GraveCompassConfig();

        public static class EnchantmentConfig {
            public boolean enabled;
            public boolean isTreasure;
            public boolean isAvailableForEnchantedBookOffer;
            public boolean isAvailableForRandomSelection;

            public EnchantmentConfig(boolean enabled, boolean isTreasure, boolean isAvailableForEnchantedBookOffer, boolean isAvailableForRandomSelection) {
                this.enabled = enabled;
                this.isTreasure = isTreasure;
                this.isAvailableForEnchantedBookOffer = isAvailableForEnchantedBookOffer;
                this.isAvailableForRandomSelection = isAvailableForRandomSelection;
            }
        }
        public static class DeathSightConfig /* extends EnchantmentConfig */ {
            public boolean enabled = false;
            public boolean isTreasure = true;
            public boolean isAvailableForEnchantedBookOffer = true;
            public boolean isAvailableForRandomSelection = false;
            public double range = 64;
            @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
            public GraveTargets targets = GraveTargets.PLAYER_GRAVES;

            /*
            Reimplement when https://github.com/shedaniel/cloth-config/pull/236 is merged
            public DeathSightConfig() {
                super(false, true, true, false);
            }
            */

            public enum GraveTargets {
                OWN_GRAVES, PLAYER_GRAVES, ALL_GRAVES
            }
        }
        public static class GraveKeyConfig {
            public boolean enabled = false;
            public boolean rebindable = true;
            public boolean required = true;
            public boolean receiveOnRespawn = true;
            public boolean obtainableFromGui = false;
            @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
            public KeyTargeting targeting = KeyTargeting.PLAYER_GRAVE;
            public enum KeyTargeting {
                ANY_GRAVE, PLAYER_GRAVE, SPECIFIC_GRAVE
            }
        }
        public static class ScrollConfig {
            public boolean enabled = false;
            public boolean rebindable = false;
            public boolean receiveOnRespawn = false;
            @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
            public ClickFunction clickFunction = ClickFunction.VIEW_CONTENTS;
            public enum ClickFunction {
                RESTORE_CONTENTS, VIEW_CONTENTS, TELEPORT_TO_LOCATION
            }
        }
        public static class GraveCompassConfig {
            public boolean receiveOnRespawn = false;
            public boolean consumeOnUse = true;
            public boolean deleteWhenUnlinked = true;
            public boolean cloneRecoveryCompassWithGUI = false;
            @ConfigEntry.Gui.RequiresRestart
            @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
            public CompassGraveTarget pointToClosest = CompassGraveTarget.DISABLED;
            public enum CompassGraveTarget {
                DISABLED, PLAYER, ALL
            }
        }
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


    @Override
    public void validatePostLoad() throws ValidationException {
        ConfigData.super.validatePostLoad();
    }
}
