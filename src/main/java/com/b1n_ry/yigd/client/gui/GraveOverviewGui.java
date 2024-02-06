package com.b1n_ry.yigd.client.gui;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.client.gui.widget.WHoverButton;
import com.b1n_ry.yigd.client.gui.widget.WHoverToggleButton;
import com.b1n_ry.yigd.client.gui.widget.WItemStack;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.packets.ClientPacketHandler;
import com.b1n_ry.yigd.util.DropRule;
import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import io.github.cottonmc.cotton.gui.widget.icon.TextureIcon;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class GraveOverviewGui extends LightweightGuiDescription {
    private final GraveComponent graveComponent;
    private InventoryComponent visibleInventoryComponent;

    private final Set<WWidget> removables = new HashSet<>();

    private final WPlainPanel root;
    private final WPlainPanel invPanel;
    private final WPlainPanel buttonPanel;

    private boolean viewGraveItems = true;
    private boolean viewDeletedItems = false;
    private boolean viewSoulboundItems = false;
    private boolean viewDroppedItems = false;
    private final Screen previousScreen;

    private static final int SLOT_SIZE = 18;
    private static final int SLOTS_PER_LINE = 9;
    public GraveOverviewGui(GraveComponent graveComponent, Screen previousScreen, boolean canRestore, boolean canRob,
                            boolean canDelete, boolean canUnlock, boolean obtainableKeys, boolean obtainableCompass) {
        this.graveComponent = graveComponent;
        this.previousScreen = previousScreen;

        this.root = new WPlainPanel();
        this.setRootPanel(this.root);

        this.invPanel = new WPlainPanel();
        this.invPanel.setBackgroundPainter(BackgroundPainter.VANILLA);
        this.invPanel.setInsets(Insets.ROOT_PANEL);

        this.buttonPanel = new WPlainPanel();

        int width = this.invPanel.getWidth();
        int height = this.invPanel.getHeight();
        this.root.add(this.invPanel, 0, 0);
        this.invPanel.setSize(width, height);

        int slightlyRight = (int) (1.1 * SLOT_SIZE);

        WLabel title = new WLabel(this.graveComponent.getDeathMessage().getDeathMessage());
        this.invPanel.add(title, slightlyRight, 0);

        this.addCoordinates(slightlyRight);
        this.addDimension(slightlyRight);
        this.addXpInfo(slightlyRight);

        this.addButtons(canRestore, canRob, canDelete, canUnlock, obtainableKeys, obtainableCompass);

        this.updateFilters();  // Update visible inventory component

        this.root.validate(this);
    }

    @Override
    public void addPainters() {
        // Leave empty so no background is drawn on root panel
    }

    public Screen getPreviousScreen() {
        return this.previousScreen;
    }

    private void updateFilters() {
        // Remove all wItemStacks
        for (WWidget removable : this.removables) {
            if (removable.getParent() == null) continue;
            removable.getParent().remove(removable);
        }

        this.visibleInventoryComponent = this.graveComponent.getInventoryComponent().filteredInv(dropRule -> switch (dropRule) {
            case PUT_IN_GRAVE -> this.viewGraveItems;
            case DESTROY -> this.viewDeletedItems;
            case KEEP -> this.viewSoulboundItems;
            case DROP -> this.viewDroppedItems;
        });

        this.addItemSlots();
    }

    private void addItemSlots() {
        DefaultedList<ItemStack> items = DefaultedList.of();
        for (Pair<ItemStack, DropRule> pair : this.visibleInventoryComponent.getItems()) {
            items.add(pair.getLeft());
        }

        items.addAll(this.visibleInventoryComponent.getAllExtraItems(true));

        int generateArmorAndOffhandFrom = Math.max(this.visibleInventoryComponent.armorSize, this.visibleInventoryComponent.offHandSize) - 1;

        // Armor slots
        this.addItemSlot(this.invPanel, this.visibleInventoryComponent.mainSize, this.visibleInventoryComponent.armorSize,
                items, i -> new Point(0, (generateArmorAndOffhandFrom - i) * SLOT_SIZE));

        // Offhand slot(s?)
        this.addItemSlot(this.invPanel, this.visibleInventoryComponent.mainSize + this.visibleInventoryComponent.armorSize,
                this.visibleInventoryComponent.offHandSize, items,
                i -> new Point((SLOTS_PER_LINE - 1) * SLOT_SIZE, (generateArmorAndOffhandFrom - i) * SLOT_SIZE));

        int mainInvHeight = this.visibleInventoryComponent.mainSize / SLOTS_PER_LINE;

        // Hot-bar slots
        this.addItemSlot(this.invPanel, 0, SLOTS_PER_LINE, items,
                i -> new Point(i * SLOT_SIZE, (mainInvHeight + generateArmorAndOffhandFrom + 1) * SLOT_SIZE));

        // Main slots
        this.addItemSlot(this.invPanel, SLOTS_PER_LINE, this.visibleInventoryComponent.mainSize - SLOTS_PER_LINE, items,
                i -> new Point((i % SLOTS_PER_LINE) * SLOT_SIZE,
                        (int) ((generateArmorAndOffhandFrom + 1.5 + (i / SLOTS_PER_LINE)) * SLOT_SIZE)));

        int collectiveSize = this.visibleInventoryComponent.mainSize + this.visibleInventoryComponent.armorSize + this.visibleInventoryComponent.offHandSize;
        int sizeDiff = items.size() - collectiveSize;
        if (sizeDiff > 0) {
            WPlainPanel extraItemsPanel = new WPlainPanel();
            extraItemsPanel.setInsets(new Insets(6));
            extraItemsPanel.setBackgroundPainter(BackgroundPainter.VANILLA);

            Insets panelInsets = this.invPanel.getInsets();
            int slotsHigh = (this.invPanel.getHeight() - (panelInsets.bottom() + panelInsets.top())) / SLOT_SIZE;

            // If any extra, those slots go here (included modded inventories, non-empty slots)
            this.addItemSlot(extraItemsPanel, collectiveSize, sizeDiff, items, i -> new Point((i / slotsHigh) * SLOT_SIZE, i * SLOT_SIZE));

            int width = extraItemsPanel.getWidth();
            int height = extraItemsPanel.getHeight();
            this.root.add(extraItemsPanel, 0, panelInsets.top() - extraItemsPanel.getInsets().top());
            extraItemsPanel.setSize(width, height);  // WPlainPanel#add automatically makes added elements 18x18 pixels

            this.invPanel.setLocation(extraItemsPanel.getWidth(), 0);
            this.buttonPanel.setLocation(this.invPanel.getX() + this.invPanel.getWidth(), this.buttonPanel.getY());

            this.removables.add(extraItemsPanel);  // Make sure entire left panel is reset
        } else {
            this.invPanel.setLocation(0, 0);
            this.buttonPanel.setLocation(this.invPanel.getX() + this.invPanel.getWidth(), this.buttonPanel.getY());
        }
    }

    private void addCoordinates(int x) {
        BlockPos pos = this.graveComponent.getPos();
        WText coordinates = new WText(Text.of("X: %d / Y: %d / Z: %d".formatted(pos.getX(), pos.getY(), pos.getZ())));

        this.invPanel.add(coordinates, x, GraveOverviewGui.SLOT_SIZE, SLOTS_PER_LINE * SLOT_SIZE - 2 * x, SLOT_SIZE);
    }

    private void addDimension(int x) {
        RegistryKey<World> key = this.graveComponent.getWorldRegistryKey();
        String dimId = key.getValue().toString();
        WText dimension = new WText(Text.translatableWithFallback("text.yigd.dimension.name." + dimId, dimId));

        this.invPanel.add(dimension, x, 36, SLOTS_PER_LINE * SLOT_SIZE - 2 * x, SLOT_SIZE);
    }

    private void addXpInfo(int x) {
        WSprite xpIcon = new WSprite(new Identifier(Yigd.MOD_ID, "textures/gui/exp_orb.png"));
        int spriteSize = (int) (SLOT_SIZE * 0.7);
        this.invPanel.add(xpIcon, x, 54, spriteSize, spriteSize);

        int level = this.graveComponent.getExpComponent().getXpLevel();
        Text text = Text.of(String.valueOf(level));

        // Create outline 1 pixel to each side
        WText bgTextUp = new WText(text, 0x000000);
        WText bgTextDown = new WText(text, 0x000000);
        WText bgTextLeft = new WText(text, 0x000000);
        WText bgTextRight = new WText(text, 0x000000);

        // Text/number itself
        WText wText = new WText(text, 0x80FF20);

        int textX = x + spriteSize;
        int textY = 54 + spriteSize / 2;
        this.invPanel.add(bgTextRight, textX + 1, textY);
        this.invPanel.add(bgTextLeft, textX - 1, textY);
        this.invPanel.add(bgTextUp, textX, textY + 1);
        this.invPanel.add(bgTextDown, textX, textY - 1);
        this.invPanel.add(wText, textX, textY);
    }

    private void addItemSlot(WPlainPanel root, int fromIndex, int amount, DefaultedList<ItemStack> items, Function<Integer, Point> posCalculation) {
        for (int i = 0; i < amount; i++) {
            Point pos = posCalculation.apply(i);

            WItemStack wItemStack = new WItemStack(items.get(fromIndex + i), SLOT_SIZE);

            this.removables.add(wItemStack);

            root.add(wItemStack, pos.x, pos.y);
        }
    }

    private void addButtons(boolean restoreBtn, boolean robBtn, boolean deleteBtn, boolean lockingBtn,
                            boolean obtainableKeys, boolean obtainableCompass) {
        WHoverToggleButton viewGraveItems = new WHoverToggleButton(new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/unclaimed_grave.png")),
                Text.translatable("button.yigd.gui.view_grave_items"), new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/unclaimed_grave_cross.png")),
                Text.translatable("button.yigd.gui.hide_grave_items"));
        WHoverToggleButton viewDeletedItems = new WHoverToggleButton(new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/trashcan_icon.png")),
                Text.translatable("button.yigd.gui.view_deleted_items"), new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/trashcan_icon_cross.png")),
                Text.translatable("button.yigd.gui.hide_deleted_items"));
        WHoverToggleButton viewSoulboundItems = new WHoverToggleButton(new TextureIcon(new Identifier("textures/item/enchanted_book.png")),
                Text.translatable("button.yigd.gui.view_soulbound_items"), new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/enchanted_book_cross.png")),
                Text.translatable("button.yigd.gui.hide_soulbound_items"));
        WHoverToggleButton viewDroppedItems = new WHoverToggleButton(new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/drop_icon.png")),
                Text.translatable("button.yigd.gui.view_dropped_items"), new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/drop_icon_cross.png")),
                Text.translatable("button.yigd.gui.hide_dropped_items"));

        WHoverButton restoreButton = new WHoverButton(new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/restore_btn.png")), Text.translatable("button.yigd.gui.restore"));
        WHoverButton robButton = new WHoverButton(new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/rob_btn.png")), Text.translatable("button.yigd.gui.rob"));
        WHoverButton deleteButton = new WHoverButton(new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/trashcan_icon.png")), Text.translatable("button.yigd.gui.delete"));

        TextureIcon lockingIconOn = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/locked_btn.png"));
        TextureIcon lockingIconOff = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/unlocked_btn.png"));
        WHoverToggleButton lockingButton = new WHoverToggleButton(lockingIconOn, Text.translatable("button.yigd.gui.locked"), lockingIconOff, Text.translatable("button.yigd.gui.unlocked"));
        lockingButton.setToggle(this.graveComponent.isLocked());

        WHoverButton obtainKeysButton = new WHoverButton(new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/item/grave_key.png")), Text.translatable("button.yigd.gui.obtain_keys"));
        WHoverButton obtainCompassButton = new WHoverButton(new TextureIcon(new Identifier("textures/item/recovery_compass_18.png")), Text.translatable("button.yigd.gui.obtain_compass"));

        UUID graveId = this.graveComponent.getGraveId();

        viewGraveItems.setToggle(this.viewGraveItems);
        viewDeletedItems.setToggle(this.viewDeletedItems);
        viewSoulboundItems.setToggle(this.viewSoulboundItems);
        viewDroppedItems.setToggle(this.viewDroppedItems);

        viewGraveItems.setOnToggle(aBoolean -> {
            this.viewGraveItems = aBoolean;
            this.updateFilters();
        });
        viewDeletedItems.setOnToggle(aBoolean -> {
            this.viewDeletedItems = aBoolean;
            this.updateFilters();
        });
        viewSoulboundItems.setOnToggle(aBoolean -> {
            this.viewSoulboundItems = aBoolean;
            this.updateFilters();
        });
        viewDroppedItems.setOnToggle(aBoolean -> {
            this.viewDroppedItems = aBoolean;
            this.updateFilters();
        });

        restoreButton.setOnClick(() -> ClientPacketHandler.sendRestoreGraveRequestPacket(graveId, this.viewGraveItems, this.viewDeletedItems, this.viewSoulboundItems, this.viewDroppedItems));
        robButton.setOnClick(() -> ClientPacketHandler.sendRobGraveRequestPacket(graveId, this.viewGraveItems, this.viewDeletedItems, this.viewSoulboundItems, this.viewDroppedItems));
        deleteButton.setOnClick(() -> ClientPacketHandler.sendDeleteGraveRequestPacket(graveId));
        lockingButton.setOnToggle(aBoolean -> ClientPacketHandler.sendGraveLockRequestPacket(graveId, aBoolean));
        obtainKeysButton.setOnClick(() -> ClientPacketHandler.sendObtainKeysRequestPacket(graveId));
        obtainCompassButton.setOnClick(() -> ClientPacketHandler.sendObtainCompassRequestPacket(graveId));

        int x = 0;
        final int y = 0;

        int i = 0;

        this.buttonPanel.setBackgroundPainter(BackgroundPainter.VANILLA);
        this.buttonPanel.setInsets(new Insets(6));

        this.buttonPanel.add(viewGraveItems, x, y); i += 22;
        this.buttonPanel.add(viewDeletedItems, x, y + i); i += 22;
        this.buttonPanel.add(viewSoulboundItems, x, y + i); i += 22;
        this.buttonPanel.add(viewDroppedItems, x, y + i);

        i = 0;
        x += 22;

        if (restoreBtn) {
            this.buttonPanel.add(restoreButton, x, y);
            i += 22;
        }
        if (robBtn) {
            this.buttonPanel.add(robButton, x, y + i);
            i += 22;
        }
        if (lockingBtn) {
            this.buttonPanel.add(lockingButton, x, y + i);
            i += 22;
        }
        if (deleteBtn) {
            this.buttonPanel.add(deleteButton, x, y + i);
            i += 22;
        }
        if (obtainableKeys) {
            this.buttonPanel.add(obtainKeysButton, x, y + i);
            i += 22;
        }
        if (obtainableCompass) {
            this.buttonPanel.add(obtainCompassButton, x, y + i);
        }

        int width = this.buttonPanel.getWidth();
        int height = this.buttonPanel.getHeight();
        this.root.add(this.buttonPanel, this.invPanel.getX() + this.invPanel.getWidth(), this.invPanel.getInsets().top() - this.buttonPanel.getInsets().top());
        this.buttonPanel.setSize(width, height);
    }
}
