package com.b1n_ry.yigd.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.icon.Icon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WCardButton extends WButton {
    @Nullable
    private Text cardText = null;
    @Nullable
    private List<Text> tooltipText = null;

    private boolean coloredRendering;
    private int overlayColor;

    private static final TextRenderer TEXT_RENDERER = MinecraftClient.getInstance().textRenderer;

    public WCardButton(@Nullable Icon icon) {
        super(icon);
        this.coloredRendering = false;
        this.overlayColor = 0x00000000;
    }

    public void setCardText(@Nullable Text text) {
        this.cardText = text;
    }
    public void setTooltipText(@Nullable List<Text> text) {
        this.tooltipText = text;
    }
    public void setOverlayColor(int color) {
        this.overlayColor = color;
    }

    @Override
    public void paint(DrawContext context, int x, int y, int mouseX, int mouseY) {
        super.paint(context, x, y, mouseX, mouseY);

        if (this.coloredRendering)
            context.fill(x + 1, y + 1, x + this.width - 1, y + this.height - 1, this.overlayColor);

        if (this.cardText != null) context.drawText(TEXT_RENDERER, this.cardText, x + 20, y + 5, 0xFFFFFF, true);

        if (this.tooltipText != null && this.isWithinBounds(mouseX, mouseY)) {
            context.drawTooltip(TEXT_RENDERER, this.tooltipText, x + mouseX, y + mouseY);
        }
    }

    public void setColoredRendering(boolean enabled) {
        this.coloredRendering = enabled;
    }
}
