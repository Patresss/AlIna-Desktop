package com.patres.alina.uidesktop.command.settings;

import atlantafx.base.controls.ToggleSwitch;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeyPane;
import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeys;
import com.patres.alina.uidesktop.ui.atlantafx.CustomTile;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.kordamp.ikonli.feather.Feather;

import java.util.ArrayList;
import java.util.List;

import static com.patres.alina.uidesktop.ui.language.LanguageManager.getLanguageString;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.*;

public abstract class CommandSavePane extends ApplicationModalPaneContent {

    protected TextField commandNameTextField;
    protected TextArea commandDescriptionTextArea;
    protected TextArea commandSystemPromptTextArea;
    protected ChoiceBox<String> commandModelChoiceBox;
    protected ComboBox<AutoCompleteComboBox.HideableItem<ApplicationIcon>> iconComboBox;
    protected ToggleSwitch showInChatToggleSwitch;
    protected ToggleSwitch showInContextMenuPasteToggleSwitch;
    protected ToggleSwitch showInContextMenuDisplayToggleSwitch;
    protected ToggleSwitch showInContextMenuExecuteToggleSwitch;
    protected ShortcutKeyPane pasteShortcutKeyPane;
    protected ShortcutKeyPane displayShortcutKeyPane;
    protected ShortcutKeyPane executeShortcutKeyPane;

    public CommandSavePane(Runnable backFunction) {
        super(backFunction);
    }

    @FXML
    public void initialize() {
        super.initialize();
        final Button saveButton = createButton(Feather.SAVE, e -> saveCommand());
        buttonBar.getButtons().add(saveButton);
    }

    protected abstract void saveCommand();

    @Override
    protected List<Node> generateContent() {
        var commandNameTile = new CustomTile(
                getLanguageString("command.name.title"),
                null
        );
        commandNameTextField = createResizableTextField(settingsBox);
        commandNameTile.setAction(commandNameTextField);

        var commandDescriptionTile = new CustomTile(
                getLanguageString("command.description.title"),
                getLanguageString("command.description.description")
        );
        commandDescriptionTextArea = createResizableTextArea(settingsBox);
        commandDescriptionTile.setAction(commandDescriptionTextArea);

        var commandSystemPromptTile = new CustomTile(
                getLanguageString("command.systemPrompt.title"),
                getLanguageString("command.systemPrompt.description")
        );
        commandSystemPromptTextArea = createResizableTextArea(settingsBox);
        commandSystemPromptTile.setAction(commandSystemPromptTextArea);

        var commandModelTile = new CustomTile(
                getLanguageString("command.model.title"),
                getLanguageString("command.model.description")
        );
        commandModelChoiceBox = createResizableRegion(ChoiceBox::new, settingsBox);
        loadAvailableModels();
        commandModelTile.setAction(commandModelChoiceBox);

        iconComboBox = createResizableRegion(IconComboBox::create, settingsBox);
        var iconTile = new CustomTile(
                getLanguageString("command.icon.title"),
                getLanguageString("command.icon.description")
        );
        iconTile.setAction(iconComboBox);

        showInChatToggleSwitch = new ToggleSwitch();
        var showInChatTile = new CustomTile(
                getLanguageString("command.visibility.chat.title"),
                getLanguageString("command.visibility.chat.description")
        );
        showInChatTile.setAction(showInChatToggleSwitch);
        showInChatTile.setActionHandler(showInChatToggleSwitch::fire);

        showInContextMenuPasteToggleSwitch = new ToggleSwitch();
        var showInContextMenuPasteTile = new CustomTile(
                getLanguageString("command.visibility.contextmenu.paste.title"),
                getLanguageString("command.visibility.contextmenu.paste.description")
        );
        showInContextMenuPasteTile.setAction(showInContextMenuPasteToggleSwitch);
        showInContextMenuPasteTile.setActionHandler(showInContextMenuPasteToggleSwitch::fire);

        showInContextMenuDisplayToggleSwitch = new ToggleSwitch();
        var showInContextMenuDisplayTile = new CustomTile(
                getLanguageString("command.visibility.contextmenu.display.title"),
                getLanguageString("command.visibility.contextmenu.display.description")
        );
        showInContextMenuDisplayTile.setAction(showInContextMenuDisplayToggleSwitch);
        showInContextMenuDisplayTile.setActionHandler(showInContextMenuDisplayToggleSwitch::fire);

        showInContextMenuExecuteToggleSwitch = new ToggleSwitch();
        var showInContextMenuExecuteTile = new CustomTile(
                getLanguageString("command.visibility.contextmenu.execute.title"),
                getLanguageString("command.visibility.contextmenu.execute.description")
        );
        showInContextMenuExecuteTile.setAction(showInContextMenuExecuteToggleSwitch);
        showInContextMenuExecuteTile.setActionHandler(showInContextMenuExecuteToggleSwitch::fire);

        pasteShortcutKeyPane = new ShortcutKeyPane();
        var pasteShortcutTile = new CustomTile(
                getLanguageString("command.shortcut.paste.title"),
                getLanguageString("command.shortcut.paste.description")
        );
        pasteShortcutTile.setAction(pasteShortcutKeyPane.createPane());

        displayShortcutKeyPane = new ShortcutKeyPane();
        var displayShortcutTile = new CustomTile(
                getLanguageString("command.shortcut.display.title"),
                getLanguageString("command.shortcut.display.description")
        );
        displayShortcutTile.setAction(displayShortcutKeyPane.createPane());

        executeShortcutKeyPane = new ShortcutKeyPane();
        var executeShortcutTile = new CustomTile(
                getLanguageString("command.shortcut.execute.title"),
                getLanguageString("command.shortcut.execute.description")
        );
        executeShortcutTile.setAction(executeShortcutKeyPane.createPane());

        return List.of(
                commandNameTile,
                commandDescriptionTile,
                commandSystemPromptTile,
                commandModelTile,
                iconTile,
                new Separator(),
                showInChatTile,
                showInContextMenuPasteTile,
                showInContextMenuDisplayTile,
                showInContextMenuExecuteTile,
                new Separator(),
                pasteShortcutTile,
                displayShortcutTile,
                executeShortcutTile,
                new Separator()
        );
    }

    protected void resetVisibilityAndShortcuts() {
        showInChatToggleSwitch.setSelected(true);
        showInContextMenuPasteToggleSwitch.setSelected(true);
        showInContextMenuDisplayToggleSwitch.setSelected(true);
        showInContextMenuExecuteToggleSwitch.setSelected(false);
        pasteShortcutKeyPane.setValues(new ShortcutKeys());
        displayShortcutKeyPane.setValues(new ShortcutKeys());
        executeShortcutKeyPane.setValues(new ShortcutKeys());
    }

    protected void loadAvailableModels() {
        final String defaultLabel = getLanguageString("command.model.default");
        final List<String> items = new ArrayList<>();
        items.add(defaultLabel);
        try {
            items.addAll(BackendApi.getChatModels());
        } catch (Exception ignored) {
        }
        commandModelChoiceBox.getItems().setAll(items);
    }

    protected void setSelectedModel(final String model) {
        final String defaultLabel = getLanguageString("command.model.default");
        if (model == null || model.isBlank()) {
            commandModelChoiceBox.setValue(defaultLabel);
        } else {
            if (!commandModelChoiceBox.getItems().contains(model)) {
                commandModelChoiceBox.getItems().add(model);
            }
            commandModelChoiceBox.setValue(model);
        }
    }

    protected String getSelectedModel() {
        final String defaultLabel = getLanguageString("command.model.default");
        final String value = commandModelChoiceBox.getValue();
        if (value == null || value.equals(defaultLabel)) {
            return "";
        }
        return value;
    }
}
