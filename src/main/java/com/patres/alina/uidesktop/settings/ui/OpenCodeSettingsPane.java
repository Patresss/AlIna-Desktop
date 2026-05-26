package com.patres.alina.uidesktop.settings.ui;

import atlantafx.base.theme.Styles;
import com.patres.alina.common.ai.AiProvider;
import com.patres.alina.common.ai.AiRuntimeStatus;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.uidesktop.backend.BackendApi;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTextSeparator;
import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTile;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizableRegion;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizableTextArea;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizableTextField;

/**
 * Settings pane for OpenCode runtime configuration and diagnostics.
 */
public class OpenCodeSettingsPane extends SettingsModalPaneContent {

    private ChoiceBox<String> chatModelSelector;
    private ChoiceBox<AiProvider> aiProviderSelector;
    private TextField openCodeHostnameField;
    private TextField openCodePortField;
    private TextField openCodeWorkingDirectoryField;
    private TextField codexCommandField;
    private TextField codexWorkingDirectoryField;
    private TextField codexSandboxField;
    private TextArea openCodeStatusArea;

    private WorkspaceSettings settings;
    private AssistantSettings assistantSettings;

    public OpenCodeSettingsPane(final Runnable backFunction) {
        super(backFunction);
    }

    @Override
    protected void reset() {
        settings = BackendApi.getWorkspaceSettings();
        assistantSettings = BackendApi.getAssistantSettings();
        aiProviderSelector.getItems().setAll(Arrays.asList(AiProvider.values()));
        aiProviderSelector.setValue(settings.aiProvider());
        openCodeHostnameField.setText(orEmpty(settings.openCodeHostname()));
        openCodePortField.setText(String.valueOf(settings.openCodePort()));
        openCodeWorkingDirectoryField.setText(orEmpty(settings.openCodeWorkingDirectory()));
        codexCommandField.setText(orEmpty(settings.codexCommand()));
        codexWorkingDirectoryField.setText(orEmpty(settings.codexWorkingDirectory()));
        codexSandboxField.setText(orEmpty(settings.codexSandbox()));

        final List<String> chatModels = BackendApi.getChatModels();
        chatModelSelector.getItems().setAll(chatModels);
        final String selectedModel = chatModels.contains(assistantSettings.resolveModelIdentifier())
                ? assistantSettings.resolveModelIdentifier()
                : chatModels.isEmpty() ? assistantSettings.resolveModelIdentifier() : chatModels.getFirst();
        chatModelSelector.setValue(selectedModel);

        refreshOpenCodeStatus();
    }

    @Override
    protected void save() {
        final WorkspaceSettings updated = new WorkspaceSettings(
                settings.showDashboard(),
                settings.dashboardCollapsed(),
                settings.keepWindowAlwaysOnTop(),
                settings.tasksFile(),
                settings.dashboardTaskLimit(),
                settings.taskGroups(),
                Optional.ofNullable(aiProviderSelector.getValue()).orElse(WorkspaceSettings.DEFAULT_AI_PROVIDER),
                openCodeHostnameField.getText(),
                parseInteger(openCodePortField.getText(), WorkspaceSettings.DEFAULT_OPENCODE_PORT),
                openCodeWorkingDirectoryField.getText(),
                codexCommandField.getText(),
                codexWorkingDirectoryField.getText(),
                codexSandboxField.getText(),
                settings.githubToken(),
                settings.dashboardTasksRefreshSeconds(),
                settings.dashboardGithubRefreshSeconds(),
                settings.dashboardMediaRefreshSeconds(),
                settings.dashboardGithubPrLimit(),
                settings.dashboardJiraRefreshSeconds(),
                settings.dashboardJiraIssueLimit(),
                settings.jiraEmail(),
                settings.jiraApiToken(),
                settings.showDashboardMusic(),
                settings.showDashboardTasks(),
                settings.showDashboardGithub(),
                settings.showDashboardJira(),
                settings.showDashboardCalendar(),
                settings.dashboardCalendarRefreshSeconds(),
                settings.calendarHideAllDayEvents(),
                settings.calendarShowOnlyCurrentAndFuture(),
                settings.calendarNotificationsEnabled(),
                settings.calendarNotificationMinutesBefore(),
                settings.calendarChangeNotificationsEnabled(),
                settings.githubChangeNotificationsEnabled(),
                settings.jiraChangeNotificationsEnabled(),
                settings.splitMode(),
                settings.calendarAiPrompt(),
                settings.tasksAiPrompt(),
                settings.jiraAiPrompt(),
                settings.githubAiPrompt(),
                settings.showDashboardObsidian(),
                settings.obsidianCliPath(),
                settings.dashboardObsidianNoteLimit(),
                settings.dashboardObsidianRefreshSeconds(),
                settings.obsidianChangeNotificationsEnabled(),
                settings.obsidianAiPrompt(),
                settings.obsidianExcludePatterns()
        );
        BackendApi.updateWorkspaceSettings(updated);
        settings = updated;

        final String chatModel = Optional.ofNullable(chatModelSelector)
                .map(ChoiceBox::getValue)
                .orElse(assistantSettings.resolveModelIdentifier());
        final AssistantSettings updatedAssistant = new AssistantSettings(chatModel);
        BackendApi.updateAssistantSettings(updatedAssistant);
        assistantSettings = updatedAssistant;

        refreshOpenCodeStatus();
    }

    @Override
    protected List<Node> generateContent() {
        settings = BackendApi.getWorkspaceSettings();
        assistantSettings = BackendApi.getAssistantSettings();

        final var header = createTextSeparator("settings.opencode.title", Styles.TITLE_3);

        final var providerHeader = createTextSeparator("settings.workspace.aiProvider.section", Styles.TITLE_4);
        aiProviderSelector = createResizableRegion(ChoiceBox::new, settingsBox);
        aiProviderSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(final AiProvider provider) {
                return provider == null ? "" : provider.displayName();
            }

            @Override
            public AiProvider fromString(final String value) {
                return AiProvider.from(value);
            }
        });
        final var providerTile = createTile("settings.workspace.aiProvider.title", "settings.workspace.aiProvider.description");
        providerTile.setAction(aiProviderSelector);

        // ── Chat model ──
        final var chatModelHeader = createTextSeparator("settings.chatModel.title", Styles.TITLE_4);
        chatModelSelector = createResizableRegion(ChoiceBox::new, settingsBox);
        final var chatModelTile = createTile("settings.chatModel.title", "settings.chatModel.description");
        chatModelTile.setAction(chatModelSelector);

        // ── Runtime ──
        final var runtimeHeader = createTextSeparator("settings.workspace.runtime.title", Styles.TITLE_4);
        final var openCodeHeader = createTextSeparator("settings.workspace.openCode.section", Styles.TITLE_4);
        final var codexHeader = createTextSeparator("settings.workspace.codex.section", Styles.TITLE_4);
        final var runtimeStatusHeader = createTextSeparator("settings.workspace.openCode.status.section", Styles.TITLE_4);

        openCodeHostnameField = createResizableTextField(settingsBox);
        openCodePortField = createResizableTextField(settingsBox);
        openCodeWorkingDirectoryField = createResizableTextField(settingsBox);
        codexCommandField = createResizableTextField(settingsBox);
        codexWorkingDirectoryField = createResizableTextField(settingsBox);
        codexSandboxField = createResizableTextField(settingsBox);

        openCodeStatusArea = createResizableTextArea(settingsBox);
        openCodeStatusArea.setPrefRowCount(8);
        openCodeStatusArea.setEditable(false);
        openCodeStatusArea.setWrapText(true);
        openCodeStatusArea.setFocusTraversable(false);

        final Button refreshOpenCodeStatusButton = createButton(Feather.REFRESH_CCW, e -> refreshOpenCodeStatus());
        final Node openCodeWorkingDirectoryPicker = createFilePickerField(openCodeWorkingDirectoryField, this::chooseOpenCodeWorkingDirectory);
        final Node codexWorkingDirectoryPicker = createFilePickerField(codexWorkingDirectoryField, this::chooseCodexWorkingDirectory);
        final VBox openCodeStatusBox = new VBox(8, openCodeStatusArea, refreshOpenCodeStatusButton);

        return List.of(
                header,
                providerHeader,
                providerTile,
                chatModelHeader,
                chatModelTile,
                runtimeHeader,
                openCodeHeader,
                tileFor(openCodeHostnameField, "settings.workspace.openCode.hostname.title", "settings.workspace.openCode.hostname.description"),
                tileFor(openCodePortField, "settings.workspace.openCode.port.title", "settings.workspace.openCode.port.description"),
                tileFor(openCodeWorkingDirectoryPicker, "settings.workspace.openCode.directory.title", "settings.workspace.openCode.directory.description"),
                codexHeader,
                tileFor(codexCommandField, "settings.workspace.codex.command.title", "settings.workspace.codex.command.description"),
                tileFor(codexWorkingDirectoryPicker, "settings.workspace.codex.directory.title", "settings.workspace.codex.directory.description"),
                tileFor(codexSandboxField, "settings.workspace.codex.sandbox.title", "settings.workspace.codex.sandbox.description"),
                runtimeStatusHeader,
                tileFor(openCodeStatusBox, "settings.workspace.openCode.status.title", "settings.workspace.openCode.status.description")
        );
    }

    private Node tileFor(final Node action, final String titleKey, final String descriptionKey) {
        final var tile = createTile(titleKey, descriptionKey);
        tile.setAction(action);
        return tile;
    }

    private Node createFilePickerField(final TextField field, final Runnable onPick) {
        final Button browseButton = createButton(Feather.FOLDER, e -> onPick.run());
        browseButton.setFocusTraversable(false);
        final HBox box = new HBox(8, field, browseButton);
        HBox.setHgrow(field, Priority.ALWAYS);
        return box;
    }

    private void chooseOpenCodeWorkingDirectory() {
        chooseWorkingDirectory(openCodeWorkingDirectoryField, "OpenCode working directory");
    }

    private void chooseCodexWorkingDirectory() {
        chooseWorkingDirectory(codexWorkingDirectoryField, "Codex working directory");
    }

    private void chooseWorkingDirectory(final TextField targetField, final String title) {
        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        applyInitialDirectory(chooser, targetField.getText());
        final File selected = chooser.showDialog(settingsBox.getScene() == null ? null : settingsBox.getScene().getWindow());
        if (selected != null) {
            targetField.setText(selected.getAbsolutePath());
        }
    }

    private void applyInitialDirectory(final DirectoryChooser chooser, final String currentValue) {
        final Path path = resolveExistingDirectory(currentValue);
        if (path != null) {
            chooser.setInitialDirectory(path.toFile());
        }
    }

    private Path resolveExistingDirectory(final String currentValue) {
        if (currentValue == null || currentValue.isBlank()) {
            return existingDirectory(Path.of(System.getProperty("user.home", ".")));
        }
        final Path path = Path.of(currentValue).toAbsolutePath().normalize();
        final Path directory = existingDirectory(path);
        return directory != null ? directory : existingDirectory(Path.of(System.getProperty("user.home", ".")));
    }

    private Path existingDirectory(final Path path) {
        if (path == null) {
            return null;
        }
        return Files.isDirectory(path) ? path : null;
    }

    private void refreshOpenCodeStatus() {
        try {
            final AiRuntimeStatus status = BackendApi.getAiRuntimeStatus();
            openCodeStatusArea.setText(formatOpenCodeStatus(status));
        } catch (Exception e) {
            openCodeStatusArea.setText("Status: unavailable" + System.lineSeparator()
                    + "Message: " + fallback(e.getMessage(), "Unknown error"));
        }
    }

    private String formatOpenCodeStatus(final AiRuntimeStatus status) {
        if (status == null) {
            return "Status unavailable.";
        }
        final List<String> lines = new ArrayList<>();
        lines.add("Runtime: " + fallback(status.displayName(), status.provider() == null ? "-" : status.provider().displayName()));
        lines.add("Status: " + (status.healthy() ? "running" : "unreachable"));
        lines.add("Version: " + fallback(status.version(), "-"));
        lines.add("Command: " + fallback(status.command(), "-"));
        if (status.baseUrl() != null && !status.baseUrl().isBlank()) {
            lines.add("Base URL: " + status.baseUrl());
            lines.add("Host: " + fallback(status.hostname(), "-"));
            lines.add("Port: " + status.port());
        }
        lines.add("Working directory: " + fallback(status.workingDirectory(), "-"));
        lines.add("Working directory exists: " + yesNo(status.workingDirectoryExists()));
        lines.add("Process started by AlIna: " + yesNo(status.processRunning()));
        lines.add("Message: " + fallback(status.statusMessage(), "-"));
        return String.join(System.lineSeparator(), lines);
    }

    private String orEmpty(final String value) {
        return value == null ? "" : value;
    }

    private String fallback(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String yesNo(final boolean value) {
        return value ? "yes" : "no";
    }

    private int parseInteger(final String raw, final int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
