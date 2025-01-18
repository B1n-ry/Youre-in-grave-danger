package com.b1n_ry.yigd.config;

import com.b1n_ry.yigd.Yigd;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

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
    public GraveRenderingConfig graveRendering = new GraveRenderingConfig();

    @Comment("Toggleable custom features (registries)")
    @ConfigEntry.Gui.CollapsibleObject
    public ExtraFeaturesConfig extraFeatures = new ExtraFeaturesConfig();


    @Override
    public void validatePostLoad() throws ValidationException {
        ConfigData.super.validatePostLoad();
    }
}
