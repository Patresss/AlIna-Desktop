package com.patres.alina.uidesktop.settings.ui;

import atlantafx.base.controls.PasswordTextField;
import atlantafx.base.theme.Styles;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.uidesktop.backend.BackendApi;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.util.converter.IntegerStringConverter;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTextSeparator;
import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTile;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.*;

public class AssistantSettingsPane extends SettingsModalPaneContent {


    private TextArea chatContextTextArea;
    private ChoiceBox<String> chatModelSelector;
    private Spinner<Integer> numberOfMessagesInContextSpinner;
    private PasswordTextField openAiApiKeyTextField;
    private TextField timeoutTextField;

    private AssistantSettings settings;

    public AssistantSettingsPane(Runnable backFunction) {
        super(backFunction);
    }

    @Override
    protected void reset() {
        loadDataFromSettings();

        List<String> chatModels = BackendApi.getChatModels();
        chatModelSelector.getItems().setAll(chatModels);

        chatContextTextArea.setText(settings.systemPrompt());
        chatModelSelector.setValue(settings.chatModel());
        numberOfMessagesInContextSpinner.getValueFactory().setValue(settings.numberOfMessagesInContext());
        openAiApiKeyTextField.setText(settings.openAiApiKey());
    }

    @Override
    protected void save() {
        final String context = Optional.ofNullable(chatContextTextArea)
                .map(TextInputControl::getText)
                .orElse(null);
        final String chatModel = Optional.ofNullable(chatModelSelector)
                .map(ChoiceBox::getValue)
                .orElse(null);
        final String openAiApiKey = Optional.ofNullable(openAiApiKeyTextField)
                .map(PasswordTextField::getPassword)
                .orElse(null);
        final Integer numberOfMessagesInContext = Optional.ofNullable(numberOfMessagesInContextSpinner)
                .map(Spinner::getValue)
                .orElse(-1);
        final Integer timeout = Optional.ofNullable(timeoutTextField)
                .map(TextInputControl::getText)
                .map(Integer::valueOf)
                .orElse(-1);

        final AssistantSettings assistantSettings = new AssistantSettings(chatModel, context, numberOfMessagesInContext, openAiApiKey, timeout);
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

        chatModelSelector = createResizableRegion(ChoiceBox::new, settingsBox); ;
        chatModel.setAction(chatModelSelector);

        var chatContext = createTile(
                "settings.context.title",
                "settings.context.description"
        );
        chatContextTextArea = createResizableTextArea(settingsBox);
        chatContext.setAction(chatContextTextArea);


        var numberOfMessagesInContextTile = createTile(
                "settings.numberOfMessagesInContext.title",
                "settings.numberOfMessagesInContext.description"
        );
        numberOfMessagesInContextSpinner = createResizableRegion(() -> new Spinner<>(0, 100, 5), settingsBox);
        numberOfMessagesInContextTile.setAction(numberOfMessagesInContextSpinner);


        var openAiApiKeyTextTile = createTile(
                "settings.openAiApiKey.title",
                null
        );

        openAiApiKeyTextField = createResizableRegion(PasswordTextField::new, settingsBox);
        var icon = new FontIcon(Feather.EYE_OFF);
        icon.setCursor(Cursor.HAND);
        icon.setOnMouseClicked(e -> {
            icon.setIconCode(openAiApiKeyTextField.getRevealPassword()
                    ? Feather.EYE_OFF : Feather.EYE
            );
            openAiApiKeyTextField.setRevealPassword(!openAiApiKeyTextField.getRevealPassword());
        });
        openAiApiKeyTextField.setRight(icon);
        openAiApiKeyTextTile.setAction(openAiApiKeyTextField);


        var timeoutTile = createTile(
                "settings.timeout.title",
                "settings.timeout.description"
        );

        timeoutTextField = createResizableTextField(settingsBox); ;
        timeoutTile.setAction(timeoutTextField);

        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("([1-9][0-9]*)?")) {
                return change;
            }
            return null;
        };

        timeoutTextField.setTextFormatter(
                new TextFormatter<>(new IntegerStringConverter(), 120, integerFilter));

        return List.of(header, chatModel, openAiApiKeyTextTile, chatContext, numberOfMessagesInContextTile, timeoutTile);
    }


}
