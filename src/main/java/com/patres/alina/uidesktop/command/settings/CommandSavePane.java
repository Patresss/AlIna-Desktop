package com.patres.alina.uidesktop.command.settings;

import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
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

        return List.of(commandNameTile, commandDescriptionTile, commandSystemPromptTile, iconTile, new Separator());
    }
}
