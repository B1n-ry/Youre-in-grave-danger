package com.b1n_ry.yigd.client.gui;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.client.gui.widget.WCardButton;
import com.b1n_ry.yigd.client.gui.widget.WFilterableListPanel;
import com.b1n_ry.yigd.client.gui.widget.WHoverToggleButton;
import com.b1n_ry.yigd.components.ExpComponent;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.packets.ClientPacketHandler;
import com.b1n_ry.yigd.packets.LightGraveData;
import com.mojang.authlib.GameProfile;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import io.github.cottonmc.cotton.gui.widget.icon.ItemIcon;
import io.github.cottonmc.cotton.gui.widget.icon.TextureIcon;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class GraveSelectionGui extends LightweightGuiDescription {
    private final List<LightGraveData> data;
    private final Screen previousScreen;
    public GraveSelectionGui(List<LightGraveData> data, GameProfile profile, Screen previousScreen) {
        this.data = data;
        this.previousScreen = previousScreen;

        WGridPanel root = new WGridPanel();
        this.setRootPanel(root);
        root.setInsets(Insets.ROOT_PANEL);
        root.setGaps(2, 5);

        WLabel title = new WLabel(Text.translatable("text.yigd.gui.graves_of", profile.getName()));
        root.add(title, 0, 0);

        WFilterableListPanel<LightGraveData, WCardButton> listPanel = this.addGraveList(root);
        this.addFilterButtons(root, listPanel);

        root.validate(this);
    }

    private WFilterableListPanel<LightGraveData, WCardButton> addGraveList(WGridPanel root) {
        ItemIcon icon = new ItemIcon(Yigd.GRAVE_BLOCK.asItem());
        WFilterableListPanel<LightGraveData, WCardButton> listPanel = new WFilterableListPanel<>(this.data,
                () -> new WCardButton(icon), (lightGraveData, wCardButton) -> {
            wCardButton.setCardText(lightGraveData.deathMessage().getDeathMessage());
            wCardButton.setOverlayColor(lightGraveData.status().getTransparentColor());

            BlockPos gravePos = lightGraveData.pos();
            String dimensionName = lightGraveData.registryKey().getValue().toString();
            wCardButton.setTooltipText(List.of(
                    Text.translatable("text.yigd.gui.grave_location", gravePos.getX(), gravePos.getY(), gravePos.getZ()),
                    Text.translatableWithFallback("text.yigd.dimension.name." + dimensionName, dimensionName),
                    Text.translatable("text.yigd.gui.item_count", lightGraveData.itemCount()),
                    Text.translatable("text.yigd.gui.level_count", ExpComponent.xpToLevels(lightGraveData.xpPoints()))
            ));
            wCardButton.setOnClick(() -> ClientPacketHandler.sendGraveOverviewRequest(lightGraveData.id()));
        });

        root.add(listPanel, 0, 1, 12, 6);

        return listPanel;
    }
    private void addFilterButtons(WGridPanel root, WFilterableListPanel<LightGraveData, WCardButton> filterableList) {
        WHoverToggleButton viewClaimed = this.addToggleButton(
                new Identifier(Yigd.MOD_ID, "textures/gui/claimed_grave.png"),
                "button.yigd.gui.viewing_claimed",
                new Identifier(Yigd.MOD_ID, "textures/gui/claimed_grave_cross.png"),
                "button.yigd.gui.hiding_claimed");
        WHoverToggleButton viewUnclaimed = this.addToggleButton(
                new Identifier(Yigd.MOD_ID, "textures/gui/unclaimed_grave.png"),
                "button.yigd.gui.viewing_unclaimed",
                new Identifier(Yigd.MOD_ID, "textures/gui/unclaimed_grave_cross.png"),
                "button.yigd.gui.hiding_unclaimed");
        WHoverToggleButton viewDestroyed = this.addToggleButton(
                new Identifier(Yigd.MOD_ID, "textures/gui/destroyed_grave.png"),
                "button.yigd.gui.viewing_destroyed",
                new Identifier(Yigd.MOD_ID, "textures/gui/destroyed_grave_cross.png"),
                "button.yigd.gui.hiding_destroyed");
        WHoverToggleButton showStatus = this.addToggleButton(
                new Identifier(Yigd.MOD_ID, "textures/gui/show_status.png"),
                "button.yigd.gui.showing_status",
                new Identifier(Yigd.MOD_ID, "textures/gui/hide_status.png"),
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
    private WHoverToggleButton addToggleButton(Identifier stateOnImg, String stateOnTranslationKey,
                                               Identifier stateOffImg, String stateOffTranslationKey) {
        TextureIcon onIcon = new TextureIcon(stateOnImg);
        TextureIcon offIcon = new TextureIcon(stateOffImg);

        Text stateOnText = Text.translatable(stateOnTranslationKey);
        Text stateOffText = Text.translatable(stateOffTranslationKey);

        return new WHoverToggleButton(onIcon, stateOnText, offIcon, stateOffText);
    }

    public Screen getPreviousScreen() {
        return this.previousScreen;
    }
}
