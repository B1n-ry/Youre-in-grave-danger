package com.b1n_ry.yigd.config;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

public class ExtraFeaturesConfig {
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

    public static class DeathSightConfig {
        public boolean enabled = false;
        public boolean isTreasure = true;
        public boolean isAvailableForEnchantedBookOffer = true;
        public boolean isAvailableForRandomSelection = false;
        public double range = 64;
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public DeathSightConfig.GraveTargets targets = DeathSightConfig.GraveTargets.PLAYER_GRAVES;

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
        public GraveKeyConfig.KeyTargeting targeting = GraveKeyConfig.KeyTargeting.PLAYER_GRAVE;

        public enum KeyTargeting {
            ANY_GRAVE, PLAYER_GRAVE, SPECIFIC_GRAVE
        }
    }

    public static class ScrollConfig {
        public boolean enabled = false;
        public boolean rebindable = false;
        public boolean receiveOnRespawn = false;
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public ScrollConfig.ClickFunction clickFunction = ScrollConfig.ClickFunction.VIEW_CONTENTS;
        public boolean consumeOnUse = false;
        public int useTime = 0;
        public int useCooldown = 0;

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
        public GraveCompassConfig.CompassGraveTarget pointToClosest = GraveCompassConfig.CompassGraveTarget.DISABLED;

        public enum CompassGraveTarget {
            DISABLED, PLAYER, ALL
        }
    }
}
