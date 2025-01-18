package com.b1n_ry.yigd.config;

import com.b1n_ry.yigd.util.DropRule;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

public class CompatConfig {
    @Comment("Enables compatibility with Common Protection API (\"Claim API\"), if present")
    public boolean enableProtectionApiCompat = true;
    @Comment("Defines the standard drop rule in claims where the player can NOT place blocks. While PUT_IN_GRAVE, other drop rules will be prioritized")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public DropRule standardDropRuleInClaim = DropRule.DROP;
    @Comment("Defines the standard drop rule in claims where the player CAN place blocks. While PUT_IN_GRAVE, other drop rules will be prioritized")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public DropRule standardDropRuleInOwnClaim = DropRule.PUT_IN_GRAVE;

    public boolean enableAccessoriesCompat = true;
    @Comment("While PUT_IN_GRAVE, other drop rules will be prioritized")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public DropRule defaultAccessoriesDropRule = DropRule.PUT_IN_GRAVE;
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
    public boolean enableBeansBackpacksCompat = true;
    @Comment("While PUT_IN_GRAVE, other drop rules will be prioritized")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public DropRule defaultBeansBackpacksDropRule = DropRule.PUT_IN_GRAVE;
    public boolean enableRespawnObelisksCompat = true;
}
