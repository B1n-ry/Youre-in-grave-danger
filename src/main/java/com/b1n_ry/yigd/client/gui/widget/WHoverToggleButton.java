package com.b1n_ry.yigd.client.gui.widget;

import io.github.cottonmc.cotton.gui.widget.WToggleButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class WHoverToggleButton extends WToggleButton {
    private final Text onText;
    private final Text offText;

    private static final TextRenderer TEXT_RENDERER = MinecraftClient.getInstance().textRenderer;

    public WHoverToggleButton(Identifier onImg, Text onText, Identifier offImg, Text offText) {
        super(onImg, offImg);

        this.onText = onText;
        this.offText = offText;
    }

    @Override
    public void paint(DrawContext context, int x, int y, int mouseX, int mouseY) {
        super.paint(context, x, y, mouseX, mouseY);

        if (mouseX >= 0 && mouseX <= this.width && mouseY >= 0 && mouseY <= this.height) {
            context.drawTooltip(TEXT_RENDERER, this.isOn ? this.onText : this.offText, x + mouseX, y + mouseY);
        }
    }
}
