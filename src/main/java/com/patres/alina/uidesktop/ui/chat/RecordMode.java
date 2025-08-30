package com.patres.alina.uidesktop.ui.chat;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

public enum RecordMode {

    PREPARE_TO_START_RECORDING(new FontIcon(Feather.MIC)),
    PREPARE_TO_STOP_RECORDING(new FontIcon(Feather.STOP_CIRCLE));

    private final FontIcon fontIcon;

    RecordMode(FontIcon fontIcon) {
        this.fontIcon = fontIcon;
    }

    public FontIcon getFontIcon() {
        return fontIcon;
    }
}
