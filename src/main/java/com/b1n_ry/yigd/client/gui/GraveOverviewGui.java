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

    private final Set<Runnable> removeItemFunctions = new HashSet<>();

    private boolean viewGraveItems = true;
    private boolean viewDeletedItems = false;
    private boolean viewSoulboundItems = false;
    private boolean viewDroppedItems = false;
    private final Screen previousScreen;

    private static final int SLOT_SIZE = 18;
    private static final int SLOTS_PER_LINE = 9;
    public GraveOverviewGui(GraveComponent graveComponent, Screen previousScreen, boolean canRestore, boolean canRob,
                            boolean canDelete, boolean canUnlock) {
        this.graveComponent = graveComponent;
        this.previousScreen = previousScreen;

        WPlainPanel root = new WPlainPanel();
        this.setRootPanel(root);
        root.setInsets(Insets.ROOT_PANEL);

        int slightlyRight = (int) (1.1 * SLOT_SIZE);

        WLabel title = new WLabel(this.graveComponent.getDeathMessage().getDeathMessage());
        root.add(title, slightlyRight, 0);

        this.addCoordinates(root, slightlyRight);
        this.addDimension(root, slightlyRight);
        this.addXpInfo(root, slightlyRight);

        this.addButtons(root, canRestore, canRob, canDelete, canUnlock);

        root.validate(this);

        this.updateFilters(root);  // Update visible inventory component
    }

    public Screen getPreviousScreen() {
        return this.previousScreen;
    }

    private void updateFilters(WPlainPanel root) {
        // Remove all wItemStacks
        for (Runnable runnable : this.removeItemFunctions) {
            runnable.run();
        }

        this.visibleInventoryComponent = this.graveComponent.getInventoryComponent().filteredInv(dropRule -> switch (dropRule) {
            case PUT_IN_GRAVE -> this.viewGraveItems;
            case DESTROY -> this.viewDeletedItems;
            case KEEP -> this.viewSoulboundItems;
            case DROP -> this.viewDroppedItems;
        });

        this.addItemSlots(root);
    }

    private void addItemSlots(WPlainPanel root) {
        DefaultedList<ItemStack> items = DefaultedList.of();
        for (Pair<ItemStack, DropRule> pair : this.visibleInventoryComponent.getItems()) {
            items.add(pair.getLeft());
        }

        items.addAll(this.visibleInventoryComponent.getAllExtraItems(true));

        int generateArmorAndOffhandFrom = Math.max(this.visibleInventoryComponent.armorSize, this.visibleInventoryComponent.offHandSize) - 1;

        // Armor slots
        this.addItemSlot(root, this.visibleInventoryComponent.mainSize, this.visibleInventoryComponent.armorSize,
                items, i -> new Point(0, (generateArmorAndOffhandFrom - i) * SLOT_SIZE));

        // Offhand slot(s?)
        this.addItemSlot(root, this.visibleInventoryComponent.mainSize + this.visibleInventoryComponent.armorSize,
                this.visibleInventoryComponent.offHandSize, items,
                i -> new Point((SLOTS_PER_LINE - 1) * SLOT_SIZE, (generateArmorAndOffhandFrom - i) * SLOT_SIZE));

        int mainInvHeight = this.visibleInventoryComponent.mainSize / SLOTS_PER_LINE;

        // Hot-bar slots
        this.addItemSlot(root, 0, SLOTS_PER_LINE, items,
                i -> new Point(i * SLOT_SIZE, (mainInvHeight + generateArmorAndOffhandFrom + 1) * SLOT_SIZE));

        // Main slots
        this.addItemSlot(root, SLOTS_PER_LINE, this.visibleInventoryComponent.mainSize - SLOTS_PER_LINE, items,
                i -> new Point((i % SLOTS_PER_LINE) * SLOT_SIZE,
                        (int) ((generateArmorAndOffhandFrom + 1.5 + (i / SLOTS_PER_LINE)) * SLOT_SIZE)));

        int collectiveSize = this.visibleInventoryComponent.mainSize + this.visibleInventoryComponent.armorSize + this.visibleInventoryComponent.offHandSize;
        int sizeDiff = items.size() - collectiveSize;
        if (sizeDiff > 0) {
            WPlainPanel extraItemsPanel = new WPlainPanel();
            extraItemsPanel.setInsets(new Insets(6));
            extraItemsPanel.setBackgroundPainter(BackgroundPainter.VANILLA);

            Insets panelInsets = root.getInsets();
            int slotsHigh = (root.getHeight() - (panelInsets.bottom() + panelInsets.top())) / SLOT_SIZE;

            // If any extra, those slots go here (included modded inventories, non-empty slots)
            this.addItemSlot(extraItemsPanel, collectiveSize, sizeDiff, items, i -> new Point((i / slotsHigh) * SLOT_SIZE, i * SLOT_SIZE));

            int width = extraItemsPanel.getWidth();
            int height = extraItemsPanel.getHeight();
            root.add(extraItemsPanel, -(panelInsets.left() + extraItemsPanel.getWidth()), -extraItemsPanel.getInsets().top());
            extraItemsPanel.setSize(width, height);  // WPlainPanel#add automatically makes added elements 18x18 pixels

            this.removeItemFunctions.add(() -> root.remove(extraItemsPanel));  // Make sure entire left panel is reset
        }
    }

    private void addCoordinates(WPlainPanel root, int x) {
        BlockPos pos = this.graveComponent.getPos();
        WText coordinates = new WText(Text.of("X: %d / Y: %d / Z: %d".formatted(pos.getX(), pos.getY(), pos.getZ())));

        root.add(coordinates, x, GraveOverviewGui.SLOT_SIZE, SLOTS_PER_LINE * SLOT_SIZE - 2 * x, SLOT_SIZE);
    }

    private void addDimension(WPlainPanel root, int x) {
        RegistryKey<World> key = this.graveComponent.getWorldRegistryKey();
        String dimId = key.getValue().toString();
        WText dimension = new WText(Text.translatableWithFallback("text.yigd.dimension.name." + dimId, dimId));

        root.add(dimension, x, 36, SLOTS_PER_LINE * SLOT_SIZE - 2 * x, SLOT_SIZE);
    }

    private void addXpInfo(WPlainPanel root, int x) {
        WSprite xpIcon = new WSprite(new Identifier(Yigd.MOD_ID, "textures/gui/exp_orb.png"));
        int spriteSize = (int) (SLOT_SIZE * 0.7);
        root.add(xpIcon, x, 54, spriteSize, spriteSize);

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
        root.add(bgTextRight, textX + 1, textY);
        root.add(bgTextLeft, textX - 1, textY);
        root.add(bgTextUp, textX, textY + 1);
        root.add(bgTextDown, textX, textY - 1);
        root.add(wText, textX, textY);
    }

    private void addItemSlot(WPlainPanel root, int fromIndex, int amount, DefaultedList<ItemStack> items, Function<Integer, Point> posCalculation) {
        for (int i = 0; i < amount; i++) {
            Point pos = posCalculation.apply(i);

            WItemStack wItemStack = new WItemStack(items.get(fromIndex + i), SLOT_SIZE);

            this.removeItemFunctions.add(() -> root.remove(wItemStack));

            root.add(wItemStack, pos.x, pos.y);
        }
    }

    private void addButtons(WPlainPanel root, boolean restoreBtn, boolean robBtn, boolean deleteBtn, boolean lockingBtn) {
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

        UUID graveId = this.graveComponent.getGraveId();

        viewGraveItems.setToggle(this.viewGraveItems);
        viewDeletedItems.setToggle(this.viewDeletedItems);
        viewSoulboundItems.setToggle(this.viewSoulboundItems);
        viewDroppedItems.setToggle(this.viewDroppedItems);

        viewGraveItems.setOnToggle(aBoolean -> {
            this.viewGraveItems = aBoolean;
            this.updateFilters(root);
        });
        viewDeletedItems.setOnToggle(aBoolean -> {
            this.viewDeletedItems = aBoolean;
            this.updateFilters(root);
        });
        viewSoulboundItems.setOnToggle(aBoolean -> {
            this.viewSoulboundItems = aBoolean;
            this.updateFilters(root);
        });
        viewDroppedItems.setOnToggle(aBoolean -> {
            this.viewDroppedItems = aBoolean;
            this.updateFilters(root);
        });

        restoreButton.setOnClick(() -> ClientPacketHandler.sendRestoreGraveRequestPacket(graveId, this.viewGraveItems, this.viewDeletedItems, this.viewSoulboundItems, this.viewDroppedItems));
        robButton.setOnClick(() -> ClientPacketHandler.sendRobGraveRequestPacket(graveId, this.viewGraveItems, this.viewDeletedItems, this.viewSoulboundItems, this.viewDroppedItems));
        deleteButton.setOnClick(() -> ClientPacketHandler.sendDeleteGraveRequestPacket(graveId));
        lockingButton.setOnToggle(aBoolean -> ClientPacketHandler.sendGraveLockRequestPacket(graveId, aBoolean));

        final int x = 167;
        final int topY = 0;

        int i = 0;

        root.add(viewGraveItems, x, topY); i += 21;
        root.add(viewDeletedItems, x, topY + i); i += 21;
        root.add(viewSoulboundItems, x, topY + i); i += 21;
        root.add(viewDroppedItems, x, topY + i); i += 22;

        if (restoreBtn) {
            root.add(restoreButton, x, topY + i);
            i += 21;
        }
        if (robBtn) {
            root.add(robButton, x, topY + i);
            i += 21;
        }
        if (lockingBtn) {
            root.add(lockingButton, x, topY + i);
            i += 21;
        }
        if (deleteBtn) {
            root.add(deleteButton, x, topY + i);
        }
    }
}
