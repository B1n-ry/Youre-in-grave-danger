package com.b1n_ry.yigd.client.gui;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Environment(EnvType.CLIENT)
public class GraveViewScreen extends Screen {
    private final Identifier GRAVE_VIEW_TEXTURE = new Identifier("yigd", "textures/gui/grave_view.png");

    private final DeadPlayerData data;

    private ItemStack hoveredStack = null;
    private final int modItemSize;
    private final int xpLevels;
    private final Screen previousScreen;

    public GraveViewScreen(DeadPlayerData data, @Nullable Screen previousScreen) {
        super(data.graveOwner != null ? new TranslatableText("text.yigd.gui.grave_view.title", data.graveOwner.getName()) : new TranslatableText("text.yigd.gui.grave_view.title.missing"));
        this.data = data;
        this.previousScreen = previousScreen;

        int size = 0;
        for (int i = 41; i < data.inventory.size(); i++) {
            if (data.inventory.get(i).isEmpty()) continue;
            size++;
        }
        for (int i = 0; i < data.modInventories.size(); i++) {
            YigdApi yigdApi = Yigd.apiMods.get(i);
            size += yigdApi.getInventorySize(data.modInventories.get(i));
        }
        this.modItemSize = size;

        int points = data.xp;
        int i;
        for (i = 0; points >= 0; i++) {
            if (i < 16) points -= (2 * i) + 7;
            else if (i < 31) points -= (5 * i) - 38;
            else points -= (9 * i) - 158;
        }
        this.xpLevels = i - 1;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        final int screenWidth = 176;
        final int screenHeight = 174;
        final int originX = this.width / 2;
        final int originY = this.height / 2;

        RenderSystem.setShaderTexture(0, GRAVE_VIEW_TEXTURE);
        drawTexture(matrices, originX - screenWidth / 2, originY - screenHeight / 2, 0, 0, screenWidth, screenHeight);

        this.hoveredStack = null;

        if (client != null) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = data.inventory.get(i);
                int slotX = originX - screenWidth / 2 + 8 + i * 18;
                int slotY = originY - screenHeight / 2 + 150;

                renderSlot(matrices, stack, slotX, slotY, mouseX, mouseY);
            }
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 9; x++) {
                    int slot = y * 9 + x + 9;
                    ItemStack stack = data.inventory.get(slot);
                    int slotX = originX - screenWidth / 2 + 8 + x * 18;
                    int slotY = originY - screenHeight / 2 + 128 - y * 18;

                    renderSlot(matrices, stack, slotX, slotY, mouseX, mouseY);
                }
            }
            for (int i = 36; i < 40; i++) {
                ItemStack stack = data.inventory.get(i);
                int slotX = originX - screenWidth / 2 + 8;
                int slotY = originY - screenHeight / 2 + 70 - (i - 36) * 18;

                renderSlot(matrices, stack, slotX, slotY, mouseX, mouseY);
            }

            int slotX = originX - screenWidth / 2 + 77;
            int slotY = originY - screenHeight / 2 + 70;
            renderSlot(matrices, data.inventory.get(40), slotX, slotY, mouseX, mouseY);


            if (hoveredStack != null && !hoveredStack.isEmpty() && client.player != null) {
                List<Text> tooltip = getTooltipFromItem(hoveredStack);
                renderTooltip(matrices, tooltip, mouseX, mouseY);
            }
        }

        String string = "death.attack." + data.deathSource.name;
        Text deathMsg = new TranslatableText(string, data.graveOwner.getName());

        super.render(matrices, mouseX, mouseY, delta);

        float textX = originX - screenWidth / 2f + 80f;
        textRenderer.draw(matrices, deathMsg, originX - screenWidth / 2f + 7f, originY - screenHeight / 2f + 5f, 0xAA0000);
        textRenderer.draw(matrices, "Dim: " + data.dimensionName, textX, originY - screenHeight / 2f + 16, 0x0055c4);
        textRenderer.draw(matrices, data.gravePos.getX() + " " + data.gravePos.getY() + " " + data.gravePos.getZ(), textX, originY - screenHeight / 2f + 28f, 0xBB00BB);
        if (data.modInventories.size() > 0 || data.inventory.size() > 41) {
            textRenderer.draw(matrices, modItemSize + " items in mod", textX, originY - screenHeight / 2f + 40f, 0x555555);
            textRenderer.draw(matrices, "inventories", textX, originY - screenHeight / 2f + 49f, 0x555555);
        }
        textRenderer.draw(matrices, "Levels: " + this.xpLevels, textX + 18f, originY - screenHeight / 2f + 77f, 0x299608);

        if (client != null && client.player != null) {
            int playerX = originX - screenWidth / 2 + 50;
            PlayerEntity otherPlayer = client.player.world.getPlayerByUuid(data.graveOwner.getId());
            LivingEntity renderedEntity;
            if (otherPlayer != null) {
                renderedEntity = otherPlayer;
            } else {
                renderedEntity = client.player;
            }
            int playerY = originY - screenHeight / 2 + 51;
            InventoryScreen.drawEntity(playerX, playerY + 30, 30, (float) playerX - mouseX, (float) (playerY - 20) - mouseY, renderedEntity);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (client != null && client.options.keyInventory.matchesKey(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (previousScreen == null) {
                this.onClose();
                return true;
            }
            if (client != null) {
                client.setScreen(previousScreen);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderSlot(MatrixStack matrices, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        if (mouseX > x && mouseX < x + 16 && mouseY > y && mouseY < y + 16) {
            fill(matrices, x, y, x + 16, y + 16, 0xFFBBBB);
            this.hoveredStack = stack.copy();
        }

        itemRenderer.renderGuiItemIcon(stack, x, y);
        itemRenderer.renderGuiItemOverlay(textRenderer, stack, x, y);
    }
}
