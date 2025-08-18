package com.patres.alina.uidesktop.ui.util;

import atlantafx.base.controls.Tile;
import com.patres.alina.uidesktop.ui.TextSeparator;
import com.patres.alina.uidesktop.ui.atlantafx.CustomTile;
import com.patres.alina.uidesktop.ui.language.LanguageManager;

public class TranslatedComponentUtils {

    public static Tile createTile(final String internalizedTitle, final String internalizedDescription) {
        var customTile = new CustomTile(
                internalizedTitle,
                internalizedDescription
        );

        if (internalizedTitle != null) {
            customTile.titleProperty().bind(LanguageManager.createStringBinding(internalizedTitle));
        }

        if (internalizedDescription != null) {
            customTile.descriptionProperty().bind(LanguageManager.createStringBinding(internalizedDescription));
        }
        return customTile;
    }

    public static TextSeparator createTextSeparator(final String internalizedTitle, final String style) {
        var textSeparator = new TextSeparator(internalizedTitle, style);
        textSeparator.getText().textProperty().bind(LanguageManager.createStringBinding(internalizedTitle));
        return textSeparator;
    }
}
