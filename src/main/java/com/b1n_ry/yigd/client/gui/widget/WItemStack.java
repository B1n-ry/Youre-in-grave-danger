package com.b1n_ry.yigd.client.gui.widget;

import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public class WItemStack extends WWidget {
    private final ItemStack stack;
    private final TextRenderer textRenderer;

    public WItemStack(ItemStack stack, int slotSize) {
        this.stack = stack;
        this.width = slotSize - 2;
        this.height = slotSize - 2;

        MinecraftClient client = MinecraftClient.getInstance();
        this.textRenderer = client.textRenderer;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void paint(DrawContext context, int x, int y, int mouseX, int mouseY) {
        BackgroundPainter.SLOT.paintBackground(context, x, y, this);

        context.drawItem(this.stack, x, y);
        context.drawItemInSlot(this.textRenderer, this.stack, x, y);

        if (this.isHovered()) {
            context.drawHoverEvent(this.textRenderer, this.stack.toHoverableText().getStyle(), mouseX + x, mouseY + y);
        }
    }
}
