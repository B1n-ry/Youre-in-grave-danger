package com.b1n_ry.yigd.client.gui.screens;

import io.github.cottonmc.cotton.gui.GuiDescription;
import io.github.cottonmc.cotton.gui.client.CottonClientScreen;

public class GraveOverviewScreen extends CottonClientScreen {
    public GraveOverviewScreen(GuiDescription description) {
        super(description);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
