package com.b1n_ry.yigd.client.gui;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.PacketIdentifiers;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class GraveSelectScreen extends Screen {
    private final Identifier GRAVE_SELECT_TEXTURE = new Identifier("yigd", "textures/gui/select_grave_menu.png");
    private final Identifier SELECT_ELEMENT_TEXTURE = new Identifier("yigd", "textures/gui/select_elements.png");

    private final Map<UUID, GraveGuiInfo> graveInfo; // ID for grave and info
    private final int page;

    private final Screen previousScreen;

    private final GameProfile graveOwner;

    private boolean mouseIsClicked = false;
    private String hoveredElement = null;

    private boolean showClaimed;
    private boolean showDeleted;
    private boolean showPlaced;
    private boolean showStatus;

    private final YigdConfig.GuiTextColors textColors;

    private final List<UUID> filteredGraveIds;

    public GraveSelectScreen(GameProfile owner, Map<UUID, GraveGuiInfo> info, int page, Screen previousScreen) {
        this(owner, info, page, previousScreen, true, false, false, false);
    }
    public GraveSelectScreen(GameProfile owner, Map<UUID, GraveGuiInfo> info, int page, Screen previousScreen, boolean showPlaced, boolean showClaimed, boolean showDeleted, boolean showStatus) {
        super(Text.translatable("text.yigd.gui.grave_select.title"));

        this.graveOwner = owner;
        this.graveInfo = info;
        this.filteredGraveIds = new ArrayList<>(info.keySet());

        this.page = page;
        this.previousScreen = previousScreen;

        this.showPlaced = showPlaced;
        this.showClaimed = showClaimed;
        this.showDeleted = showDeleted;
        this.showStatus = showStatus;

        reloadFilters();

        this.textColors = YigdConfig.getConfig().graveSettings.graveRenderSettings.guiTextColors;
    }

    private void reloadFilters() {
        this.filteredGraveIds.clear();
        for (Map.Entry<UUID, GraveGuiInfo> entry : this.graveInfo.entrySet()) {
            UUID uuid = entry.getKey();

            GraveGuiInfo info = entry.getValue();
            switch (info.availability) {
                case -1 -> {
                    if (this.showDeleted) this.filteredGraveIds.add(uuid);
                }
                case 0 -> {
                    if (this.showClaimed) this.filteredGraveIds.add(uuid);
                }
                case 1 -> {
                    if (this.showPlaced) this.filteredGraveIds.add(uuid);
                }
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (client != null && client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (previousScreen == null) {
                this.close();
                return true;
            }
            if (client != null) {
                client.setScreen(previousScreen);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredElement != null) {
            mouseIsClicked = true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mouseIsClicked = false;
        if (button == 0 && hoveredElement != null && client != null) {
            if (hoveredElement.equals("left") && page > 1) {
                GraveSelectScreen screen = new GraveSelectScreen(this.graveOwner, this.graveInfo, this.page - 1, this.previousScreen, this.showPlaced, this.showClaimed, this.showDeleted, this.showStatus);
                client.setScreen(screen);
            } else if (hoveredElement.equals("right") && filteredGraveIds.size() > page * 4) {
                GraveSelectScreen screen = new GraveSelectScreen(this.graveOwner, this.graveInfo, this.page + 1, this.previousScreen, this.showPlaced, this.showClaimed, this.showDeleted, this.showStatus);
                client.setScreen(screen);
            } else if (hoveredElement.equals("show_available")) {
                this.showPlaced = !this.showPlaced;
            } else if (hoveredElement.equals("show_claimed")) {
                this.showClaimed = !this.showClaimed;
            } else if (hoveredElement.equals("show_destroyed")) {
                this.showDeleted = !this.showDeleted;
            } else if (hoveredElement.equals("show_status")) {
                this.showStatus = !this.showStatus;

            } else if (isUuid(hoveredElement)) {
                UUID parsedUuid = UUID.fromString(hoveredElement);

                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeGameProfile(this.graveOwner);
                buf.writeUuid(parsedUuid);

                ClientPlayNetworking.send(PacketIdentifiers.SINGLE_GRAVE_GUI, buf);
            }

            if (hoveredElement.startsWith("show_")) {
                reloadFilters();
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        final int screenWidth = 220;
        final int screenHeight = 219;
        final int originX = this.width / 2;
        final int originY = this.height / 2;

        final int screenLeft = originX - screenWidth / 2;
        final int screenTop = originY - screenHeight / 2;

        hoveredElement = null;

        RenderSystem.setShaderTexture(0, GRAVE_SELECT_TEXTURE);
        drawTexture(matrices, screenLeft, screenTop, 0, 0, screenWidth, screenHeight);

        RenderSystem.setShaderTexture(0, SELECT_ELEMENT_TEXTURE);
        if (mouseX > screenLeft + 6 && mouseX < screenLeft + 14 && mouseY > originY - 8 && mouseY < originY + 7) {
            hoveredElement = "left";
        } else if (mouseX > screenLeft + screenWidth - 14 && mouseX < screenLeft + screenWidth - 6 && mouseY > originY - 8 && mouseY < originY + 7) {
            hoveredElement = "right";
        }
        if (hoveredElement != null && hoveredElement.equals("left") && mouseIsClicked) {
            drawTexture(matrices, screenLeft + 6, originY - 8, 16, 84, 8, 15);
        } else {
            drawTexture(matrices, screenLeft + 6, originY - 8, 0, 84, 8, 15);
        }
        if (hoveredElement != null && hoveredElement.equals("right") && mouseIsClicked) {
            drawTexture(matrices, screenLeft + screenWidth - 14, originY - 8, 24, 84, 8, 15);
        } else {
            drawTexture(matrices, screenLeft + screenWidth - 14, originY - 8, 8, 84, 8, 15);
        }

        int infoSize = this.filteredGraveIds.size();
        int startValue = infoSize - (page - 1) * 4;
        int whileMoreThan = Math.max(startValue - 4, 0);
        int iterations = 0;
        for (int i = startValue; i > whileMoreThan; i--) {
            UUID graveId = this.filteredGraveIds.get(i - 1);
            GraveGuiInfo info = this.graveInfo.get(graveId);

            if (this.showStatus && info.availability != 1) {
                if (info.availability == -1) RenderSystem.setShaderColor(1f, 0, 0, 0.5f);
                if (info.availability == 0) RenderSystem.setShaderColor(1f, 1f, 0, 0.5f);
            }
            RenderSystem.setShaderTexture(0, SELECT_ELEMENT_TEXTURE);
            int left = screenLeft + 19;
            int top = screenTop + 43 + 42 * iterations;
            int width = screenWidth - 19 * 2;
            int height = 42;

            if (mouseX > left && mouseX < left + width && mouseY > top && mouseY < top + height) {
                hoveredElement = graveId.toString();
            }
            if (isUuid(hoveredElement) && UUID.fromString(hoveredElement).equals(graveId) && mouseIsClicked) {
                drawTexture(matrices, left, top, 0, height, width, height);
            } else {
                drawTexture(matrices, left, top, 0, 0, width, height);
            }

            String dimName;
            if (GraveViewScreen.dimensionNameOverrides.containsKey(info.dimension)) {
                dimName = GraveViewScreen.dimensionNameOverrides.get(info.dimension);
            } else {
                dimName = info.dimension;
            }

            textRenderer.draw(matrices, info.pos.getX() + " " + info.pos.getY() + " " + info.pos.getZ() + " " + dimName, left + 5f, top + 5f, this.textColors.graveSelectGraveLocation);
            textRenderer.draw(matrices, Text.translatable("text.yigd.gui.grave_select.x_items", info.itemCount), left + 5f, top + 17f, this.textColors.graveSelectItemSize);
            textRenderer.draw(matrices, Text.translatable("text.yigd.gui.grave_select.x_levels", info.levelCount), left + 5f, top + 29f, this.textColors.graveSelectLevelSize);
            iterations++;
        }

        super.render(matrices, mouseX, mouseY, delta);

        renderCheckButtons(matrices, mouseX, mouseY, screenTop, screenLeft, originX);

        int firstElement = (page - 1) * 4 + 1;
        String gravesDisplayed = firstElement + "-" + (firstElement + Math.min(3, infoSize - firstElement)) + "/" + infoSize;
        textRenderer.draw(matrices, Text.translatable("text.yigd.gui.grave_select.graves_of_user", graveOwner.getName()), screenLeft + 19f, screenTop + 10f, this.textColors.graveSelectTitle);

        int offset = textRenderer.getWidth(gravesDisplayed);
        textRenderer.draw(matrices, gravesDisplayed, screenLeft + screenWidth - 19f - offset, screenTop + 10f, this.textColors.graveSelectPageView);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean isUuid(String uuidString) {
        if (uuidString == null) return false;
        try {
            UUID.fromString(uuidString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void renderCheckButtons(MatrixStack matrices, int mouseX, int mouseY, int screenTop, int screenLeft, int originX) {
        int boxTop = screenTop + 22;
        int boxRow = boxTop + 9;

        // Rendering checkbox for showing available graves
        RenderSystem.setShaderTexture(0, SELECT_ELEMENT_TEXTURE);
        int leftEdge = screenLeft + 18;
        if (mouseX > leftEdge && mouseX < leftEdge + 6 && mouseY > boxTop && mouseY < boxTop + 6) {
            hoveredElement = "show_available";
        }
        if (hoveredElement != null && hoveredElement.equals("show_available") && mouseIsClicked) {
            drawTexture(matrices, leftEdge, boxTop, 32, 90, 6, 6);
        } else {
            drawTexture(matrices, leftEdge, boxTop, 32, 84, 6, 6);
        }
        if (this.showPlaced) drawTexture(matrices, leftEdge, boxTop, 38, 84, 6, 6);
        textRenderer.draw(matrices, Text.translatable("text.yigd.gui.grave_select.show_available"), leftEdge + 8f, boxTop - 1f, this.textColors.graveSelectShowAvailableCheckbox);

        // Show Claimed
        RenderSystem.setShaderTexture(0, SELECT_ELEMENT_TEXTURE);
        if (mouseX > originX && mouseX < originX + 6 && mouseY > boxTop && mouseY < boxTop + 6) {
            hoveredElement = "show_claimed";
        }
        if (hoveredElement != null && hoveredElement.equals("show_claimed") && mouseIsClicked) {
            drawTexture(matrices, originX, boxTop, 32, 90, 6, 6);
        } else {
            drawTexture(matrices, originX, boxTop, 32, 84, 6, 6);
        }
        if (this.showClaimed) drawTexture(matrices, originX, boxTop, 38, 84, 6, 6);
        textRenderer.draw(matrices, Text.translatable("text.yigd.gui.grave_select.show_claimed"), originX + 8f, boxTop - 1f, this.textColors.graveSelectShowClaimedCheckbox);

        // Show Destroyed
        RenderSystem.setShaderTexture(0, SELECT_ELEMENT_TEXTURE);
        if (mouseX > leftEdge && mouseX < leftEdge + 6 && mouseY > boxRow && mouseY < boxRow + 6) {
            hoveredElement = "show_destroyed";
        }
        if (hoveredElement != null && hoveredElement.equals("show_destroyed") && mouseIsClicked) {
            drawTexture(matrices, leftEdge, boxRow, 32, 90, 6, 6);
        } else {
            drawTexture(matrices, leftEdge, boxRow, 32, 84, 6, 6);
        }
        if (this.showDeleted) drawTexture(matrices, leftEdge, boxRow, 38, 84, 6, 6);
        textRenderer.draw(matrices, Text.translatable("text.yigd.gui.grave_select.show_destroyed"), leftEdge + 8f, boxRow - 1f, this.textColors.graveSelectShowDestroyedCheckbox);

        // Show Status
        RenderSystem.setShaderTexture(0, SELECT_ELEMENT_TEXTURE);
        if (mouseX > originX && mouseX < originX + 6 && mouseY > boxRow && mouseY < boxRow + 6) {
            hoveredElement = "show_status";
        }
        if (hoveredElement != null && hoveredElement.equals("show_status") && mouseIsClicked) {
            drawTexture(matrices, originX, boxRow, 32, 90, 6, 6);
        } else {
            drawTexture(matrices, originX, boxRow, 32, 84, 6, 6);
        }
        if (this.showStatus) drawTexture(matrices, originX, boxRow, 38, 84, 6, 6);
        textRenderer.draw(matrices, Text.translatable("text.yigd.gui.grave_select.show_status"), originX + 8f, boxRow - 1f, this.textColors.graveSelectShowStatusCheckbox);
    }

    public record GraveGuiInfo(BlockPos pos, String dimension, int itemCount, int levelCount, byte availability) {}
}