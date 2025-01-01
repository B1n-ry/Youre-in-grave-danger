package com.b1n_ry.yigd.client.gui;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.client.gui.widget.WCardButton;
import com.b1n_ry.yigd.client.gui.widget.WFilterableListPanel;
import com.b1n_ry.yigd.client.gui.widget.WHoverToggleButton;
import com.b1n_ry.yigd.components.ExpComponent;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.networking.ClientPacketHandler;
import com.b1n_ry.yigd.networking.LightGraveData;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import io.github.cottonmc.cotton.gui.widget.icon.ItemIcon;
import io.github.cottonmc.cotton.gui.widget.icon.TextureIcon;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.List;

public class GraveSelectionGui extends LightweightGuiDescription {
    private final List<LightGraveData> data;
    private final Screen previousScreen;
    public GraveSelectionGui(List<LightGraveData> data, ResolvableProfile profile, Screen previousScreen) {
        this.data = data;
        this.previousScreen = previousScreen;

        WGridPanel root = new WGridPanel();
        this.setRootPanel(root);
        root.setInsets(Insets.ROOT_PANEL);
        root.setGaps(2, 5);

        WLabel title = new WLabel(Component.translatable("text.yigd.gui.graves_of", profile.name().orElse("PLAYER_NOT_FOUND")));
        root.add(title, 0, 0);

        WFilterableListPanel<LightGraveData, WCardButton> listPanel = this.addGraveList(root);
        this.addFilterButtons(root, listPanel);

        root.validate(this);
    }

    private WFilterableListPanel<LightGraveData, WCardButton> addGraveList(WGridPanel root) {
        ItemIcon icon = new ItemIcon(Yigd.GRAVE_BLOCK.asItem());
        WFilterableListPanel<LightGraveData, WCardButton> listPanel = new WFilterableListPanel<>(this.data,
                () -> new WCardButton(icon), (lightGraveData, wCardButton) -> {
            wCardButton.setCardText(lightGraveData.deathMessage());
            wCardButton.setOverlayColor(lightGraveData.status().getTransparentColor());

            BlockPos gravePos = lightGraveData.pos();
            String dimensionName = lightGraveData.registryKey().registry().toString();
            wCardButton.setTooltipText(List.of(
                    Component.translatable("text.yigd.gui.grave_location", gravePos.getX(), gravePos.getY(), gravePos.getZ()),
                    Component.translatableWithFallback("text.yigd.dimension.name." + dimensionName, dimensionName),
                    Component.translatable("text.yigd.gui.item_count", lightGraveData.itemCount()),
                    Component.translatable("text.yigd.gui.level_count", ExpComponent.xpToLevels(lightGraveData.xpPoints()))
            ));
            wCardButton.setOnClick(() -> ClientPacketHandler.sendGraveOverviewRequest(lightGraveData.id()));
        });

        root.add(listPanel, 0, 1, 12, 6);

        return listPanel;
    }
    private void addFilterButtons(WGridPanel root, WFilterableListPanel<LightGraveData, WCardButton> filterableList) {
        WHoverToggleButton viewClaimed = this.addToggleButton(
                ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "textures/gui/claimed_grave.png"),
                "button.yigd.gui.viewing_claimed",
                ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "textures/gui/claimed_grave_cross.png"),
                "button.yigd.gui.hiding_claimed");
        WHoverToggleButton viewUnclaimed = this.addToggleButton(
                ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "textures/gui/unclaimed_grave.png"),
                "button.yigd.gui.viewing_unclaimed",
                ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "textures/gui/unclaimed_grave_cross.png"),
                "button.yigd.gui.hiding_unclaimed");
        WHoverToggleButton viewDestroyed = this.addToggleButton(
                ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "textures/gui/destroyed_grave.png"),
                "button.yigd.gui.viewing_destroyed",
                ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "textures/gui/destroyed_grave_cross.png"),
                "button.yigd.gui.hiding_destroyed");
        WHoverToggleButton showStatus = this.addToggleButton(
                ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "textures/gui/show_status.png"),
                "button.yigd.gui.showing_status",
                ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "textures/gui/hide_status.png"),
                "button.yigd.gui.hiding_status");

        // a == b && bool <=> a == b if bool, else a != b
        viewClaimed.setOnToggle(aBoolean -> {
            filterableList.setFilter("claimed", graveData -> graveData.status() == GraveStatus.CLAIMED && aBoolean);
            /*listPanel.reload();*/
        });
        viewUnclaimed.setOnToggle(aBoolean -> {
            filterableList.setFilter("unclaimed", graveData -> graveData.status() == GraveStatus.UNCLAIMED && aBoolean);
            /*listPanel.reload();*/
        });
        viewDestroyed.setOnToggle(aBoolean -> {
            filterableList.setFilter("destroyed", graveData -> graveData.status() == GraveStatus.DESTROYED && aBoolean);
            /*listPanel.reload();*/
        });

        showStatus.setOnToggle(aBoolean -> {
            for (WCardButton card : filterableList.getWidgets()) {
                card.setColoredRendering(aBoolean);
            }
        });

        viewClaimed.setToggle(false);
        viewUnclaimed.setToggle(true);
        viewDestroyed.setToggle(false);
        showStatus.setToggle(false);

        root.add(viewClaimed, 12, 1);
        root.add(viewUnclaimed, 12, 2);
        root.add(viewDestroyed, 12, 3);
        root.add(showStatus, 12, 4);
    }
    private WHoverToggleButton addToggleButton(ResourceLocation stateOnImg, String stateOnTranslationKey,
                                               ResourceLocation stateOffImg, String stateOffTranslationKey) {
        TextureIcon onIcon = new TextureIcon(stateOnImg);
        TextureIcon offIcon = new TextureIcon(stateOffImg);

        Component stateOnText = Component.translatable(stateOnTranslationKey);
        Component stateOffText = Component.translatable(stateOffTranslationKey);

        return new WHoverToggleButton(onIcon, stateOnText, offIcon, stateOffText);
    }

    public Screen getPreviousScreen() {
        return this.previousScreen;
    }
}
