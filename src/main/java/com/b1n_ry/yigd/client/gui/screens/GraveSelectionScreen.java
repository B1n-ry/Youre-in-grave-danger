package com.b1n_ry.yigd.client.gui.screens;

import com.b1n_ry.yigd.client.gui.GraveSelectionGui;
import io.github.cottonmc.cotton.gui.GuiDescription;
import io.github.cottonmc.cotton.gui.client.CottonClientScreen;
import org.lwjgl.glfw.GLFW;

public class GraveSelectionScreen extends CottonClientScreen {
    public GraveSelectionScreen(GuiDescription description) {
        super(description);
    }

    @Override
    public boolean keyPressed(int ch, int keyCode, int modifiers) {
        if (ch == GLFW.GLFW_KEY_BACKSPACE && this.description instanceof GraveSelectionGui gui && this.client != null) {
            this.client.setScreen(gui.getPreviousScreen());
            return true;
        }

        return super.keyPressed(ch, keyCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
