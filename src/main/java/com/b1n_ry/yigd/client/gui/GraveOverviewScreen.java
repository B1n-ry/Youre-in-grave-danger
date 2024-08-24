package com.b1n_ry.yigd.client.gui;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.networking.packets.*;
import com.b1n_ry.yigd.util.DropRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class GraveOverviewScreen extends Screen {
    private static final ResourceLocation WINDOW_BG = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "window_bg");
    private static final ResourceLocation SLOT = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "slot");
    private static final ResourceLocation EXP_ORB = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "exp_orb");
    private static final ResourceLocation GRAVE = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "unclaimed_grave");
    private static final ResourceLocation GRAVE_CROSSED = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "unclaimed_grave_cross");
    private static final ResourceLocation TRASH_CAN = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "trashcan_icon");
    private static final ResourceLocation TRASH_CAN_CROSSED = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "trashcan_icon_cross");
    private static final ResourceLocation BOOK_CROSSED = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "enchanted_book_cross");
    private static final ResourceLocation DROP_ICON = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "drop_icon");
    private static final ResourceLocation DROP_ICON_CROSSED = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "drop_icon_cross");
    private static final ResourceLocation RESTORE_ICON = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "restore_btn");
    private static final ResourceLocation ROB_ICON = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "rob_btn");
    private static final ResourceLocation LOCKED_ICON = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "locked_btn");
    private static final ResourceLocation UNLOCKED_ICON = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "unlocked_btn");

    private final GraveComponent graveComponent;
    // These are hard coded since otherwise the layout has to change, and that's kinda annoying
    private static final int MAIN_SIZE = 36;
    private static final int ARMOR_SIZE = 4;

    private static final int SCREEN_WIDTH = 178;
    private static final int SCREEN_HEIGHT = 178;

    private boolean viewGraveItems = true;
    private boolean viewDeletedItems = false;
    private boolean viewSoulboundItems = false;
    private boolean viewDroppedItems = false;
    private final Screen previousScreen;

    private final ItemStack[] mainInv = Stream.generate(() -> ItemStack.EMPTY).limit(MAIN_SIZE).toArray(ItemStack[]::new);
    private final ItemStack[] armor = Stream.generate(() -> ItemStack.EMPTY).limit(ARMOR_SIZE).toArray(ItemStack[]::new);
    private ItemStack offhand = ItemStack.EMPTY;
    private final NonNullList<ItemStack> extraItems = NonNullList.create();

    private final Button toggleGraveItems = Button.builder(Component.empty(), button -> {
        this.viewGraveItems = !this.viewGraveItems;
        button.setTooltip(Tooltip.create(this.viewGraveItems ?
                Component.translatable("button.yigd.gui.view_grave_items") :
                Component.translatable("button.yigd.gui.hide_grave_items")));
        this.reloadFilters();
    }).size(20, 20).tooltip(Tooltip.create(this.viewGraveItems ?
            Component.translatable("button.yigd.gui.view_grave_items") :
            Component.translatable("button.yigd.gui.hide_grave_items"))).build();
    private final Button toggleDeletedItems = Button.builder(Component.empty(), button -> {
        this.viewDeletedItems = !this.viewDeletedItems;
        button.setTooltip(Tooltip.create(this.viewDeletedItems ?
                Component.translatable("button.yigd.gui.view_deleted_items") :
                Component.translatable("button.yigd.gui.hide_deleted_items")));
        this.reloadFilters();
    }).size(20, 20).tooltip(Tooltip.create(this.viewDeletedItems ?
            Component.translatable("button.yigd.gui.view_deleted_items") :
            Component.translatable("button.yigd.gui.hide_deleted_items"))).build();
    private final Button toggleDroppedItems = Button.builder(Component.empty(), button -> {
        this.viewDroppedItems = !this.viewDroppedItems;
        button.setTooltip(Tooltip.create(this.viewDroppedItems ?
                Component.translatable("button.yigd.gui.view_dropped_items") :
                Component.translatable("button.yigd.gui.hide_dropped_items")));
        this.reloadFilters();
    }).size(20, 20).tooltip(Tooltip.create(this.viewDroppedItems ?
            Component.translatable("button.yigd.gui.view_dropped_items") :
            Component.translatable("button.yigd.gui.hide_dropped_items"))).build();
    private final Button toggleKeptItems = Button.builder(Component.empty(), button -> {
        this.viewSoulboundItems = !this.viewSoulboundItems;
        button.setTooltip(Tooltip.create(this.viewSoulboundItems ?
                Component.translatable("button.yigd.gui.view_soulbound_items") :
                Component.translatable("button.yigd.gui.hide_soulbound_items")));
        this.reloadFilters();
    }).size(20, 20).tooltip(Tooltip.create(this.viewSoulboundItems ?
            Component.translatable("button.yigd.gui.view_soulbound_items") :
            Component.translatable("button.yigd.gui.hide_soulbound_items"))).build();

    private final Map<String, Button> permissionLockedButtons = new HashMap<>();
    private final String[] buttonOrder = { "restore", "rob", "toggle_lock", "delete", "get_key", "get_compass" };

    private static final Font FONT = Minecraft.getInstance().font;

    public GraveOverviewScreen(GraveComponent graveComponent, Screen previousScreen, boolean canRestore, boolean canRob,
                               boolean canDelete, boolean canUnlock, boolean obtainableKeys, boolean obtainableCompass) {
        super(graveComponent.getDeathMessage());

        this.graveComponent = graveComponent;
        this.previousScreen = previousScreen;

        Component lockedText = this.graveComponent.isLocked() ? Component.translatable("button.yigd.gui.locked") : Component.translatable("button.yigd.gui.unlocked");

        if (canRestore) this.permissionLockedButtons.put("restore", Button.builder(Component.empty(), button -> PacketDistributor.sendToServer(
                new RestoreGraveC2SPacket(this.graveComponent.getGraveId(), this.viewGraveItems,
                        this.viewDeletedItems, this.viewSoulboundItems, this.viewDroppedItems)
        )).size(20, 20).tooltip(Tooltip.create(Component.translatable("button.yigd.gui.restore"))).build());
        if (canRob) this.permissionLockedButtons.put("rob", Button.builder(Component.empty(), button -> PacketDistributor.sendToServer(
                new RobGraveC2SPacket(this.graveComponent.getGraveId(), this.viewGraveItems,
                        this.viewDeletedItems, this.viewSoulboundItems, this.viewDroppedItems)
        )).size(20, 20).tooltip(Tooltip.create(Component.translatable("button.yigd.gui.rob"))).build());
        if (canUnlock) this.permissionLockedButtons.put("toggle_lock", Button.builder(Component.empty(), button -> {
            this.graveComponent.setLocked(!this.graveComponent.isLocked());
            boolean locked = this.graveComponent.isLocked();
            Component lockedComponent = this.graveComponent.isLocked() ? Component.translatable("button.yigd.gui.locked") : Component.translatable("button.yigd.gui.unlocked");
            button.setTooltip(Tooltip.create(lockedComponent));
            PacketDistributor.sendToServer(new LockGraveC2SPacket(this.graveComponent.getGraveId(), locked));
        }).size(20, 20).tooltip(Tooltip.create(lockedText)).build());
        if (canDelete) this.permissionLockedButtons.put("delete", Button.builder(Component.empty(), button -> PacketDistributor.sendToServer(
                new DeleteGraveC2SPacket(this.graveComponent.getGraveId())
        )).size(20, 20).tooltip(Tooltip.create(Component.translatable("button.yigd.gui.delete"))).build());
        if (obtainableKeys) this.permissionLockedButtons.put("get_key", Button.builder(Component.empty(), button -> PacketDistributor.sendToServer(
                new RequestKeyC2SPacket(this.graveComponent.getGraveId())
        )).size(20, 20).tooltip(Tooltip.create(Component.translatable("button.yigd.gui.obtain_keys"))).build());
        if (obtainableCompass) this.permissionLockedButtons.put("get_compass", Button.builder(Component.empty(), button -> PacketDistributor.sendToServer(
                new RequestCompassC2SPacket(this.graveComponent.getGraveId())
        )).size(20, 20).tooltip(Tooltip.create(Component.translatable("button.yigd.gui.obtain_compass"))).build());
    }

    @Override
    public void init() {
        this.reloadFilters();

        this.clearWidgets();
        this.addWidget(this.toggleGraveItems);
        this.addWidget(this.toggleDeletedItems);
        this.addWidget(this.toggleKeptItems);
        this.addWidget(this.toggleDroppedItems);
        for (Button b : this.permissionLockedButtons.values()) {
            this.addWidget(b);
        }

        super.init();
    }

    private void reloadFilters() {
        InventoryComponent visibleInventoryComponent = this.graveComponent.getInventoryComponent().filteredInv(dropRule -> switch (dropRule) {
            case PUT_IN_GRAVE -> this.viewGraveItems;
            case DESTROY -> this.viewDeletedItems;
            case KEEP -> this.viewSoulboundItems;
            case DROP -> this.viewDroppedItems;
        });

        this.extraItems.clear();
        NonNullList<Tuple<ItemStack, DropRule>> items = visibleInventoryComponent.getItems();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i).getA();
            if (i < MAIN_SIZE) {  // Main size on screen
                this.mainInv[i] = stack;
            } else if (i < visibleInventoryComponent.mainSize) {  // Main size in inventory but can't fit on screen
                this.extraItems.add(stack);
            } else if (i - visibleInventoryComponent.mainSize < ARMOR_SIZE) {  // First 4 in armor
                this.armor[i - visibleInventoryComponent.mainSize] = stack;
            } else if (i - visibleInventoryComponent.mainSize < visibleInventoryComponent.armorSize) {  // Still armor but can't fit
                this.extraItems.add(stack);
            } else if (i - visibleInventoryComponent.mainSize - visibleInventoryComponent.armorSize == 0) {  // First after armor (offhand)
                this.offhand = stack;
            } else if (!stack.isEmpty()) {  // Everything that is after the offhand (and not empty)
                this.extraItems.add(stack);
            }
        }
        this.extraItems.addAll(visibleInventoryComponent.getAllExtraItems(true));
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
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int leftEdge = this.width / 2 - SCREEN_WIDTH / 2;
        int topEdge = this.height / 2 - SCREEN_HEIGHT / 2;
        graphics.blitSprite(WINDOW_BG, leftEdge, topEdge, SCREEN_WIDTH, SCREEN_HEIGHT);

        this.renderHotbar(graphics, mouseX, mouseY);
        this.renderMainInventory(graphics, mouseX, mouseY);
        this.renderArmor(graphics, mouseX, mouseY);
        this.renderOffhand(graphics, mouseX, mouseY);

        this.renderGraveInfo(graphics);
        this.renderExtraItems(graphics, mouseX, mouseY);
        this.renderButtons(graphics, mouseX, mouseY, partialTick);
    }

    private void renderItemSlot(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, ItemStack stack) {
        graphics.blitSprite(SLOT, x, y, 18, 18);
        graphics.renderItem(stack, x + 1, y + 1);
        String itemCount = String.valueOf(stack.getCount());
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400.0D);
        if (stack.getCount() > 1) graphics.drawString(FONT, itemCount, x + 18 - FONT.width(itemCount), y + 19 - FONT.lineHeight, 0xFFFFFF, true);

        if (x <= mouseX && mouseX < x + 18 && y <= mouseY && mouseY < y + 18) {
            if (!stack.isEmpty()) {
                graphics.renderTooltip(FONT, stack, mouseX, mouseY);
            }
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x55FFFFFF);
        }
        graphics.pose().popPose();
    }

    private void renderHotbar(GuiGraphics graphics, int mouseX, int mouseY) {
        int leftEdge = this.width / 2 - SCREEN_WIDTH / 2;
        int topEdge = this.height / 2 - SCREEN_HEIGHT / 2;

        for (int i = 0; i < 9; i++) {
            this.renderItemSlot(graphics, mouseX, mouseY, leftEdge + 8 + i * 18, topEdge + 152, this.mainInv[i]);
        }
    }
    private void renderMainInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        int leftEdge = this.width / 2 - SCREEN_WIDTH / 2;
        int topEdge = this.height / 2 - SCREEN_HEIGHT / 2;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.renderItemSlot(graphics, mouseX, mouseY, leftEdge + 8 + j * 18, topEdge + 89 + i * 18, this.mainInv[9 + i * 9 + j]);
            }
        }
    }
    private void renderArmor(GuiGraphics graphics, int mouseX, int mouseY) {
        int leftEdge = this.width / 2 - SCREEN_WIDTH / 2;
        int topEdge = this.height / 2 - SCREEN_HEIGHT / 2;

        for (int i = 0; i < 4; i++) {
            this.renderItemSlot(graphics, mouseX, mouseY, leftEdge + 8, topEdge + 62 - i * 18, this.armor[i]);
        }
    }
    private void renderOffhand(GuiGraphics graphics, int mouseX, int mouseY) {
        int leftEdge = this.width / 2 - SCREEN_WIDTH / 2;
        int topEdge = this.height / 2 - SCREEN_HEIGHT / 2;

        this.renderItemSlot(graphics, mouseX, mouseY, leftEdge + 152, topEdge + 62, this.offhand);
    }

    private void renderGraveInfo(GuiGraphics graphics) {
        int leftEdge = this.width / 2 - SCREEN_WIDTH / 2;
        int topEdge = this.height / 2 - SCREEN_HEIGHT / 2;

        BlockPos pos = this.graveComponent.getPos();
        String dimId = this.graveComponent.getWorldRegistryKey().location().toString();
        Component number = Component.nullToEmpty(String.valueOf(this.graveComponent.getExpComponent().getXpLevel()));

        graphics.drawString(FONT, this.title, leftEdge + 28, topEdge + 8, 0x404040, false);
        graphics.drawString(FONT, Component.nullToEmpty("X: %d / Y: %d / Z: %d".formatted(pos.getX(), pos.getY(), pos.getZ())), leftEdge + 28, topEdge + 26, 0x404040, false);
        graphics.drawString(FONT, Component.translatableWithFallback("text.yigd.dimension.name." + dimId, dimId), leftEdge + 28, topEdge + 44, 0x404040, false);
        graphics.blitSprite(EXP_ORB, leftEdge + 28, topEdge + 62, 12, 12);
        graphics.drawString(FONT, number, leftEdge + 39, topEdge + 68, 0x000000, false);
        graphics.drawString(FONT, number, leftEdge + 40, topEdge + 67, 0x000000, false);
        graphics.drawString(FONT, number, leftEdge + 41, topEdge + 68, 0x000000, false);
        graphics.drawString(FONT, number, leftEdge + 40, topEdge + 69, 0x000000, false);
        graphics.drawString(FONT, number, leftEdge + 40, topEdge + 68, 0x80FF20, false);
    }

    public void renderExtraItems(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.extraItems.isEmpty()) return;
        int leftEdge = this.width / 2 - SCREEN_WIDTH / 2;
        int topEdge = this.height / 2 - SCREEN_HEIGHT / 2;

        int itemCount = this.extraItems.size();

        int slotsWide = ((itemCount - 1) / 9) + 1;
        int slotsTall = Math.min(itemCount, 9);
        graphics.blitSprite(WINDOW_BG, leftEdge - slotsWide * 18 - 14, topEdge + 1, 14 + slotsWide * 18, slotsTall * 18 + 14);

        for (int i = 0; i < itemCount; i++) {
            this.renderItemSlot(graphics, mouseX, mouseY, leftEdge - 7 - (slotsWide - i / 9) * 18, topEdge + 8 + (i % 9) * 18, this.extraItems.get(i));
        }
    }

    public void renderButtons(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int rightEdge = this.width / 2 + SCREEN_WIDTH / 2;
        int topEdge = this.height / 2 - SCREEN_HEIGHT / 2;

        int width = this.permissionLockedButtons.isEmpty() ? 34 : 58;
        int height = Math.max(106, this.permissionLockedButtons.size() * 24 + 10);

        graphics.blitSprite(WINDOW_BG, rightEdge, topEdge + 1, width, height);
        this.toggleGraveItems.setPosition(rightEdge + 7, topEdge + 8);
        this.toggleGraveItems.render(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(this.viewGraveItems ? GRAVE : GRAVE_CROSSED, rightEdge + 9, topEdge + 10, 16, 16);
        this.toggleDeletedItems.setPosition(rightEdge + 7, topEdge + 32);
        this.toggleDeletedItems.render(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(this.viewDeletedItems ? TRASH_CAN : TRASH_CAN_CROSSED, rightEdge + 9, topEdge + 34, 16, 16);
        this.toggleKeptItems.setPosition(rightEdge + 7, topEdge + 56);
        this.toggleKeptItems.render(graphics, mouseX, mouseY, partialTick);
        if (this.viewSoulboundItems)
            graphics.renderItem(Items.ENCHANTED_BOOK.getDefaultInstance(), rightEdge + 9, topEdge + 58);
        else
            graphics.blitSprite(BOOK_CROSSED, rightEdge + 9, topEdge + 58, 16, 16);
        this.toggleDroppedItems.setPosition(rightEdge + 7, topEdge + 80);
        this.toggleDroppedItems.render(graphics, mouseX, mouseY, partialTick);
        graphics.blitSprite(this.viewDroppedItems ? DROP_ICON : DROP_ICON_CROSSED, rightEdge + 9, topEdge + 82, 16, 16);

        for (int i = 0; i < this.buttonOrder.length; i++) {
            String buttonName = this.buttonOrder[i];
            if (!this.permissionLockedButtons.containsKey(buttonName))
                continue;

            Button button = this.permissionLockedButtons.get(buttonName);

            button.setPosition(rightEdge + 31, topEdge + 8 + 24 * i);
            button.render(graphics, mouseX, mouseY, partialTick);
            ResourceLocation sprite = switch (buttonName) {
                case "restore" -> RESTORE_ICON;
                case "rob" -> ROB_ICON;
                case "toggle_lock" -> this.graveComponent.isLocked() ? LOCKED_ICON : UNLOCKED_ICON;
                case "delete" -> TRASH_CAN;
                default -> null;
            };
            if (sprite == null) {
                if (buttonName.equals("get_key")) {
                    graphics.renderItem(Yigd.GRAVE_KEY_ITEM.toStack(), rightEdge + 33, topEdge + 10 + 24 * i);
                } else if (buttonName.equals("get_compass")) {
                    graphics.renderItem(Items.RECOVERY_COMPASS.getDefaultInstance(), rightEdge + 33, topEdge + 10 + 24 * i);
                }
            } else {
                graphics.blitSprite(sprite, rightEdge + 33, topEdge + 10 + 24 * i, 16, 16);
            }
        }
    }

    public static void openScreen(GraveOverviewS2CPacket payload) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.setScreen(new GraveOverviewScreen(payload.component(), client.screen,
                payload.canRestore(), payload.canRob(), payload.canDelete(), payload.canUnlock(),
                payload.obtainableKeys(), payload.obtainableCompass())));
    }
}
