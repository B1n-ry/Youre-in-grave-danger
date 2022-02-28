package com.b1n_ry.yigd.client.gui;

import com.b1n_ry.yigd.core.DeadPlayerData;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class PlayerSelectScreen extends Screen {
    private final Identifier GRAVE_SELECT_TEXTURE = new Identifier("yigd", "textures/gui/select_menu.png");
    private final Identifier SELECT_ELEMENT_TEXTURE = new Identifier("yigd", "textures/gui/select_elements.png");

    private final List<UUID> playerIds;
    private final Map<UUID, List<DeadPlayerData>> data;
    private final Map<UUID, Identifier> playerSkinTextures;
    private final Map<UUID, GameProfile> graveOwners;
    private final int page;

    private boolean mouseIsClicked = false;
    private String hoveredElement = null;

    public PlayerSelectScreen(Map<UUID, List<DeadPlayerData>> data, int page) {
        super(new TranslatableText("text.yigd.gui.player_select.title"));

        List<UUID> playerIds = new ArrayList<>();
        Map<UUID, List<DeadPlayerData>> nonEmpty = new HashMap<>();
        Map<UUID, Identifier> playerSkinTextures = new HashMap<>();
        Map<UUID, GameProfile> graveOwners = new HashMap<>();

        data.forEach((uuid, userData) -> {
            if (userData.size() > 0) {
                GameProfile profile = userData.get(0).graveOwner;
                playerIds.add(uuid);
                nonEmpty.put(uuid, userData);
                graveOwners.put(uuid, profile);

                Identifier defaultPlayerSkin = DefaultSkinHelper.getTexture(uuid);

                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                if (minecraftClient != null) {
                    Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraftClient.getSkinProvider().getTextures(profile);
                    if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                        playerSkinTextures.put(uuid, minecraftClient.getSkinProvider().loadSkin(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN));
                    } else {
                        playerSkinTextures.put(uuid, defaultPlayerSkin);
                    }
                } else {
                    playerSkinTextures.put(uuid, defaultPlayerSkin);
                }
            }
        });

        this.playerIds = playerIds;
        this.data = nonEmpty;
        this.playerSkinTextures = playerSkinTextures;
        this.page = page;
        this.graveOwners = graveOwners;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (client != null && client.options.inventoryKey.matchesKey(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            this.close();
            return true;
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
                PlayerSelectScreen screen = new PlayerSelectScreen(data, page - 1);
                client.setScreen(screen);
            } else if (hoveredElement.equals("right") && data.size() > page * 4) {
                PlayerSelectScreen screen = new PlayerSelectScreen(data, page + 1);
                client.setScreen(screen);
            } else if (isUuid(hoveredElement)) {
                UUID parsedString = UUID.fromString(hoveredElement);
                if (data.containsKey(parsedString)) {
                    GraveSelectScreen screen = new GraveSelectScreen(data.get(parsedString), 1, this);
                    client.setScreen(screen);
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        final int screenWidth = 220;
        final int screenHeight = 200;
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

        int infoSize = this.playerIds.size();
        int startValue = (page - 1) * 4;
        int whileLessThan = startValue + Math.min(4, infoSize - startValue);
        for (int i = startValue; i < whileLessThan; i++) {
            UUID playerId = playerIds.get(i);
            RenderSystem.setShaderTexture(0, SELECT_ELEMENT_TEXTURE);

            int left = screenLeft + 19;
            int top = screenTop + 24 + 42 * i - startValue;
            int width = screenWidth - 19 * 2;
            int height = 42;
            if (mouseX > left && mouseX < left + width && mouseY > top && mouseY < top + height) {
                hoveredElement = playerId.toString();
            }
            if (hoveredElement != null && hoveredElement.equals(playerId.toString()) && mouseIsClicked) {
                drawTexture(matrices, left, top, 0, height, width, height);
            } else {
                drawTexture(matrices, left, top, 0, 0, width, height);
            }

            RenderSystem.setShaderTexture(0, playerSkinTextures.get(playerId));
            drawTexture(matrices, left + 5, top + 5, 32, 32, 32, 32);

            textRenderer.draw(matrices, graveOwners.get(playerId).getName(), left + 42, top + 7, 0x009900);
            textRenderer.draw(matrices, data.get(playerId).size() + " graves", left + 42, top + 22, 0x555555);
        }

        super.render(matrices, mouseX, mouseY, delta);

        String gravesDisplayed = (startValue + 1) + "-" + whileLessThan + "/" + infoSize;
        textRenderer.draw(matrices, "Players with graves", screenLeft + 19f, screenTop + 10f, 0x555555);

        int offset = textRenderer.getWidth(gravesDisplayed);
        textRenderer.draw(matrices, gravesDisplayed, screenLeft + screenWidth - 19f - offset, screenTop + 10f, 0x007700);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean isUuid(String intString) {
        if (intString == null) return false;
        try {
            UUID.fromString(intString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
