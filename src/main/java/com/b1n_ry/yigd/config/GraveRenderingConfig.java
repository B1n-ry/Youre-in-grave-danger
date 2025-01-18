package com.b1n_ry.yigd.config;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

public class GraveRenderingConfig {
    public boolean useCustomFeatureRenderer = true;
    public boolean useSkullRenderer = true;
    public boolean useTextRenderer = true;
    @ConfigEntry.Gui.RequiresRestart
    public boolean adaptRenderer = false;
    public boolean useGlowingEffect = true;
    public int glowingDistance = 15;
}
