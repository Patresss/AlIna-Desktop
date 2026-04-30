package com.patres.alina.uidesktop.ui.dashboard;

import atlantafx.base.theme.Styles;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.common.tracking.DashboardChangeTracker;
import com.patres.alina.common.tracking.DashboardSection;
import com.patres.alina.common.tracking.TrackableItem;
import com.patres.alina.server.integration.ObsidianCliService;
import com.patres.alina.server.integration.ObsidianNote;
import com.patres.alina.server.integration.ObsidianNotesResult;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.util.EmojiLabelHelper;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard widget displaying recently edited Obsidian notes.
 * Uses the obsidian-cli to fetch the list of recent notes.
 */
public class ObsidianWidget extends VBox {

    private static final String STYLE_DASHBOARD = "workspace-dashboard";
    private static final String STYLE_DASHBOARD_TITLE = "workspace-dashboard-title";
    private static final String STYLE_DASHBOARD_COUNT = "workspace-dashboard-count";
    private static final String STYLE_DASHBOARD_HEADER = "workspace-dashboard-header";
    private static final String STYLE_DASHBOARD_CONTENT = "workspace-dashboard-content";
    private static final String STYLE_DASHBOARD_EMPTY = "workspace-dashboard-empty";
    private static final String STYLE_TASK_LIST = "workspace-task-list";
    private static final String STYLE_COLLAPSE_BUTTON = "workspace-collapse-button";
    private static final String STYLE_OBSIDIAN_ITEM = "workspace-obsidian-item";
    private static final String STYLE_OBSIDIAN_NAME = "workspace-obsidian-name";
    private static final String STYLE_OBSIDIAN_FOLDER = "workspace-obsidian-folder";
    private static final String STYLE_OBSIDIAN_DATE = "workspace-obsidian-date";
    private static final String STYLE_OBSIDIAN_ERROR = "workspace-calendar-error";

    private final Label titleLabel = new Label();
    private final Label countLabel = new Label();
    private final Button collapseButton = new Button();
    private final VBox contentBox = new VBox(2);
    private final VBox detailsBox = new VBox(2);

    private boolean collapsed = false;
    private Timeline refreshTimeline;

    public ObsidianWidget() {
        getStyleClass().add(STYLE_DASHBOARD);

        final FontIcon obsidianIcon = new FontIcon(Feather.BOOK_OPEN);
        obsidianIcon.getStyleClass().add(STYLE_DASHBOARD_TITLE);
        titleLabel.textProperty().bind(LanguageManager.createStringBinding("dashboard.obsidian.title"));
        titleLabel.setGraphic(obsidianIcon);
        titleLabel.setGraphicTextGap(5);
        titleLabel.getStyleClass().add(STYLE_DASHBOARD_TITLE);

        countLabel.getStyleClass().add(STYLE_DASHBOARD_COUNT);
        countLabel.setManaged(false);
        countLabel.setVisible(false);

        collapseButton.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT, STYLE_COLLAPSE_BUTTON);
        collapseButton.setOnAction(event -> toggleCollapsed());

        contentBox.getStyleClass().add(STYLE_TASK_LIST);
        detailsBox.getStyleClass().add(STYLE_DASHBOARD_CONTENT);

        final HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final HBox header = new HBox(8, titleLabel, countLabel, spacer, collapseButton);
        header.getStyleClass().add(STYLE_DASHBOARD_HEADER);

        detailsBox.getChildren().add(contentBox);

        setSpacing(2);
        getChildren().addAll(header, detailsBox);

        updateCollapseButton();
        renderEmpty();

        setManaged(false);
        setVisible(false);
    }

    public void refresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        final int refreshSeconds = BackendApi.getWorkspaceSettings().dashboardObsidianRefreshSeconds();
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(refreshSeconds), event -> refreshAsync()));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
        refreshAsync();
    }

    private void refreshAsync() {
        Thread.startVirtualThread(() -> {
            final WorkspaceSettings settings = BackendApi.getWorkspaceSettings();
            final String cliPath = settings.obsidianCliPath();
            final String vaultName = settings.obsidianVaultName();
            final int limit = settings.dashboardObsidianNoteLimit();
            final String excludePatterns = settings.obsidianExcludePatterns();

            final ObsidianNotesResult result = ObsidianCliService.fetchRecentNotes(cliPath, vaultName, limit, excludePatterns);

            if (!result.cliMissing() && result.errorMessage().isEmpty()) {
                trackChanges(result.notes());
            }

            Platform.runLater(() -> render(result));
        });
    }

    // ── Rendering ────────────────────────────────────────────────

    private void render(final ObsidianNotesResult result) {
        setManaged(true);
        setVisible(true);

        contentBox.getChildren().clear();

        if (result.cliMissing()) {
            renderError(LanguageManager.getLanguageString("dashboard.obsidian.cliMissing"));
            return;
        }

        if (!result.errorMessage().isEmpty()) {
            renderError(result.errorMessage());
            return;
        }

        final int noteCount = result.notes().size();
        countLabel.setText(String.valueOf(noteCount));
        countLabel.setManaged(noteCount > 0);
        countLabel.setVisible(noteCount > 0);

        detailsBox.setManaged(!collapsed);
        detailsBox.setVisible(!collapsed);

        if (result.notes().isEmpty()) {
            renderEmpty();
            return;
        }

        for (final ObsidianNote note : result.notes()) {
            contentBox.getChildren().add(createNoteRow(note));
        }
    }

    private void renderEmpty() {
        contentBox.getChildren().clear();
        final Label emptyLabel = new Label(LanguageManager.getLanguageString("dashboard.obsidian.empty"));
        emptyLabel.getStyleClass().add(STYLE_DASHBOARD_EMPTY);
        contentBox.getChildren().add(emptyLabel);
    }

    private void renderError(final String message) {
        contentBox.getChildren().clear();
        final Label errorLabel = new Label(message);
        errorLabel.getStyleClass().add(STYLE_OBSIDIAN_ERROR);
        errorLabel.setWrapText(true);
        contentBox.getChildren().add(errorLabel);
    }

    // ── Note row ─────────────────────────────────────────────────

    private HBox createNoteRow(final ObsidianNote note) {
        final Label nameLabel = new Label();
        EmojiLabelHelper.applyEmojiText(nameLabel, note.name());
        nameLabel.getStyleClass().add(STYLE_OBSIDIAN_NAME);
        nameLabel.setWrapText(false);
        nameLabel.setOnMouseClicked(e -> openNoteInObsidian(note));

        final Region rowSpacer = new Region();
        HBox.setHgrow(rowSpacer, Priority.ALWAYS);

        final String aiPrompt = BackendApi.getWorkspaceSettings().obsidianAiPrompt();
        final Region aiSlot = DashboardAiButton.createSlot(aiPrompt, buildNoteArguments(note));

        final Label dateLabel = new Label(formatDate(note.lastModified()));
        dateLabel.getStyleClass().add(STYLE_OBSIDIAN_DATE);
        dateLabel.setWrapText(false);
        dateLabel.setMinWidth(Region.USE_PREF_SIZE);

        final HBox row;
        if (!note.folder().isEmpty()) {
            final Label folderLabel = new Label(note.folder());
            folderLabel.getStyleClass().add(STYLE_OBSIDIAN_FOLDER);
            folderLabel.setWrapText(false);
            folderLabel.setMinWidth(Region.USE_PREF_SIZE);
            row = new HBox(6, nameLabel, folderLabel, rowSpacer, dateLabel, aiSlot);
        } else {
            row = new HBox(6, nameLabel, rowSpacer, dateLabel, aiSlot);
        }

        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(STYLE_OBSIDIAN_ITEM);
        row.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(row, Priority.ALWAYS);
        return row;
    }

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd (HH:mm)");

    private String formatDate(final Instant instant) {
        if (instant == null || instant.equals(Instant.EPOCH)) {
            return "";
        }
        final LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return dateTime.format(DATE_TIME_FORMAT);
    }

    private void openNoteInObsidian(final ObsidianNote note) {
        final WorkspaceSettings settings = BackendApi.getWorkspaceSettings();
        Thread.startVirtualThread(() ->
                ObsidianCliService.openNote(settings.obsidianCliPath(), settings.obsidianVaultName(), note.path())
        );
    }

    private String buildNoteArguments(final ObsidianNote note) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Note: ").append(note.name());
        if (!note.folder().isEmpty()) {
            sb.append("\nFolder: ").append(note.folder());
        }
        sb.append("\nPath: ").append(note.path());
        return sb.toString();
    }

    // ── Change tracking ──────────────────────────────────────────

    private void trackChanges(final List<ObsidianNote> notes) {
        final boolean enabled = BackendApi.getWorkspaceSettings().obsidianChangeNotificationsEnabled();
        DashboardChangeTracker.getInstance().trackChanges(
                DashboardSection.OBSIDIAN,
                notes,
                ObsidianWidget::toTrackableItem,
                enabled,
                false
        );
    }

    static TrackableItem toTrackableItem(final ObsidianNote note) {
        final String key = note.path();
        final String displayName = note.name();
        final Map<String, String> fields = new LinkedHashMap<>();
        fields.put("name", note.name());
        fields.put("folder", note.folder());
        fields.put("path", note.path());
        return new TrackableItem(key, displayName, fields);
    }

    // ── Collapse toggle ──────────────────────────────────────────

    private void toggleCollapsed() {
        collapsed = !collapsed;
        updateCollapseButton();
        updateCollapsedStyle();
        detailsBox.setManaged(!collapsed);
        detailsBox.setVisible(!collapsed);
    }

    private void updateCollapseButton() {
        collapseButton.setText(null);
        collapseButton.setGraphic(new FontIcon(collapsed ? Feather.CHEVRON_DOWN : Feather.CHEVRON_UP));
    }

    private void updateCollapsedStyle() {
        if (collapsed) {
            if (!getStyleClass().contains("workspace-dashboard-collapsed")) {
                getStyleClass().add("workspace-dashboard-collapsed");
            }
        } else {
            getStyleClass().remove("workspace-dashboard-collapsed");
        }
    }
}
