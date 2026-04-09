package com.patres.alina.uidesktop.settings.ui;

import atlantafx.base.theme.Styles;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.uidesktop.backend.BackendApi;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.util.List;
import java.util.Optional;

import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTextSeparator;
import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTile;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.*;

public class AssistantSettingsPane extends SettingsModalPaneContent {

    private ChoiceBox<String> chatModelSelector;

    private AssistantSettings settings;

    public AssistantSettingsPane(Runnable backFunction) {
        super(backFunction);
    }

    @Override
    protected void reset() {
        loadDataFromSettings();

        List<String> chatModels = BackendApi.getChatModels();
        loadDataFromSettings();
        chatModelSelector.getItems().setAll(chatModels);

        final String selectedModel = chatModels.contains(settings.resolveModelIdentifier())
                ? settings.resolveModelIdentifier()
                : chatModels.isEmpty() ? settings.resolveModelIdentifier() : chatModels.getFirst();
        chatModelSelector.setValue(selectedModel);
    }

    @Override
    protected void save() {
        final String chatModel = Optional.ofNullable(chatModelSelector)
                .map(ChoiceBox::getValue)
                .orElse(settings.resolveModelIdentifier());

        final AssistantSettings assistantSettings = new AssistantSettings(chatModel);
        BackendApi.updateAssistantSettings(assistantSettings);
    }

    private void loadDataFromSettings() {
        settings = BackendApi.getAssistantSettings();
    }

    @Override
    protected List<Node> generateContent() {
        loadDataFromSettings();
        var header = createTextSeparator("settings.assistant.title", Styles.TITLE_3);

        var chatModel = createTile(
                "settings.chatModel.title",
                "settings.chatModel.description"
        );

        chatModelSelector = createResizableRegion(ChoiceBox::new, settingsBox);
        chatModel.setAction(chatModelSelector);

        return List.of(header, chatModel);
    }

}
