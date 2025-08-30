package com.patres.alina.uidesktop.plugin;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.common.event.PluginUpdateEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Point2D;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

public class SearchPluginPopup extends Popup {

    public static final String FIND_PLUGIN_CHARACTER = "/";
    public static final int CELL_HEIGTH = 40;
    public static final int MAX_SIZE_OF_CELLS = 6;
    public static final int CELL_MARGIN_LIST_VIEW = 2;

    private final ListView<CardListItem> pluginListView;
    private final TextArea chatTextArea;
    private final ObjectProperty<CardListItem> selectedPlugin = new SimpleObjectProperty<>();

    private List<CardListItem> allPlugins;

    public SearchPluginPopup(final TextArea chatTextArea) {
        this.chatTextArea = chatTextArea;
        pluginListView = createListView();

        getContent().add(pluginListView);
        setAutoHide(true);
        fetchAllPlugins();
        setupEscapeKeyHandler();

        DefaultEventBus.getInstance().subscribe(PluginUpdateEvent.class, e -> fetchAllPlugins());
    }

    private void fetchAllPlugins() {
        allPlugins = BackendApi.getPluginListItems();

        updatePlugins("");
    }

    private void updatePlugins(final String filter) {
        List<CardListItem> filteredPlugins = allPlugins.stream()
                .filter(p -> containsIgnoreCase(p.name(), filter))
                .toList();
        pluginListView.getItems().setAll(filteredPlugins);

        updateHeightOfListView(filteredPlugins);

    }

    private void updateHeightOfListView(final List<CardListItem> filteredPlugins) {
        if (filteredPlugins.size() > MAX_SIZE_OF_CELLS) {
            pluginListView.prefHeightProperty().unbind();
            pluginListView.setPrefHeight(MAX_SIZE_OF_CELLS * CELL_HEIGTH + CELL_MARGIN_LIST_VIEW);
        } else {
            pluginListView.prefHeightProperty().bind(Bindings.size(FXCollections.observableArrayList(filteredPlugins))
                    .multiply(CELL_HEIGTH).add(CELL_MARGIN_LIST_VIEW));
        }
    }

    private ListView<CardListItem> createListView() {
        final ListView<CardListItem> listView = new ListView<>();
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CardListItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.name());
                    setGraphic(new FontIcon(item.icon()));
                }
            }
        });

        listView.setOnMouseClicked(event -> {
            updateCurrentPlugin(listView.getSelectionModel().getSelectedItem());
            hide();
        });

        listView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                updateCurrentPlugin(listView.getSelectionModel().getSelectedItem());
                hide();
            }
        });

        return listView;
    }

    private void setupEscapeKeyHandler() {
        chatTextArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                updateCurrentPlugin(null);
                hide();
            }
        });
    }

    private void updateCurrentPlugin(final CardListItem cardListItem) {
        if (chatTextArea.getText().startsWith(FIND_PLUGIN_CHARACTER)) {
            chatTextArea.clear();
        }
        selectedPlugin.setValue(cardListItem);
    }

    public ObjectProperty<CardListItem> getSelectedPluginProperty() {
        return selectedPlugin;
    }

    public void handleTextChangeListener(final String text, final Stage stage) {
        Platform.runLater(() -> {
            pluginListView.setPrefWidth(chatTextArea.getWidth());
            if (text.startsWith(FIND_PLUGIN_CHARACTER)) {
                final String searchText = text.substring(1);
                updatePlugins(searchText);

                final Point2D point = chatTextArea.localToScene(0.0, 0.0);
                show(
                        stage,
                        stage.getX() + point.getX(),
                        stage.getY() + point.getY() + chatTextArea.getHeight()
                );
            } else {
                hide();
            }
        });
    }

}
