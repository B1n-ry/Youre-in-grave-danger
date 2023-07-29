package com.b1n_ry.yigd.client.gui;

import com.b1n_ry.yigd.packets.LightPlayerData;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

public class PlayerSelectionGui extends LightweightGuiDescription {
    private List<LightPlayerData> data;
    private int page;
    private Screen previousScreen;

    public PlayerSelectionGui(List<LightPlayerData> data, int page, Screen previousScreen) {
        this.data = data;
        this.page = page;
        this.previousScreen = previousScreen;
    }
}
