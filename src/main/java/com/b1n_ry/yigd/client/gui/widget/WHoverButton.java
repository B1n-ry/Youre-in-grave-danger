package com.b1n_ry.yigd.client.gui.widget;

import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.icon.Icon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class WHoverButton extends WButton {
    private final Text text;
    private static final TextRenderer TEXT_RENDERER = MinecraftClient.getInstance().textRenderer;
    public WHoverButton(Icon icon, Text text) {
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
    public void paint(DrawContext context, int x, int y, int mouseX, int mouseY) {
        super.paint(context, x, y, mouseX, mouseY);

        if (mouseX >= 0 && mouseX <= this.width && mouseY >= 0 && mouseY <= this.height) {
            context.drawTooltip(TEXT_RENDERER, this.text, x + mouseX, y + mouseY);
        }
    }
}
