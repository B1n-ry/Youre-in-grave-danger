package com.b1n_ry.yigd.client.gui.widget;

import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.icon.Icon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WCardButton extends WButton {
    @Nullable
    private Component cardText = null;
    @Nullable
    private List<Component> tooltipText = null;

    private boolean coloredRendering;
    private int overlayColor;

    private static final Font TEXT_RENDERER = Minecraft.getInstance().font;

    public WCardButton(@Nullable Icon icon) {
        super(icon);
        this.coloredRendering = false;
        this.overlayColor = 0x00000000;
    }

    public void setCardText(@Nullable Component text) {
        this.cardText = text;
    }
    public void setTooltipText(@Nullable List<Component> text) {
        this.tooltipText = text;
    }
    public void setOverlayColor(int color) {
        this.overlayColor = color;
    }

    @Override
    public void paint(GuiGraphics context, int x, int y, int mouseX, int mouseY) {
        super.paint(context, x, y, mouseX, mouseY);

        if (this.coloredRendering)
            context.fill(x + 1, y + 1, x + this.width - 1, y + this.height - 1, this.overlayColor);

        if (this.cardText != null) context.drawString(TEXT_RENDERER, this.cardText, x + 20, y + 5, 0xFFFFFF, true);

        if (this.tooltipText != null && this.isWithinBounds(mouseX, mouseY)) {
            context.renderComponentTooltip(TEXT_RENDERER, this.tooltipText, x + mouseX, y + mouseY);
        }
    }

    public void setColoredRendering(boolean enabled) {
        this.coloredRendering = enabled;
    }
}
