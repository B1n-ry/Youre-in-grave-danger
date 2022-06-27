package com.b1n_ry.yigd.client.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WarningScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ConfigWarningScreen extends WarningScreen {
    private static final Text HEADER = Text.translatable("text.yigd.gui.config_warning.header");
    private static final Text MESSAGE = Text.translatable("text.yigd.gui.config_warning.message");
    private static final Text CHECK_MESSAGE = Text.translatable("text.yigd.gui.config_warning.check");
    private static final Text NARRATED_TEXT = HEADER.copy().append("\n").append(MESSAGE);
    private final Screen parent;

    public ConfigWarningScreen(Screen parent) {
        super(HEADER, MESSAGE, CHECK_MESSAGE, NARRATED_TEXT);
        this.parent = parent;
    }

    @Override
    protected void initButtons(int yOffset) {
        if (this.client == null) return;
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 155, 100 + yOffset, 150, 20, ScreenTexts.PROCEED, buttonWidget -> this.client.setScreen(parent)));
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 155 + 160, 100 + yOffset, 150, 20, Text.translatable("menu.quit"), buttonWidget -> this.client.scheduleStop()));
    }
}
