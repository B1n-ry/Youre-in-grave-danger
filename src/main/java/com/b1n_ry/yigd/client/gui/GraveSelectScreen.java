package com.b1n_ry.yigd.client.gui;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GraveSelectScreen extends Screen {
    private final Identifier GRAVE_SELECT_TEXTURE = new Identifier("yigd", "textures/gui/select_grave_menu.png");
    private final Identifier SELECT_ELEMENT_TEXTURE = new Identifier("yigd", "textures/gui/select_elements.png");

    private List<DeadPlayerData> data;
    private List<GuiGraveInfo> graveInfo;
    private final int page;

    private final Screen previousScreen;

    private final GameProfile graveOwner;

    private boolean mouseIsClicked = false;
    private String hoveredElement = null;

    private boolean showClaimed;
    private boolean showDeleted;
    private boolean showPlaced;
    private boolean showStatus;

    private final List<GuiGraveInfo> filteredGraves = new ArrayList<>();

    public GraveSelectScreen(List<DeadPlayerData> data, int page, Screen previousScreen) {
        this(data, page, previousScreen, true, false, false, false);
    }
    public GraveSelectScreen(List<DeadPlayerData> data, int page, Screen previousScreen, boolean showPlaced, boolean showClaimed, boolean showDeleted, boolean showStatus) {
        super(MutableText.of(new TranslatableTextContent("text.yigd.gui.grave_select.title")));
        List<GuiGraveInfo> info = new ArrayList<>();
        for (DeadPlayerData deadData : data) {
            int size = 0;
            for (ItemStack stack : deadData.inventory) {
                if (!stack.isEmpty()) size++;
            }
            for (int i = 0; i < deadData.modInventories.size(); i++) {
                YigdApi yigdApi = Yigd.apiMods.get(i);
                size += yigdApi.getInventorySize(deadData.modInventories.get(i));
            }

            int points = deadData.xp;
            int i;
            for (i = 0; points >= 0; i++) {
                if (i < 16) points -= (2 * i) + 7;
                else if (i < 31) points -= (5 * i) - 38;
                else points -= (9 * i) - 158;
            }

            info.add(new GuiGraveInfo(deadData, size, i - 1));
        }

        this.data = data;
        this.graveInfo = info;
        this.page = page;
        this.previousScreen = previousScreen;

        this.showPlaced = showPlaced;
        this.showClaimed = showClaimed;
        this.showDeleted = showDeleted;
        this.showStatus = showStatus;

        if (data.size() > 0) {
            this.graveOwner = data.get(0).graveOwner;
        } else {
            this.graveOwner = null;
        }

        reloadFilters();
    }

    private void reloadFilters() {
        this.filteredGraves.clear();
        for (GuiGraveInfo data : this.graveInfo) {
            switch (data.data.availability) {
                case -1 -> {
                    if (this.showDeleted) this.filteredGraves.add(data);
                }
                case 0 -> {
                    if (this.showClaimed) this.filteredGraves.add(data);
                }
                case 1 -> {
                    if (this.showPlaced) this.filteredGraves.add(data);
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
                GraveSelectScreen screen = new GraveSelectScreen(data, page - 1, this.previousScreen);
                client.setScreen(screen);
            } else if (hoveredElement.equals("right") && filteredGraves.size() > page * 4) {
                GraveSelectScreen screen = new GraveSelectScreen(data, page + 1, this.previousScreen);
                client.setScreen(screen);
            } else if (hoveredElement.equals("show_available")) {
                this.showPlaced = !this.showPlaced;
            } else if (hoveredElement.equals("show_claimed")) {
                this.showClaimed = !this.showClaimed;
            } else if (hoveredElement.equals("show_destroyed")) {
                this.showDeleted = !this.showDeleted;
            } else if (hoveredElement.equals("show_status")) {
                this.showStatus = !this.showStatus;

            } else if (isInt(hoveredElement)) {
                int parsedString = Integer.parseInt(hoveredElement) - 1;
                if (filteredGraves.size() > parsedString && parsedString >= 0) {
                    GraveViewScreen screen = new GraveViewScreen(filteredGraves.get(parsedString).data, this);
                    client.setScreen(screen);
                }
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

        int infoSize = this.filteredGraves.size();
        int startValue = infoSize - (page - 1) * 4;
        int whileMoreThan = Math.max(startValue - 4, 0);
        int iterations = 0;
        for (int i = startValue; i > whileMoreThan; i--) {
            GuiGraveInfo info = this.filteredGraves.get(i - 1);

            if (this.showStatus && info.data.availability != 1) {
                if (info.data.availability == -1) RenderSystem.setShaderColor(1f, 0, 0, 0.5f);
                if (info.data.availability == 0) RenderSystem.setShaderColor(1f, 1f, 0, 0.5f);
            }
            RenderSystem.setShaderTexture(0, SELECT_ELEMENT_TEXTURE);
            int left = screenLeft + 19;
            int top = screenTop + 43 + 42 * iterations;
            int width = screenWidth - 19 * 2;
            int height = 42;

            if (mouseX > left && mouseX < left + width && mouseY > top && mouseY < top + height) {
                hoveredElement = "" + i;
            }
            if (isInt(hoveredElement) && Integer.parseInt(hoveredElement) == i && mouseIsClicked) {
                drawTexture(matrices, left, top, 0, height, width, height);
            } else {
                drawTexture(matrices, left, top, 0, 0, width, height);
            }

            textRenderer.draw(matrices, info.data.gravePos.getX() + " " + info.data.gravePos.getY() + " " + info.data.gravePos.getZ() + " " + info.data.dimensionName, left + 5f, top + 5f, 0xCC00CC);
            textRenderer.draw(matrices, MutableText.of(new TranslatableTextContent("text.yigd.gui.grave_select.x_items", info.itemSize)), left + 5f, top + 17f, 0x0000CC);
            textRenderer.draw(matrices, MutableText.of(new TranslatableTextContent("text.yigd.gui.grave_select.x_levels", info.xpLevels)), left + 5f, top + 29f, 0x299608);
            iterations++;
        }

        super.render(matrices, mouseX, mouseY, delta);

        renderCheckButtons(matrices, mouseX, mouseY, screenTop, screenLeft, originX);

        int firstElement = (page - 1) * 4 + 1;
        String gravesDisplayed = firstElement + "-" + (firstElement + Math.min(3, infoSize - firstElement)) + "/" + infoSize;
        textRenderer.draw(matrices, MutableText.of(new TranslatableTextContent("text.yigd.gui.grave_select.graves_of_user", graveOwner.getName())), screenLeft + 19f, screenTop + 10f, 0x555555);

        int offset = textRenderer.getWidth(gravesDisplayed);
        textRenderer.draw(matrices, gravesDisplayed, screenLeft + screenWidth - 19f - offset, screenTop + 10f, 0x007700);
    }

    private boolean isInt(String intString) {
        if (intString == null) return false;
        try {
            Integer.parseInt(intString);
            return true;
        } catch (NumberFormatException e) {
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
        textRenderer.draw(matrices, MutableText.of(new TranslatableTextContent("text.yigd.gui.grave_select.show_available")), leftEdge + 8f, boxTop - 1f, 0x777777);

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
        textRenderer.draw(matrices, MutableText.of(new TranslatableTextContent("text.yigd.gui.grave_select.show_claimed")), originX + 8f, boxTop - 1f, 0x777777);

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
        textRenderer.draw(matrices, MutableText.of(new TranslatableTextContent("text.yigd.gui.grave_select.show_destroyed")), leftEdge + 8f, boxRow - 1f, 0x777777);

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
        textRenderer.draw(matrices, MutableText.of(new TranslatableTextContent("text.yigd.gui.grave_select.show_status")), originX + 8f, boxRow - 1f, 0x777777);
    }

    public void addData(UUID userId, DeadPlayerData data) {
        if (!userId.equals(this.graveOwner.getId())) return;

        // Looks complicated but for some reason the list is fixed size, so this is required
        List<DeadPlayerData> deadPlayerData = new ArrayList<>(this.data);
        deadPlayerData.add(data);
        this.data = deadPlayerData;

        int size = 0;
        for (ItemStack stack : data.inventory) {
            if (!stack.isEmpty()) size++;
        }
        for (int i = 0; i < data.modInventories.size(); i++) {
            YigdApi yigdApi = Yigd.apiMods.get(i);
            size += yigdApi.getInventorySize(data.modInventories.get(i));
        }

        int points = data.xp;
        int i;
        for (i = 0; points >= 0; i++) {
            if (i < 16) points -= (2 * i) + 7;
            else if (i < 31) points -= (5 * i) - 38;
            else points -= (9 * i) - 158;
        }

        // Looks complicated but for some reason the list is fixed size, so this is required
        List<GuiGraveInfo> guiGraveInfoList = new ArrayList<>(this.graveInfo);
        guiGraveInfoList.add(new GuiGraveInfo(data, size, i - 1));
        this.graveInfo = guiGraveInfoList;

        reloadFilters();
    }

    private record GuiGraveInfo(DeadPlayerData data, int itemSize, int xpLevels) { }
}