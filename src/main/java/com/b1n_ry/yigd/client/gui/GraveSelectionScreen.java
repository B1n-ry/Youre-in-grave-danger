package com.b1n_ry.yigd.client.gui;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.components.ExpComponent;
import com.b1n_ry.yigd.networking.LightGraveData;
import com.b1n_ry.yigd.networking.packets.GraveOverviewRequestC2SPacket;
import com.b1n_ry.yigd.networking.packets.GraveSelectionS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class GraveSelectionScreen extends Screen {
    private static final ResourceLocation WINDOW_BG = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "window_bg");
    private static final ResourceLocation SLOT = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "slot");
    private static final ResourceLocation SCROLL_BAR = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "scroll_bar");
    private static final ResourceLocation SCROLL_BAR_PRESSED = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "scroll_bar_pressed");

    private static final ResourceLocation CLAIMED_GRAVE = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "claimed_grave");
    private static final ResourceLocation CLAIMED_GRAVE_CROSS = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "claimed_grave_cross");
    private static final ResourceLocation DESTROYED_GRAVE = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "destroyed_grave");
    private static final ResourceLocation DESTROYED_GRAVE_CROSS = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "destroyed_grave_cross");
    private static final ResourceLocation UNCLAIMED_GRAVE = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "unclaimed_grave");
    private static final ResourceLocation UNCLAIMED_GRAVE_CROSS = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "unclaimed_grave_cross");
    private static final ResourceLocation SHOW_STATUS = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "show_status");
    private static final ResourceLocation HIDE_STATUS = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "hide_status");

    private static final int SCREEN_WIDTH = 248;
    private static final int SCREEN_HEIGHT = 164;
    private static final int SCROLL_MENU_HEIGHT = 128;

    private final List<LightGraveData> data;
    private final Screen previousScreen;

    private ImageWidget scrollBar = ImageWidget.sprite(6, SCROLL_MENU_HEIGHT, SCROLL_BAR);
    private final List<Button> buttons = new ArrayList<>();
    private final List<Integer> overlayColorList = new ArrayList<>();
    private double scrollDistance;
    private int scrollContentHeight;

    private boolean scrolling = false;

    private boolean showClaimed = false;
    private boolean showDestroyed = true;
    private boolean showUnclaimed = true;
    private boolean overlayColors = false;

    private final Button claimedToggle = Button.builder(Component.empty(), button -> {
        this.showClaimed = !this.showClaimed;
        this.reloadButtons();
    }).size(20, 20).build();
    private final Button destroyedToggle = Button.builder(Component.empty(), button -> {
        this.showDestroyed = !this.showDestroyed;
        this.reloadButtons();
    }).size(20, 20).build();
    private final Button unclaimedToggle = Button.builder(Component.empty(), button -> {
        this.showUnclaimed = !this.showUnclaimed;
        this.reloadButtons();
    }).size(20, 20).build();
    private final Button overlayToggle = Button.builder(Component.empty(), button -> {
        this.overlayColors = !this.overlayColors;
        this.reloadButtons();
    }).size(20, 20).build();

    private static final Font FONT = Minecraft.getInstance().font;

    public GraveSelectionScreen(List<LightGraveData> data, ResolvableProfile profile, Screen previousScreen) {
        super(Component.translatable("text.yigd.gui.graves_of", profile.name().orElse("PLAYER_NOT_FOUND")));

        this.data = data;
        this.previousScreen = previousScreen;
    }

    @Override
    public void init() {
        this.reloadButtons();
        super.init();
    }

    private void reloadButtons() {
        this.scrollDistance = 0.0D;
        this.clearWidgets();
        this.buttons.clear();
        this.overlayColorList.clear();
        for (LightGraveData graveData : this.data) {
            switch (graveData.status()) {
                case CLAIMED -> { if (!this.showClaimed) continue; }
                case DESTROYED -> { if (!this.showDestroyed) continue; }
                case UNCLAIMED -> { if (!this.showUnclaimed) continue; }
            }
            Button button = Button.builder(graveData.deathMessage(), btn -> PacketDistributor.sendToServer(new GraveOverviewRequestC2SPacket(graveData.id())))
                    .size(200, 20)
                    .build();

            BlockPos gravePos = graveData.pos();
            String dimensionName = graveData.registryKey().location().toString();
            button.setTooltip(Tooltip.create(
                    Component.translatable("text.yigd.gui.grave_location", gravePos.getX(), gravePos.getY(), gravePos.getZ())
                            .append("\n")
                            .append(Component.translatableWithFallback("text.yigd.dimension.name." + dimensionName, dimensionName))
                            .append("\n")
                            .append(Component.translatable("text.yigd.gui.item_count", graveData.itemCount()))
                            .append("\n")
                            .append(Component.translatable("text.yigd.gui.level_count", ExpComponent.xpToLevels(graveData.xpPoints())))));

            this.overlayColorList.add(graveData.status().getTransparentColor());

            this.buttons.add(button);

            this.addWidget(button);
        }
        this.scrollContentHeight = this.buttons.size() * 20;

        float fraction = SCROLL_MENU_HEIGHT / (float) this.scrollContentHeight;
        this.scrollBar.setHeight(Math.max(4, (int) (Math.min(1f, fraction) * SCROLL_MENU_HEIGHT)));

        this.addWidget(this.claimedToggle);
        this.claimedToggle.setTooltip(Tooltip.create(Component.translatable(this.showClaimed ? "button.yigd.gui.viewing_claimed" : "button.yigd.gui.hiding_claimed")));
        this.addWidget(this.destroyedToggle);
        this.destroyedToggle.setTooltip(Tooltip.create(Component.translatable(this.showDestroyed ? "button.yigd.gui.viewing_destroyed" : "button.yigd.gui.hiding_destroyed")));
        this.addWidget(this.unclaimedToggle);
        this.unclaimedToggle.setTooltip(Tooltip.create(Component.translatable(this.showUnclaimed ? "button.yigd.gui.viewing_unclaimed" : "button.yigd.gui.hiding_unclaimed")));
        this.addWidget(this.overlayToggle);
        this.overlayToggle.setTooltip(Tooltip.create(Component.translatable(this.overlayColors ? "button.yigd.gui.showing_status" : "button.yigd.gui.hiding_status")));
    }

    private void setScrollDistance(double scrollDistance) {
        this.scrollDistance = Mth.clamp(scrollDistance, 0.0, this.getMaxScrollAmount());
    }
    private int getMaxScrollAmount() {
        return Math.max(0, this.scrollContentHeight - SCROLL_MENU_HEIGHT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.isMouseOver(mouseX, mouseY)) return false;
        this.setScrollDistance(this.scrollDistance - scrollY * 9.0D);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.scrollBar.isMouseOver(mouseX, mouseY)) {
            this.scrollBar = ImageWidget.sprite(this.scrollBar.getWidth(), this.scrollBar.getHeight(), SCROLL_BAR_PRESSED);
            this.scrolling = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.scrolling) {
            this.scrollBar = ImageWidget.sprite(this.scrollBar.getWidth(), this.scrollBar.getHeight(), SCROLL_BAR);
        }
        this.scrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling) {
            int scrollBarHeight = this.scrollBar.getHeight();
            int holderTop = this.height / 2 - SCREEN_HEIGHT / 2 + 8;
            if (mouseY < holderTop) {
                this.setScrollDistance(0.0D);
            } else if (mouseY > this.scrollBar.getY() + SCROLL_MENU_HEIGHT) {
                this.setScrollDistance(this.getMaxScrollAmount());
            } else {
                float barMenuRatio = this.getMaxScrollAmount() / (float) (SCROLL_MENU_HEIGHT - scrollBarHeight);
                this.setScrollDistance(this.scrollDistance + dragY * barMenuRatio);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && this.minecraft != null) {
            this.minecraft.setScreen(this.previousScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int leftEdge = this.width / 2 - SCREEN_WIDTH / 2;
        int topEdge = this.height / 2 - SCREEN_HEIGHT / 2;
        graphics.blitSprite(WINDOW_BG, leftEdge, topEdge, SCREEN_WIDTH, SCREEN_HEIGHT);

        graphics.drawString(
                FONT,
                this.title,
                leftEdge + 8,
                topEdge + 8,
                0x404040,
                false);

        this.renderScrollMenu(graphics, mouseX, mouseY, partialTick, leftEdge + 8, topEdge + 20);

        this.renderToggleButtons(graphics, mouseX, mouseY, partialTick, leftEdge + 220, topEdge + 20);
    }

    private void renderScrollMenu(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y) {
        graphics.blitSprite(SLOT, x, y, 202, SCROLL_MENU_HEIGHT + 2);
        graphics.blitSprite(SLOT, x + 202, y, 8, SCROLL_MENU_HEIGHT + 2);

        int movedScrollBar = (int) ((this.scrollDistance / this.getMaxScrollAmount()) * (SCROLL_MENU_HEIGHT - this.scrollBar.getHeight()));
        this.scrollBar.setPosition(x + 203, y + 1 + movedScrollBar);
        this.scrollBar.render(graphics, mouseX, mouseY, partialTick);
        graphics.enableScissor(0, y + 1, this.width, y + SCROLL_MENU_HEIGHT + 1);
        for (int i = 0; i < this.buttons.size(); i++) {
            Button button = this.buttons.get(i);
            button.setPosition(x + 1, y + 1 + i * 20 - (int) this.scrollDistance);
            button.render(graphics, mouseX, mouseY, partialTick);

            if (this.overlayColors) {
                graphics.fill(button.getX(), button.getY(), button.getX() + button.getWidth(), button.getY() + button.getHeight(), this.overlayColorList.get(i));
            }
        }
        graphics.disableScissor();
    }

    private void renderToggleButtons(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int x, int y) {
        this.claimedToggle.setPosition(x, y);
        this.claimedToggle.render(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(this.showClaimed ? CLAIMED_GRAVE : CLAIMED_GRAVE_CROSS, x + 2, y + 2, 16, 16);

        this.unclaimedToggle.setPosition(x, y + 24);
        this.unclaimedToggle.render(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(this.showUnclaimed ? UNCLAIMED_GRAVE : UNCLAIMED_GRAVE_CROSS, x + 2, y + 26, 16, 16);

        this.destroyedToggle.setPosition(x, y + 48);
        this.destroyedToggle.render(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(this.showDestroyed ? DESTROYED_GRAVE : DESTROYED_GRAVE_CROSS, x + 2, y + 50, 16, 16);

        this.overlayToggle.setPosition(x, y + 72);
        this.overlayToggle.render(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(this.overlayColors ? SHOW_STATUS : HIDE_STATUS, x + 2, y + 74, 16, 16);
    }

    public static void openScreen(GraveSelectionS2CPacket payload) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.setScreen(new GraveSelectionScreen(payload.data(), payload.profile(), client.screen)));
    }
}
