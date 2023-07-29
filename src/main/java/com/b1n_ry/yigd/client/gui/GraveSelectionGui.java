package com.b1n_ry.yigd.client.gui;

import com.b1n_ry.yigd.packets.LightGraveData;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

public class GraveSelectionGui extends LightweightGuiDescription {
    private List<LightGraveData> data;
    private int page;
    private Screen previousScreen;
    public GraveSelectionGui(List<LightGraveData> data, int page, Screen previousScreen) {
        this.data = data;
        this.page = page;
        this.previousScreen = previousScreen;
    }
}
