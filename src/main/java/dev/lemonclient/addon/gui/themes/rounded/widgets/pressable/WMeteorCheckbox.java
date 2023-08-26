/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package dev.lemonclient.addon.gui.themes.rounded.widgets.pressable;

import dev.lemonclient.addon.gui.themes.rounded.LemonClientGuiTheme;
import dev.lemonclient.addon.gui.themes.rounded.LemonClientGuiWidget;
import dev.lemonclient.addon.utils.gui.GuiUtils;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;

public class WMeteorCheckbox extends WCheckbox implements LemonClientGuiWidget {
    private double animProgress;

    public WMeteorCheckbox(boolean checked) {
        super(checked);
        animProgress = checked ? 1 : 0;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        LemonClientGuiTheme theme = theme();

        animProgress += (checked ? 1 : -1) * delta * 14;
        animProgress = Math.max(0, Math.min(1, animProgress));

        renderBackground(renderer, this, pressed, mouseOver);

        if (animProgress > 0) {
            double cs = (width - theme.scale(2)) / 1.75 * animProgress;
            GuiUtils.quadRounded(renderer, x + (width - cs) / 2, y + (height - cs) / 2, cs, cs, theme.checkboxColor.get(), ((LemonClientGuiTheme)theme).roundAmount());
        }
    }
}
