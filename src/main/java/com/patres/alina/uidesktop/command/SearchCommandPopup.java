package com.patres.alina.uidesktop.command;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.server.command.Command;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.command.settings.CardListItemMapper;
import com.patres.alina.uidesktop.common.event.CommandUpdateEvent;
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

public class SearchCommandPopup extends Popup {

    public static final String FIND_COMMAND_CHARACTER = "/";
    public static final int CELL_HEIGTH = 40;
    public static final int MAX_SIZE_OF_CELLS = 6;
    public static final int CELL_MARGIN_LIST_VIEW = 2;

    private final ListView<CardListItem> commandListView;
    private final TextArea chatTextArea;
    private final ObjectProperty<CardListItem> selectedCommand = new SimpleObjectProperty<>();

    private List<Command> allCommands;

    public SearchCommandPopup(final TextArea chatTextArea) {
        this.chatTextArea = chatTextArea;
        commandListView = createListView();

        getContent().add(commandListView);
        setAutoHide(true);
        fetchAllCommands();
        setupEscapeKeyHandler();

        DefaultEventBus.getInstance().subscribe(CommandUpdateEvent.class, e -> fetchAllCommands());
    }

    private void fetchAllCommands() {
        allCommands = BackendApi.getEnabledCommands().stream()
                .filter(command -> command.visibility().showInChat())
                .toList();

        updateCommands("");
    }

    private void updateCommands(final String filter) {
        List<CardListItem> filteredCommands = allCommands.stream()
                .filter(p -> containsIgnoreCase(p.name(), filter))
                .map(CardListItemMapper::toCardListItem)
                .toList();
        commandListView.getItems().setAll(filteredCommands);

        updateHeightOfListView(filteredCommands);

    }

    private void updateHeightOfListView(final List<CardListItem> filteredCommands) {
        if (filteredCommands.size() > MAX_SIZE_OF_CELLS) {
            commandListView.prefHeightProperty().unbind();
            commandListView.setPrefHeight(MAX_SIZE_OF_CELLS * CELL_HEIGTH + CELL_MARGIN_LIST_VIEW);
        } else {
            commandListView.prefHeightProperty().bind(Bindings.size(FXCollections.observableArrayList(filteredCommands))
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
            updateCurrentCommand(listView.getSelectionModel().getSelectedItem());
            hide();
        });

        listView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                updateCurrentCommand(listView.getSelectionModel().getSelectedItem());
                hide();
            }
        });

        return listView;
    }

    private void setupEscapeKeyHandler() {
        chatTextArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                updateCurrentCommand(null);
                hide();
            }
        });
    }

    private void updateCurrentCommand(final CardListItem cardListItem) {
        if (chatTextArea.getText().startsWith(FIND_COMMAND_CHARACTER)) {
            chatTextArea.clear();
        }
        selectedCommand.setValue(cardListItem);
    }

    public ObjectProperty<CardListItem> getSelectedCommandProperty() {
        return selectedCommand;
    }

    public void handleTextChangeListener(final String text, final Stage stage) {
        Platform.runLater(() -> {
            commandListView.setPrefWidth(chatTextArea.getWidth());
            if (text.startsWith(FIND_COMMAND_CHARACTER)) {
                final String searchText = text.substring(1);
                updateCommands(searchText);

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
