package com.b1n_ry.yigd.client.gui.widget;

import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class WItemStack extends WWidget {
    private final ItemStack stack;
    private final Font textRenderer;

    public WItemStack(ItemStack stack, int slotSize) {
        this.stack = stack;
        this.width = slotSize - 2;
        this.height = slotSize - 2;

        Minecraft client = Minecraft.getInstance();
        this.textRenderer = client.font;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void paint(GuiGraphics context, int x, int y, int mouseX, int mouseY) {
        BackgroundPainter.SLOT.paintBackground(context, x, y, this);

        context.renderFakeItem(this.stack, x, y);
        context.renderItemDecorations(this.textRenderer, this.stack, x, y);

        if (this.isHovered()) {
            context.renderComponentHoverEffect(this.textRenderer, this.stack.getDisplayName().getStyle(), mouseX + x, mouseY + y);
        }
    }
}
