package com.patres.alina.uidesktop.ui.card;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.theme.Styles;
import com.patres.alina.uidesktop.plugin.settings.PluginCard;
import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTextSeparator;

public abstract class CardsPane extends ApplicationModalPaneContent {

    protected final ModalPane modalPane;
    protected final ApplicationWindow applicationWindow;

    protected VBox cardsHolder;

    public CardsPane(Runnable backFunction, ApplicationWindow applicationWindow) {
        super(backFunction);
        this.modalPane = applicationWindow.getAppModalPane();
        this.applicationWindow = applicationWindow;
    }

    @FXML
    public void initialize() {
        super.initialize();
        buttonBar.getButtons().add(createButton());
    }

    public abstract ButtonBase createButton();
    public abstract List<? extends CardItem> createCards();
    public abstract String getHeaderInternalizedTitle();

    @Override
    public void reload() {
        cardsHolder.getChildren().clear();
        cardsHolder.getChildren().addAll(createCards());
    }

    @Override
    protected List<Node> generateContent() {
        final var header = createTextSeparator(getHeaderInternalizedTitle(), Styles.TITLE_3);
        cardsHolder = createCardHolder();

        final List<Node> nodes = new ArrayList<>();
        nodes.add(header);
        nodes.add(cardsHolder);
        return nodes;
    }

    private static VBox createCardHolder() {
        final VBox cardsHolder = new VBox();
        cardsHolder.getStyleClass().add("card-holder");
        return cardsHolder;
    }

    public void deletePluginCard(final CardItem card) {
        cardsHolder.getChildren().remove(card);
    }

}
