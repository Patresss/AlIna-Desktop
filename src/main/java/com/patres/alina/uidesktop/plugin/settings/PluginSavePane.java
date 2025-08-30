package com.patres.alina.uidesktop.plugin.settings;

import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
import com.patres.alina.uidesktop.ui.atlantafx.CustomTile;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;

import static com.patres.alina.uidesktop.ui.language.LanguageManager.getLanguageString;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.*;

public abstract class PluginSavePane extends ApplicationModalPaneContent {

    protected TextField pluginNameTextField;
    protected TextArea pluginDescriptionTextArea;
    protected TextArea pluginSystemPromptTextArea;
    protected ComboBox<AutoCompleteComboBox.HideableItem<ApplicationIcon>> iconComboBox;

    public PluginSavePane(Runnable backFunction) {
        super(backFunction);
    }

    @FXML
    public void initialize() {
        super.initialize();
        final Button saveButton = createButton(Feather.SAVE, e -> savePlugin());
        buttonBar.getButtons().add(saveButton);
    }

    protected abstract void savePlugin();

    @Override
    protected List<Node> generateContent() {
        var pluginNameTile = new CustomTile(
                getLanguageString("plugin.name.title"),
                null
        );
        pluginNameTextField = createResizableTextField(settingsBox);
        pluginNameTile.setAction(pluginNameTextField);

        var pluginDescriptionTile = new CustomTile(
                getLanguageString("plugin.description.title"),
                getLanguageString("plugin.description.description")
        );
        pluginDescriptionTextArea = createResizableTextArea(settingsBox);
        pluginDescriptionTile.setAction(pluginDescriptionTextArea);

        var pluginSystemPromptTile = new CustomTile(
                getLanguageString("plugin.systemPrompt.title"),
                getLanguageString("plugin.systemPrompt.description")
        );
        pluginSystemPromptTextArea = createResizableTextArea(settingsBox);
        pluginSystemPromptTile.setAction(pluginSystemPromptTextArea);

        iconComboBox = createResizableRegion(IconComboBox::create, settingsBox);


        var iconTile = new CustomTile(
                getLanguageString("plugin.icon.title"),
                getLanguageString("plugin.icon.description")
        );
        iconTile.setAction(iconComboBox);

        return List.of(pluginNameTile, pluginDescriptionTile, pluginSystemPromptTile, iconTile, new Separator());
    }
}
