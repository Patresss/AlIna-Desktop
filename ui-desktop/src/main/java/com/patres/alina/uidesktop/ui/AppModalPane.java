package com.patres.alina.uidesktop.ui;

import atlantafx.base.controls.ModalPane;
import javafx.geometry.Pos;
import javafx.geometry.Side;

public class AppModalPane extends ModalPane {

    public AppModalPane() {
        getStyleClass().add("modal-pane");
        setAlignment(Pos.TOP_LEFT);
        usePredefinedTransitionFactories(Side.LEFT);

    }



}
