package com.patres.alina.uidesktop.chat.thread.ui;

import atlantafx.base.controls.Card;
import atlantafx.base.controls.Tile;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import com.patres.alina.common.thread.ChatThreadRenameRequest;
import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.atlantafx.CustomTile;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;

import static com.patres.alina.uidesktop.ui.language.LanguageManager.getLanguageString;

public class ChatThreadHistoryCard extends Card {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ChatThread chatThread;
    private final ChatThreadHistoryPane chatThreadHistoryPane;

    public ChatThreadHistoryCard(final ChatThread chatThread, final ChatThreadHistoryPane chatThreadHistoryPane) {
        this.chatThread = chatThread;
        this.chatThreadHistoryPane = chatThreadHistoryPane;
        initializeCard();
    }

    private void initializeCard() {
        final Tile tile = createTile();
        setHeader(tile);
        setCardStyle();
        setOnClickActions(tile);
    }

    private Tile createTile() {
        final Tile tile = new CustomTile(chatThread.name(), DATE_FORMATTER.format(chatThread.createdAt()));
        tile.setAction(createSettingsButton(tile));
        return tile;
    }

    private MenuButton createSettingsButton(final Tile tile) {
        final MenuButton settingsButton = new MenuButton(null, new FontIcon(Feather.MORE_VERTICAL));
        settingsButton.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT, Tweaks.NO_ARROW);
        settingsButton.getItems().addAll(createRenameMenuItem(tile), createDeleteMenuItem());
        return settingsButton;
    }

    private MenuItem createRenameMenuItem(final Tile tile) {
        final MenuItem rename = new MenuItem(getLanguageString("chat.thread.history.rename"), new FontIcon(Feather.EDIT));
        rename.setOnAction(e -> setHeader(createRenameTextField(tile)));
        return rename;
    }

    private TextField createRenameTextField(final Tile tile) {
        final TextField renameTextField = new TextField(chatThread.name());
        renameTextField.setPromptText(chatThread.name());
        renameTextField.getStyleClass().addAll(Styles.LARGE);
        renameTextField.setOnKeyPressed(event -> handleRenameTextFieldKeyPress(event, tile, renameTextField));
        return renameTextField;
    }

    private void handleRenameTextFieldKeyPress(final javafx.scene.input.KeyEvent event, final Tile tile, final TextField renameTextField) {
        if (event.getCode() == KeyCode.ENTER) {
            final String newName = renameTextField.getText();
            tile.setTitle(newName);
            setHeader(tile);
            BackendApi.renameChatThread(new ChatThreadRenameRequest(chatThread.id(), newName));
        }
        if (event.getCode() == KeyCode.ESCAPE) {
            setHeader(tile);
        }
    }

    private MenuItem createDeleteMenuItem() {
        final MenuItem delete = new MenuItem(getLanguageString("chat.thread.history.delete"), new FontIcon(Feather.TRASH));
        delete.setOnAction(e -> removeChatThreadCard());
        return delete;
    }

    private void removeChatThreadCard() {
        chatThreadHistoryPane.removeChatThreadCard(this);
        BackendApi.deleteChatThread(chatThread.id());
    }

    private void setCardStyle() {
        getStyleClass().add(Styles.INTERACTIVE);
        setPrefWidth(Integer.MAX_VALUE);
        setMaxWidth(Integer.MAX_VALUE);
    }

    private void setOnClickActions(final Tile tile) {
        setOnMouseClicked(e -> chatThreadHistoryPane.selectChatThread(chatThread));
        tile.setOnMouseClicked(e -> chatThreadHistoryPane.selectChatThread(chatThread));
    }

    public ChatThread getChatThread() {
        return chatThread;
    }
}
