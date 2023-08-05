package com.b1n_ry.yigd.client.gui;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.client.gui.widget.WCardButton;
import com.b1n_ry.yigd.client.gui.widget.WFilterableListPanel;
import com.b1n_ry.yigd.client.gui.widget.WHoverToggleButton;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.packets.ClientPacketHandler;
import com.b1n_ry.yigd.packets.LightGraveData;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import io.github.cottonmc.cotton.gui.widget.icon.Icon;
import io.github.cottonmc.cotton.gui.widget.icon.ItemIcon;
import io.github.cottonmc.cotton.gui.widget.icon.TextureIcon;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class GraveSelectionGui extends LightweightGuiDescription {
    private List<LightGraveData> data;
    private int page;
    private final Screen previousScreen;
    public GraveSelectionGui(List<LightGraveData> data, int page, Screen previousScreen) {
        this.data = data;
        this.page = page;
        this.previousScreen = previousScreen;

        WGridPanel root = new WGridPanel();
        this.setRootPanel(root);
        root.setInsets(Insets.ROOT_PANEL);
        root.setGaps(2, 2);

        WLabel title = new WLabel(Text.translatable("Graves of %s"));
        root.add(title, 0, 0);

        Icon icon = new ItemIcon(Yigd.GRAVE_BLOCK.asItem());
        WFilterableListPanel<LightGraveData, WCardButton> listPanel = new WFilterableListPanel<>(data,
                () -> new WCardButton(icon), (lightGraveData, wCardButton) -> {
            wCardButton.setCardText(lightGraveData.deathMessage().getDeathMessage());
            wCardButton.setOverlayColor(lightGraveData.status().getTransparentColor());

            BlockPos gravePos = lightGraveData.pos();
            wCardButton.setTooltipText(List.of(
                    Text.translatable("X: %d / Y: %d / Z: %d".formatted(gravePos.getX(), gravePos.getY(), gravePos.getZ())),
                    Text.translatable("%s".formatted(lightGraveData.registryKey().getValue().toString())),
                    Text.translatable("%d items".formatted(lightGraveData.itemCount())),
                    Text.translatable("%d levels".formatted(lightGraveData.xpPoints()))
            ));
            wCardButton.setOnClick(() -> ClientPacketHandler.sendGraveOverviewRequest(lightGraveData.id()));
        });

        root.add(listPanel, 0, 1, 12, 6);


        TextureIcon viewClaimedOn = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/claimed_grave.png"));
        TextureIcon viewClaimedOff = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/claimed_grave_cross.png"));
        WHoverToggleButton viewClaimed = new WHoverToggleButton(
                viewClaimedOn,
                Text.translatable("Viewing claimed graves"),
                viewClaimedOff,
                Text.translatable("Hiding claimed graves"));
        TextureIcon viewUnclaimedOn = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/unclaimed_grave.png"));
        TextureIcon viewUnclaimedOff = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/unclaimed_grave_cross.png"));
        WHoverToggleButton viewUnclaimed = new WHoverToggleButton(
                viewUnclaimedOn,
                Text.translatable("Viewing unclaimed graves"),
                viewUnclaimedOff,
                Text.translatable("Hiding unclaimed graves"));
        TextureIcon viewDestroyedOn = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/destroyed_grave.png"));
        TextureIcon viewDestroyedOff = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/destroyed_grave_cross.png"));
        WHoverToggleButton viewDestroyed = new WHoverToggleButton(
                viewDestroyedOn,
                Text.translatable("Viewing destroyed graves"),
                viewDestroyedOff,
                Text.translatable("Hiding destroyed graves"));
        TextureIcon showStatusOn = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/show_status.png"));
        TextureIcon showStatusOff = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/hide_status.png"));
        WHoverToggleButton showStatus = new WHoverToggleButton(
                showStatusOn,
                Text.translatable("Showing grave statuses"),
                showStatusOff,
                Text.translatable("Hiding grave statuses"));

        // a == b && bool <=> a == b if bool, else a != b
        viewClaimed.setOnToggle(aBoolean -> {
            listPanel.setFilter("claimed", graveData -> graveData.status() == GraveStatus.CLAIMED && aBoolean);
            /*listPanel.reload();*/
        });
        viewUnclaimed.setOnToggle(aBoolean -> {
            listPanel.setFilter("unclaimed", graveData -> graveData.status() == GraveStatus.UNCLAIMED && aBoolean);
            /*listPanel.reload();*/
        });
        viewDestroyed.setOnToggle(aBoolean -> {
            listPanel.setFilter("destroyed", graveData -> graveData.status() == GraveStatus.DESTROYED && aBoolean);
            /*listPanel.reload();*/
        });

        showStatus.setOnToggle(aBoolean -> {
            for (WCardButton card : listPanel.getWidgets()) {
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

        root.validate(this);
    }

    public Screen getPreviousScreen() {
        return this.previousScreen;
    }
}
