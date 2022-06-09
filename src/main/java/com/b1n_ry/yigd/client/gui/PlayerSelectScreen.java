package com.b1n_ry.yigd.client.gui;

import com.b1n_ry.yigd.core.DeadPlayerData;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class PlayerSelectScreen extends Screen {
    private final Identifier GRAVE_SELECT_TEXTURE = new Identifier("yigd", "textures/gui/select_player_menu.png");
    private final Identifier SELECT_ELEMENT_TEXTURE = new Identifier("yigd", "textures/gui/select_elements.png");

    private final Map<UUID, List<DeadPlayerData>> data;
    private final Map<UUID, Identifier> playerSkinTextures;
    private final Map<UUID, GameProfile> graveOwners;
    private final int page;

    private final Map<UUID, List<DeadPlayerData>> filteredPlayers = new HashMap<>();
    private final List<UUID> filteredPlayerIds = new ArrayList<>();

    private boolean mouseIsClicked = false;
    private String hoveredElement = null;

    private final StringBuilder searchField;
    private boolean isTyping = false;

    private boolean includeAvailable;
    private boolean includeClaimed;
    private boolean includeDestroyed;
    private boolean showWithoutGrave;

    public PlayerSelectScreen(Map<UUID, List<DeadPlayerData>> data, int page) {
        this(data, page, null, true, false, false, false);
    }
    public PlayerSelectScreen(Map<UUID, List<DeadPlayerData>> data, int page, StringBuilder search, boolean includeAvailable, boolean includeClaimed, boolean includeDestroyed, boolean showWithoutGrave) {
        super(MutableText.of(new TranslatableTextContent("text.yigd.gui.player_select.title")));

        Map<UUID, List<DeadPlayerData>> nonEmpty = new HashMap<>();
        Map<UUID, Identifier> playerSkinTextures = new HashMap<>();
        Map<UUID, GameProfile> graveOwners = new HashMap<>();

        data.forEach((uuid, userData) -> {
            if (userData.size() > 0) {
                GameProfile profile = userData.get(0).graveOwner;
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

        this.data = nonEmpty;
        this.playerSkinTextures = playerSkinTextures;
        this.page = page;
        this.graveOwners = graveOwners;

        this.searchField = Objects.requireNonNullElseGet(search, StringBuilder::new);

        this.includeAvailable = includeAvailable;
        this.includeClaimed = includeClaimed;
        this.includeDestroyed = includeDestroyed;
        this.showWithoutGrave = showWithoutGrave;

        reloadFilters();
    }

    private void reloadFilters() {
        this.filteredPlayerIds.clear();
        this.filteredPlayers.clear();
        this.data.forEach((uuid, deadPlayerData) -> {
            List<DeadPlayerData> filteredGraves = new ArrayList<>();
            boolean matchSearch = true;
            for (DeadPlayerData grave : deadPlayerData) {
                if (!grave.graveOwner.getName().toLowerCase(Locale.ROOT).startsWith(searchField.toString().toLowerCase(Locale.ROOT))) {
                    matchSearch = false;
                    continue;
                }
                if (grave.availability == 1 && this.includeAvailable) {
                    filteredGraves.add(grave);
                } else if (grave.availability == 0 && this.includeClaimed) {
                    filteredGraves.add(grave);
                } else if (grave.availability == -1 && this.includeDestroyed) {
                    filteredGraves.add(grave);
                }
            }

            if ((filteredGraves.size() > 0 || this.showWithoutGrave) && matchSearch) {
                filteredPlayers.put(uuid, filteredGraves);
                filteredPlayerIds.add(uuid);
            }
        });
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (client != null && !isTyping && (client.options.inventoryKey.matchesKey(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_BACKSPACE)) {
            this.close();
            return true;
        } else if (this.isTyping) {
            String newLetter = GLFW.glfwGetKeyName(keyCode, scanCode);
            if (newLetter != null && searchField.length() < 16) {
                if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0 || (modifiers & GLFW.GLFW_MOD_CAPS_LOCK) != 0) {
                    newLetter = newLetter.toUpperCase(Locale.ROOT);
                    if (newLetter.equals("-")) newLetter = "_";
                }

                searchField.append(newLetter);
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && searchField.length() > 0) {
                searchField.deleteCharAt(searchField.length() - 1);
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.isTyping = false;
            }
            reloadFilters();
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

        this.isTyping = hoveredElement != null && hoveredElement.equals("search_bar");

        if (button == 0 && hoveredElement != null && client != null) {
            if (hoveredElement.equals("left") && page > 1) {
                PlayerSelectScreen screen = new PlayerSelectScreen(data, page - 1, this.searchField, this.includeAvailable, this.includeClaimed, this.includeDestroyed, this.showWithoutGrave);
                client.setScreen(screen);
            } else if (hoveredElement.equals("right") && filteredPlayers.size() > page * 4) {
                PlayerSelectScreen screen = new PlayerSelectScreen(data, page + 1, this.searchField, this.includeAvailable, this.includeClaimed, this.includeDestroyed, this.showWithoutGrave);
                client.setScreen(screen);
            } else if (hoveredElement.equals("include_available")) {
                this.includeAvailable = !this.includeAvailable;
                reloadFilters();
            } else if (hoveredElement.equals("include_claimed")) {
                this.includeClaimed = !this.includeClaimed;
                reloadFilters();
            } else if (hoveredElement.equals("include_destroyed")) {
                this.includeDestroyed = !this.includeDestroyed;
                reloadFilters();
            } else if (hoveredElement.equals("show_zero")) {
                this.showWithoutGrave = !this.showWithoutGrave;
                reloadFilters();

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
        final int screenHeight = 231;
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

        int infoSize = this.filteredPlayerIds.size();
        int startValue = (page - 1) * 4;
        int whileLessThan = startValue + Math.min(4, infoSize - startValue);
        for (int i = startValue; i < whileLessThan; i++) {
            UUID playerId = filteredPlayerIds.get(i);
            RenderSystem.setShaderTexture(0, SELECT_ELEMENT_TEXTURE);

            int left = screenLeft + 19;
            int top = screenTop + 55 + 42 * (i % 4);
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

            textRenderer.draw(matrices, graveOwners.get(playerId).getName(), left + 42, top + 7, 0x004000);
            textRenderer.draw(matrices, MutableText.of(new TranslatableTextContent("text.yigd.gui.player_select.grave_count", filteredPlayers.get(playerId).size())), left + 42, top + 22, 0x555555);
        }

        super.render(matrices, mouseX, mouseY, delta);

        renderCheckButtons(matrices, mouseX, mouseY, screenTop, screenLeft, originX);
        renderSearchBar(matrices, screenTop, screenLeft, mouseX, mouseY);

        String gravesDisplayed = (startValue + 1) + "-" + whileLessThan + "/" + infoSize;
        textRenderer.draw(matrices, MutableText.of(new TranslatableTextContent("text.yigd.gui.player_select.players_with_graves")), screenLeft + 19f, screenTop + 10f, 0x555555);

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

    private void renderCheckButtons(MatrixStack matrices, int mouseX, int mouseY, int screenTop, int screenLeft, int originX) {
        int boxTop = screenTop + 22;
        int boxRow = boxTop + 9;

        // Rendering checkbox for including available graves
        RenderSystem.setShaderTexture(0, SELECT_ELEMENT_TEXTURE);
        int leftEdge = screenLeft + 18;
        if (mouseX > leftEdge && mouseX < leftEdge + 6 && mouseY > boxTop && mouseY < boxTop + 6) {
            hoveredElement = "include_available";
        }
        if (hoveredElement != null && hoveredElement.equals("include_available") && mouseIsClicked) {
            drawTexture(matrices, leftEdge, boxTop, 32, 90, 6, 6);
        } else {
            drawTexture(matrices, leftEdge, boxTop, 32, 84, 6, 6);
        }
        if (this.includeAvailable) drawTexture(matrices, leftEdge, boxTop, 38, 84, 6, 6);
        textRenderer.draw(matrices, MutableText.of(new TranslatableTextContent("text.yigd.gui.player_select.include_available")), leftEdge + 8f, boxTop - 1f, 0x777777);

        // Include Claimed
        RenderSystem.setShaderTexture(0, SELECT_ELEMENT_TEXTURE);
        if (mouseX > originX && mouseX < originX + 6 && mouseY > boxTop && mouseY < boxTop + 6) {
            hoveredElement = "include_claimed";
        }
        if (hoveredElement != null && hoveredElement.equals("include_claimed") && mouseIsClicked) {
            drawTexture(matrices, originX, boxTop, 32, 90, 6, 6);
        } else {
            drawTexture(matrices, originX, boxTop, 32, 84, 6, 6);
        }
        if (this.includeClaimed) drawTexture(matrices, originX, boxTop, 38, 84, 6, 6);
        textRenderer.draw(matrices, MutableText.of(new TranslatableTextContent("text.yigd.gui.player_select.include_claimed")), originX + 8f, boxTop - 1f, 0x777777);

        // Include Destroyed
        RenderSystem.setShaderTexture(0, SELECT_ELEMENT_TEXTURE);
        if (mouseX > leftEdge && mouseX < leftEdge + 6 && mouseY > boxRow && mouseY < boxRow + 6) {
            hoveredElement = "include_destroyed";
        }
        if (hoveredElement != null && hoveredElement.equals("include_destroyed") && mouseIsClicked) {
            drawTexture(matrices, leftEdge, boxRow, 32, 90, 6, 6);
        } else {
            drawTexture(matrices, leftEdge, boxRow, 32, 84, 6, 6);
        }
        if (this.includeDestroyed) drawTexture(matrices, leftEdge, boxRow, 38, 84, 6, 6);
        textRenderer.draw(matrices, MutableText.of(new TranslatableTextContent("text.yigd.gui.player_select.include_destroyed")), leftEdge + 8f, boxRow - 1f, 0x777777);

        // Show empty
        RenderSystem.setShaderTexture(0, SELECT_ELEMENT_TEXTURE);
        if (mouseX > originX && mouseX < originX + 6 && mouseY > boxRow && mouseY < boxRow + 6) {
            hoveredElement = "show_zero";
        }
        if (hoveredElement != null && hoveredElement.equals("show_zero") && mouseIsClicked) {
            drawTexture(matrices, originX, boxRow, 32, 90, 6, 6);
        } else {
            drawTexture(matrices, originX, boxRow, 32, 84, 6, 6);
        }
        if (this.showWithoutGrave) drawTexture(matrices, originX, boxRow, 38, 84, 6, 6);
        textRenderer.draw(matrices, MutableText.of(new TranslatableTextContent("text.yigd.gui.player_select.show_zero")), originX + 8f, boxRow - 1f, 0x777777);
    }

    private void renderSearchBar(MatrixStack matrices, int screenTop, int screenLeft, int mouseX, int mouseY) {
        textRenderer.draw(matrices, Text.of(searchField.toString() + (isTyping ? "_" : "")), screenLeft + 20, screenTop + 41, 0xFFFFFF);

        if (mouseX > screenLeft + 18 && mouseX < screenLeft + 202 && mouseY > screenTop + 39 && mouseY < screenTop + 51) {
            hoveredElement = "search_bar";
        }
    }

    public void addData(UUID user, DeadPlayerData data) {
        List<DeadPlayerData> deadData = this.data.computeIfAbsent(user, entry -> new ArrayList<>());
        List<DeadPlayerData> newList = new ArrayList<>(deadData);
        newList.add(data);

        this.data.put(user, newList);

        reloadFilters();
    }
}
