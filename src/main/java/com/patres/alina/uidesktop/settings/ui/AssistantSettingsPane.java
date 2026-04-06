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
    private PasswordTextField anthropicApiKeyTextField;
    private PasswordTextField googleApiKeyTextField;
    private TextField timeoutTextField;

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

        chatContextTextArea.setText(settings.systemPrompt());
        final String selectedModel = chatModels.contains(settings.resolveModelIdentifier())
                ? settings.resolveModelIdentifier()
                : chatModels.isEmpty() ? settings.resolveModelIdentifier() : chatModels.getFirst();
        chatModelSelector.setValue(selectedModel);
        numberOfMessagesInContextSpinner.getValueFactory().setValue(settings.numberOfMessagesInContext());
        openAiApiKeyTextField.setText(settings.openAiApiKey());
        anthropicApiKeyTextField.setText(settings.anthropicApiKey() != null ? settings.anthropicApiKey() : "");
        googleApiKeyTextField.setText(settings.googleApiKey() != null ? settings.googleApiKey() : "");
    }

    @Override
    protected void save() {
        final String context = Optional.ofNullable(chatContextTextArea)
                .map(TextInputControl::getText)
                .orElse(null);
        final String chatModel = Optional.ofNullable(chatModelSelector)
                .map(ChoiceBox::getValue)
                .orElse(settings.resolveModelIdentifier());
        final String openAiApiKey = Optional.ofNullable(openAiApiKeyTextField)
                .map(PasswordTextField::getPassword)
                .orElse(null);
        final String anthropicApiKey = Optional.ofNullable(anthropicApiKeyTextField)
                .map(PasswordTextField::getPassword)
                .filter(s -> !s.isBlank())
                .orElse(null);
        final String googleApiKey = Optional.ofNullable(googleApiKeyTextField)
                .map(PasswordTextField::getPassword)
                .filter(s -> !s.isBlank())
                .orElse(null);
        final Integer numberOfMessagesInContext = Optional.ofNullable(numberOfMessagesInContextSpinner)
                .map(Spinner::getValue)
                .orElse(-1);
        final Integer timeout = Optional.ofNullable(timeoutTextField)
                .map(TextInputControl::getText)
                .map(Integer::valueOf)
                .orElse(-1);

        final AssistantSettings assistantSettings = new AssistantSettings(
                chatModel, context, numberOfMessagesInContext,
                openAiApiKey, anthropicApiKey, googleApiKey,
                timeout
        );
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
        addRevealToggle(openAiApiKeyTextField);
        openAiApiKeyTextTile.setAction(openAiApiKeyTextField);

        var anthropicApiKeyTextTile = createTile(
                "settings.anthropicApiKey.title",
                null
        );
        anthropicApiKeyTextField = createResizableRegion(PasswordTextField::new, settingsBox);
        addRevealToggle(anthropicApiKeyTextField);
        anthropicApiKeyTextTile.setAction(anthropicApiKeyTextField);

        var googleApiKeyTextTile = createTile(
                "settings.googleApiKey.title",
                null
        );
        googleApiKeyTextField = createResizableRegion(PasswordTextField::new, settingsBox);
        addRevealToggle(googleApiKeyTextField);
        googleApiKeyTextTile.setAction(googleApiKeyTextField);


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

        return List.of(header, chatModel, openAiApiKeyTextTile, anthropicApiKeyTextTile, googleApiKeyTextTile,
                chatContext, numberOfMessagesInContextTile, timeoutTile);
    }

    private void addRevealToggle(PasswordTextField field) {
        var icon = new FontIcon(Feather.EYE_OFF);
        icon.setCursor(Cursor.HAND);
        icon.setOnMouseClicked(e -> {
            icon.setIconCode(field.getRevealPassword()
                    ? Feather.EYE_OFF : Feather.EYE
            );
            field.setRevealPassword(!field.getRevealPassword());
        });
        field.setRight(icon);
    }

}
