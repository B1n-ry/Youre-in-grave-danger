package com.b1n_ry.yigd.config;

import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.ArrayList;
import java.util.List;

public class RespawnConfig {
    @Comment("On respawn, all players will receive these effects")
    public List<EffectConfig> respawnEffects = new ArrayList<>();
    @Comment("HP given to player at respawn. If 0 or negative, default health will apply")
    public int respawnHealth = -1;
    @Comment("If false, player will respawn with the same hunger level as when they died")
    public boolean resetHunger = true;
    @Comment("Hunger given to player at respawn. If negative, default hunger will apply")
    public int respawnHunger = -1;
    @Comment("If false, player will respawn with the same saturation level as when they died")
    public boolean resetSaturation = true;
    @Comment("Saturation given to player at respawn. If negative, default saturation will apply")
    public float respawnSaturation = -1f;
    @Comment("Extra items that will be given to player once respawned")
    public List<ExtraItemDrop> extraItemDrops = new ArrayList<>();

    public static class EffectConfig {
        public String effectName;
        public int effectLevel;
        public int effectTime;
        public boolean showBubbles;

        // Constructor required for the config gui to work
        @SuppressWarnings("unused")
        public EffectConfig() {
            this.effectName = "";
            this.effectLevel = 0;
            this.effectTime = 0;
            this.showBubbles = false;
        }
    }

    public static class ExtraItemDrop {
        public String itemId;
        public int count;
        public String itemNbt;

        // Constructor required for the config gui to work
        @SuppressWarnings("unused")
        public ExtraItemDrop() {
            this.itemId = "";
            this.count = 0;
            this.itemNbt = "";
        }
    }
}
