package com.b1n_ry.yigd.client.gui;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.client.gui.widget.WCardButton;
import com.b1n_ry.yigd.client.gui.widget.WFilterableListPanel;
import com.b1n_ry.yigd.client.gui.widget.WHoverButton;
import com.b1n_ry.yigd.packets.ClientPacketHandler;
import com.b1n_ry.yigd.packets.LightPlayerData;
import com.mojang.authlib.GameProfile;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WCardPanel;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WTextField;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import io.github.cottonmc.cotton.gui.widget.icon.ItemIcon;
import io.github.cottonmc.cotton.gui.widget.icon.TextureIcon;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class PlayerSelectionGui extends LightweightGuiDescription {
    private final List<LightPlayerData> data;
    private final Screen previousScreen;

    public PlayerSelectionGui(List<LightPlayerData> data, Screen previousScreen) {
        this.data = data;
        this.previousScreen = previousScreen;

        WGridPanel root = new WGridPanel();
        this.setRootPanel(root);
        root.setInsets(Insets.ROOT_PANEL);
        root.setGaps(5, 5);

        WLabel label = new WLabel(Text.translatable("text.yigd.gui.players_on_server"));
        root.add(label, 0, 0);

        WFilterableListPanel<LightPlayerData, WCardButton> listPanel = this.addFilterList(root);
        listPanel.setFilter("filter", lightPlayerData -> lightPlayerData.graveCount() - lightPlayerData.unclaimedCount() - lightPlayerData.destroyedCount() > 0);

        WTextField searchField = new WTextField();
        root.add(searchField, 0, 1, 11, 1);
        searchField.setChangedListener(s -> listPanel.setFilter("filter",
                playerData -> !s.isEmpty() && playerData.playerProfile().getName().toLowerCase().startsWith(s.toLowerCase())));

        this.addFilterButton(root, listPanel);

        root.validate(this);
    }

    private WFilterableListPanel<LightPlayerData, WCardButton> addFilterList(WGridPanel root) {
        ItemIcon defaultIcon = new ItemIcon(Items.PLAYER_HEAD.asItem());
        WFilterableListPanel<LightPlayerData, WCardButton> listPanel = new WFilterableListPanel<>(this.data,
                () -> new WCardButton(defaultIcon), (playerData, wCardButton) -> {
            GameProfile profile = playerData.playerProfile();
            ItemStack skull = new ItemStack(Items.PLAYER_HEAD);
            NbtCompound profileNbt = new NbtCompound();
            NbtHelper.writeGameProfile(profileNbt, profile);
            skull.setSubNbt("SkullOwner", profileNbt);

            ItemIcon icon = new ItemIcon(skull);
            wCardButton.setIcon(icon);
            wCardButton.setCardText(Text.translatable("text.yigd.gui.player_name", profile.getName()));
            wCardButton.setTooltipText(List.of(
                    Text.translatable("text.yigd.gui.unclaimed_count", playerData.unclaimedCount()),
                    Text.translatable("text.yigd.gui.destroyed_count", playerData.destroyedCount()),
                    Text.translatable("text.yigd.gui.total_count", playerData.graveCount())
            ));

            wCardButton.setOnClick(() -> ClientPacketHandler.sendGraveSelectionRequest(profile));
        });

        root.add(listPanel, 0, 2, 12, 6);

        return listPanel;
    }
    private void addFilterButton(WGridPanel root, WFilterableListPanel<LightPlayerData, WCardButton> filterableList) {
        ItemIcon allIcon = new ItemIcon(Items.BARRIER.asItem());
        TextureIcon claimedIcon = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/claimed_grave.png"));
        TextureIcon unclaimedIcon = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/unclaimed_grave.png"));
        TextureIcon destroyedIcon = new TextureIcon(new Identifier(Yigd.MOD_ID, "textures/gui/destroyed_grave.png"));

        WCardPanel cardPanel = new WCardPanel();
        WHoverButton showAllButton = new WHoverButton(allIcon, Text.translatable("button.yigd.gui.showing_with_data"));
        WHoverButton showClaimedButton = new WHoverButton(claimedIcon, Text.translatable("button.yigd.gui.showing_with_claimed"));
        WHoverButton showUnclaimedButton = new WHoverButton(unclaimedIcon, Text.translatable("button.yigd.gui.showing_with_unclaimed"));
        WHoverButton showDestroyedButton = new WHoverButton(destroyedIcon, Text.translatable("button.yigd.gui.showing_with_destroyed"));

        cardPanel.add(showAllButton);
        cardPanel.add(showClaimedButton);
        cardPanel.add(showUnclaimedButton);
        cardPanel.add(showDestroyedButton);
        int amountOfCards = cardPanel.getCardCount();

        showAllButton.setOnClick(() -> {
            filterableList.setFilter("filter", lightPlayerData -> lightPlayerData.graveCount() - lightPlayerData.unclaimedCount() - lightPlayerData.destroyedCount() > 0);

            int index = (cardPanel.getSelectedIndex() + 1) % amountOfCards;
            cardPanel.setSelectedIndex(index);
        });
        showClaimedButton.setOnClick(() -> {
            filterableList.setFilter("filter", lightPlayerData -> lightPlayerData.unclaimedCount() > 0);

            int index = (cardPanel.getSelectedIndex() + 1) % amountOfCards;
            cardPanel.setSelectedIndex(index);
        });
        showUnclaimedButton.setOnClick(() -> {
            filterableList.setFilter("filter", lightPlayerData -> lightPlayerData.destroyedCount() > 0);

            int index = (cardPanel.getSelectedIndex() + 1) % amountOfCards;
            cardPanel.setSelectedIndex(index);
        });
        showDestroyedButton.setOnClick(() -> {
            filterableList.setFilter("filter", lightPlayerData -> true);

            int index = (cardPanel.getSelectedIndex() + 1) % amountOfCards;
            cardPanel.setSelectedIndex(index);
        });

        root.add(cardPanel, 11, 1, 1, 1);
    }

    public Screen getPreviousScreen() {
        return this.previousScreen;
    }
}
