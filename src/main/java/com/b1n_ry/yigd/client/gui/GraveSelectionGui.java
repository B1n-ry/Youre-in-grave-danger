package com.b1n_ry.yigd.client.gui;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.client.gui.widget.WCardButton;
import com.b1n_ry.yigd.client.gui.widget.WFilterableListPanel;
import com.b1n_ry.yigd.client.gui.widget.WHoverToggleButton;
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

        WLabel title = new WLabel(Text.translatable("yigd.gui.text.graves_of", profile.getName()));
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
            wCardButton.setTooltipText(List.of(
                    Text.translatable("yigd.gui.text.grave_location", gravePos.getX(), gravePos.getY(), gravePos.getZ()),
                    Text.translatable("yigd.gui.text.grave_dim", lightGraveData.registryKey().getValue()),
                    Text.translatable("yigd.gui.text.item_count", lightGraveData.itemCount()),
                    Text.translatable("yigd.gui.text.level_count", lightGraveData.xpPoints())
            ));
            wCardButton.setOnClick(() -> ClientPacketHandler.sendGraveOverviewRequest(lightGraveData.id()));
        });

        root.add(listPanel, 0, 1, 12, 6);

        return listPanel;
    }
    private void addFilterButtons(WGridPanel root, WFilterableListPanel<LightGraveData, WCardButton> filterableList) {
        TextureIcon viewClaimedOn = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/claimed_grave.png"));
        TextureIcon viewClaimedOff = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/claimed_grave_cross.png"));
        WHoverToggleButton viewClaimed = new WHoverToggleButton(
                viewClaimedOn,
                Text.translatable("yigd.gui.text.btn.viewing_claimed"),
                viewClaimedOff,
                Text.translatable("yigd.gui.text.btn.hiding_claimed"));
        TextureIcon viewUnclaimedOn = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/unclaimed_grave.png"));
        TextureIcon viewUnclaimedOff = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/unclaimed_grave_cross.png"));
        WHoverToggleButton viewUnclaimed = new WHoverToggleButton(
                viewUnclaimedOn,
                Text.translatable("yigd.gui.text.btn.viewing_unclaimed"),
                viewUnclaimedOff,
                Text.translatable("yigd.gui.text.btn.hiding_unclaimed"));
        TextureIcon viewDestroyedOn = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/destroyed_grave.png"));
        TextureIcon viewDestroyedOff = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/destroyed_grave_cross.png"));
        WHoverToggleButton viewDestroyed = new WHoverToggleButton(
                viewDestroyedOn,
                Text.translatable("yigd.gui.text.btn.viewing_destroyed"),
                viewDestroyedOff,
                Text.translatable("yigd.gui.text.btn.hiding_destroyed"));
        TextureIcon showStatusOn = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/show_status.png"));
        TextureIcon showStatusOff = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/hide_status.png"));
        WHoverToggleButton showStatus = new WHoverToggleButton(
                showStatusOn,
                Text.translatable("yigd.gui.text.btn.showing_status"),
                showStatusOff,
                Text.translatable("yigd.gui.text.btn.hiding_status"));

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

    public Screen getPreviousScreen() {
        return this.previousScreen;
    }
}
