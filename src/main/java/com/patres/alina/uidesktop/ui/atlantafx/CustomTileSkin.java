package com.patres.alina.uidesktop.ui.atlantafx;

import atlantafx.base.controls.TileSkin;

/**
 * Workaround: https://github.com/mkpaz/atlantafx/issues/74
 * TileSkinBase title ChangeListener code is wrong #74
 */
public class CustomTileSkin extends TileSkin {

    public CustomTileSkin(CustomTile control) {
        super(control);
        registerChangeListener(control.titleProperty(), o -> {
            var value = getSkinnable().getTitle();
            titleLbl.setText(value);
            titleLbl.setVisible(value != null);
            titleLbl.setManaged(value != null);
            getSkinnable().pseudoClassStateChanged(HAS_TITLE, value != null);
        });
    }

}
