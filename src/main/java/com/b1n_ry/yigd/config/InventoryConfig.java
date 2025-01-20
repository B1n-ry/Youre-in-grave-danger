package com.b1n_ry.yigd.config;

import com.b1n_ry.yigd.util.DropRule;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.ArrayList;
import java.util.List;

public class InventoryConfig {
    public boolean dropPlayerHead = false;
    @ConfigEntry.Gui.CollapsibleObject
    public ItemLossConfig itemLoss = new ItemLossConfig();
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
        public boolean weightedSelection = true;
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public DropRule lossDropRule = DropRule.DESTROY;

        @Comment("Chance of losing an item (iterated over every item picked up by lossRange)")
        public int percentChanceOfLoss = 50;
        @Comment("If true, you can lose soulbound items from the item loss feature")
        public boolean canLoseSoulbound = false;
        public boolean includeModdedInventories = true;
    }
}
