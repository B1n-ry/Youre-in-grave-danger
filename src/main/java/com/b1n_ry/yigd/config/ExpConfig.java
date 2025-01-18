package com.b1n_ry.yigd.config;

import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

public class ExpConfig {
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public ExpDropBehaviour dropBehaviour = ExpDropBehaviour.BEST_OF_BOTH;

    @Comment("Ignored if dropBehaviour is set to VANILLA")
    @ConfigEntry.BoundedDiscrete(max = 100)
    public int dropPercentage = 0;

    @ConfigEntry.BoundedDiscrete(max = 100)
    public int keepPercentage = 0;
}
