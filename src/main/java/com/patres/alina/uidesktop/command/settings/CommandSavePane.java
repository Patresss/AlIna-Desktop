package com.patres.alina.uidesktop.command.settings;

import atlantafx.base.controls.ToggleSwitch;
import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeyPane;
import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeys;
import com.patres.alina.uidesktop.ui.atlantafx.CustomTile;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;

import static com.patres.alina.uidesktop.ui.language.LanguageManager.getLanguageString;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.*;

public abstract class CommandSavePane extends ApplicationModalPaneContent {

    protected TextField commandNameTextField;
    protected TextArea commandDescriptionTextArea;
    protected TextArea commandSystemPromptTextArea;
    protected ComboBox<AutoCompleteComboBox.HideableItem<ApplicationIcon>> iconComboBox;
    protected ToggleSwitch showInChatToggleSwitch;
    protected ToggleSwitch showInContextMenuPasteToggleSwitch;
    protected ToggleSwitch showInContextMenuDisplayToggleSwitch;
    protected ShortcutKeyPane pasteShortcutKeyPane;
    protected ShortcutKeyPane displayShortcutKeyPane;

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

        return List.of(
                commandNameTile,
                commandDescriptionTile,
                commandSystemPromptTile,
                iconTile,
                new Separator(),
                showInChatTile,
                showInContextMenuPasteTile,
                showInContextMenuDisplayTile,
                new Separator(),
                pasteShortcutTile,
                displayShortcutTile,
                new Separator()
        );
    }

    protected void resetVisibilityAndShortcuts() {
        showInChatToggleSwitch.setSelected(true);
        showInContextMenuPasteToggleSwitch.setSelected(true);
        showInContextMenuDisplayToggleSwitch.setSelected(true);
        pasteShortcutKeyPane.setValues(new ShortcutKeys());
        displayShortcutKeyPane.setValues(new ShortcutKeys());
    }
}
