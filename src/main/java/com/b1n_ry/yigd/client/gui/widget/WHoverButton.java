package com.b1n_ry.yigd.client.gui.widget;

import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.icon.Icon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class WHoverButton extends WButton {
    private final Component text;
    private static final Font TEXT_RENDERER = Minecraft.getInstance().font;
    public WHoverButton(Icon icon, Component text) {
        super(icon);

        this.text = text;

        this.width = 20;
    }

    @Override
    public boolean canResize() {
        return false;
    }

    @Override
    public void setSize(int x, int y) {
        super.setSize(20, 20);
    }

    @Override
    public void paint(GuiGraphics context, int x, int y, int mouseX, int mouseY) {
        super.paint(context, x, y, mouseX, mouseY);

        if (mouseX >= 0 && mouseX <= this.width && mouseY >= 0 && mouseY <= this.height) {
            context.renderTooltip(TEXT_RENDERER, this.text, x + mouseX, y + mouseY);
        }
    }
}
