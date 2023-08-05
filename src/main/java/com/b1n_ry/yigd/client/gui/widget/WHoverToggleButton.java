package com.b1n_ry.yigd.client.gui.widget;

import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import io.github.cottonmc.cotton.gui.widget.icon.Icon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class WHoverToggleButton extends WButton {
    private final Text onText;
    private final Text offText;
    private final Icon onImg;
    private final Icon offImg;
    private boolean isOn;
    private Consumer<Boolean> onToggle = null;

    private static final TextRenderer TEXT_RENDERER = MinecraftClient.getInstance().textRenderer;

    public WHoverToggleButton(Icon onImg, Text onText, Icon offImg, Text offText) {
        super(onImg);

        this.onImg = onImg;
        this.offImg = offImg;

        this.onText = onText;
        this.offText = offText;

        this.width = 20;
    }

    @Override
    public boolean canResize() {
        return false;
    }

    @Override
    public void paint(DrawContext context, int x, int y, int mouseX, int mouseY) {
        super.paint(context, x, y, mouseX, mouseY);

        if (mouseX >= 0 && mouseX <= this.width && mouseY >= 0 && mouseY <= this.height) {
            context.drawTooltip(TEXT_RENDERER, this.isOn ? this.onText : this.offText, x + mouseX, y + mouseY);
        }
    }

    @Override
    public InputResult onClick(int x, int y, int button) {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        this.isOn = !this.isOn;
        this.onToggle(this.isOn);
        return InputResult.PROCESSED;
    }

    private void onToggle(boolean on) {
        if (this.onToggle != null) {
            this.onToggle.accept(on);
        }

        this.setIcon(this.isOn ? this.onImg : this.offImg);
    }

    public void setOnToggle(Consumer<Boolean> onToggle) {
        this.onToggle = onToggle;
    }
    public void setToggle(boolean on) {
        this.isOn = on;
        this.onToggle(on);
    }

    @Override
    public InputResult onKeyPressed(int ch, int key, int modifiers) {
        if (isActivationKey(ch)) {
            onClick(0, 0, 0);
            return InputResult.PROCESSED;
        }

        return InputResult.IGNORED;
    }
}
