package com.patres.alina.uidesktop.command;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.State;
import com.patres.alina.server.command.Command;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.command.settings.CardListItemMapper;
import com.patres.alina.uidesktop.common.event.CommandUpdateEvent;
import com.patres.alina.uidesktop.common.event.QuickActionSettingsUpdateEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.quickaction.QuickActionSettings;
import com.patres.alina.uidesktop.quickaction.QuickActionType;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
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

import java.util.ArrayList;
import java.util.List;

import static com.patres.alina.uidesktop.settings.SettingsMangers.QUICK_ACTION_SETTINGS;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

public class SearchCommandPopup extends Popup {

    public static final String FIND_COMMAND_CHARACTER = "/";
    public static final String QUICK_ACTION_PREFIX = "qa:";
    public static final int CELL_HEIGTH = 40;
    public static final int MAX_SIZE_OF_CELLS = 6;
    public static final int CELL_MARGIN_LIST_VIEW = 2;

    private final ListView<CardListItem> commandListView;
    private final TextArea chatTextArea;
    private final ObjectProperty<CardListItem> selectedCommand = new SimpleObjectProperty<>();
    private final ObjectProperty<QuickActionType> selectedQuickAction = new SimpleObjectProperty<>();

    private List<Command> allCommands = List.of();
    private List<CardListItem> allQuickActions = List.of();

    public SearchCommandPopup(final TextArea chatTextArea) {
        this.chatTextArea = chatTextArea;
        commandListView = createListView();

        getContent().add(commandListView);
        setAutoHide(true);
        fetchAllQuickActions();
        fetchAllCommands();
        setupEscapeKeyHandler();

        DefaultEventBus.getInstance().subscribe(CommandUpdateEvent.class, e -> fetchAllCommands());
        DefaultEventBus.getInstance().subscribe(QuickActionSettingsUpdateEvent.class, e -> fetchAllQuickActions());
        LanguageManager.localeProperty().addListener((obs, oldLocale, newLocale) -> Platform.runLater(this::fetchAllQuickActions));
    }

    private void fetchAllCommands() {
        Thread.startVirtualThread(() -> {
            final List<Command> commands = BackendApi.getEnabledCommands().stream()
                    .filter(command -> command.visibility().showInChat())
                    .toList();
            Platform.runLater(() -> {
                allCommands = commands;
                updateItems("");
            });
        });
    }

    private void fetchAllQuickActions() {
        QuickActionSettings settings = QUICK_ACTION_SETTINGS.getSettings();
        allQuickActions = new ArrayList<>();
        for (QuickActionType type : QuickActionType.values()) {
            if (settings.isEnabled(type)) {
                allQuickActions.add(new CardListItem(
                        QUICK_ACTION_PREFIX + type.getId(),
                        LanguageManager.getLanguageString(type.getTitleKey()),
                        LanguageManager.getLanguageString(type.getDescriptionKey()),
                        type.getIcon(),
                        State.ENABLED
                ));
            }
        }
        updateItems("");
    }

    private void updateItems(final String filter) {
        List<CardListItem> filteredCommands = allCommands.stream()
                .filter(p -> containsIgnoreCase(p.name(), filter))
                .map(CardListItemMapper::toCardListItem)
                .toList();

        List<CardListItem> filteredQuickActions = allQuickActions.stream()
                .filter(p -> containsIgnoreCase(p.name(), filter))
                .toList();

        List<CardListItem> allItems = new ArrayList<>();
        allItems.addAll(filteredQuickActions);
        allItems.addAll(filteredCommands);

        commandListView.getItems().setAll(allItems);
        updateHeightOfListView(allItems);
    }

    private void updateHeightOfListView(final List<CardListItem> filteredItems) {
        if (filteredItems.size() > MAX_SIZE_OF_CELLS) {
            commandListView.prefHeightProperty().unbind();
            commandListView.setPrefHeight(MAX_SIZE_OF_CELLS * CELL_HEIGTH + CELL_MARGIN_LIST_VIEW);
        } else {
            commandListView.prefHeightProperty().bind(Bindings.size(FXCollections.observableArrayList(filteredItems))
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
                    getStyleClass().remove("quick-action-cell");
                } else {
                    setText(item.name());
                    setGraphic(new FontIcon(item.icon()));
                    if (isQuickAction(item)) {
                        if (!getStyleClass().contains("quick-action-cell")) {
                            getStyleClass().add("quick-action-cell");
                        }
                    } else {
                        getStyleClass().remove("quick-action-cell");
                    }
                }
            }
        });

        listView.setOnMouseClicked(event -> {
            handleSelection(listView.getSelectionModel().getSelectedItem());
            hide();
        });

        listView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSelection(listView.getSelectionModel().getSelectedItem());
                hide();
            }
        });

        return listView;
    }

    private boolean isQuickAction(CardListItem item) {
        return item != null && item.id() != null && item.id().startsWith(QUICK_ACTION_PREFIX);
    }

    private void handleSelection(final CardListItem cardListItem) {
        if (chatTextArea.getText().startsWith(FIND_COMMAND_CHARACTER)) {
            chatTextArea.clear();
        }

        if (cardListItem != null && isQuickAction(cardListItem)) {
            String actionId = cardListItem.id().substring(QUICK_ACTION_PREFIX.length());
            QuickActionType actionType = QuickActionType.fromId(actionId);
            selectedQuickAction.setValue(null);
            selectedQuickAction.setValue(actionType);
        } else {
            selectedCommand.setValue(cardListItem);
        }
    }

    private void setupEscapeKeyHandler() {
        chatTextArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                selectedCommand.setValue(null);
                hide();
            }
        });
    }

    public ObjectProperty<CardListItem> getSelectedCommandProperty() {
        return selectedCommand;
    }

    public ObjectProperty<QuickActionType> getSelectedQuickActionProperty() {
        return selectedQuickAction;
    }

    public void handleTextChangeListener(final String text, final Stage stage) {
        Platform.runLater(() -> {
            commandListView.setPrefWidth(chatTextArea.getWidth());
            if (text.startsWith(FIND_COMMAND_CHARACTER)) {
                final String searchText = text.substring(1);
                updateItems(searchText);

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
