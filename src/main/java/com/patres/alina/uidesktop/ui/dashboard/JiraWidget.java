package com.patres.alina.uidesktop.ui.dashboard;

import atlantafx.base.theme.Styles;
import com.patres.alina.common.tracking.DashboardChangeTracker;
import com.patres.alina.common.tracking.DashboardSection;
import com.patres.alina.common.tracking.TrackableItem;
import com.patres.alina.server.integration.JiraIssue;
import com.patres.alina.server.integration.JiraIssueResult;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.chat.Browser;
import com.patres.alina.uidesktop.util.EmojiLabelHelper;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.devicons.Devicons;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard widget displaying Jira issues assigned to the current user
 * that are not closed/done/rejected.
 */
public class JiraWidget extends VBox {

    private final Label titleLabel = new Label();
    private final Label countLabel = new Label();
    private final Button collapseButton = new Button();
    private final VBox contentBox = new VBox(2);
    private final VBox detailsBox = new VBox(2);

    private boolean collapsed = false;

    private Timeline refreshTimeline;

    public JiraWidget() {
        getStyleClass().add("workspace-dashboard");

        final FontIcon jiraIcon = new FontIcon(Devicons.JIRA);
        jiraIcon.getStyleClass().add("workspace-dashboard-title");
        titleLabel.setText("Jira Issues");
        titleLabel.setGraphic(jiraIcon);
        titleLabel.getStyleClass().add("workspace-dashboard-title");

        countLabel.getStyleClass().add("workspace-dashboard-count");
        countLabel.setManaged(false);
        countLabel.setVisible(false);

        collapseButton.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT, "workspace-collapse-button");
        collapseButton.setOnAction(event -> toggleCollapsed());

        contentBox.getStyleClass().add("workspace-task-list");
        detailsBox.getStyleClass().add("workspace-dashboard-content");

        final HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final HBox header = new HBox(8, titleLabel, countLabel, spacer, collapseButton);
        header.getStyleClass().add("workspace-dashboard-header");

        detailsBox.getChildren().add(contentBox);

        setSpacing(4);
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
        final int refreshSeconds = BackendApi.getWorkspaceSettings().dashboardJiraRefreshSeconds();
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(refreshSeconds), event -> refreshAsync()));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
        refreshAsync();
    }

    private void refreshAsync() {
        Thread.startVirtualThread(() -> {
            final var settings = BackendApi.getWorkspaceSettings();
            final int maxResults = settings.dashboardJiraIssueLimit();
            final String email = settings.jiraEmail();
            final String token = settings.jiraApiToken();
            
            // Hide widget if no credentials
            if (email == null || email.isBlank() || token == null || token.isBlank()) {
                Platform.runLater(() -> {
                    setManaged(false);
                    setVisible(false);
                });
                return;
            }
            
            final JiraIssueResult result = BackendApi.fetchJiraAssignedIssues(email, token, maxResults);
            trackChanges(result.issues());
            Platform.runLater(() -> render(result));
        });
    }

    private void render(final JiraIssueResult result) {
        setManaged(true);
        setVisible(true);

        final int displayedCount = result.issues().size();
        final int totalCount = result.totalCount();

        // Format: "10" or "10+" if there are more
        final String countText = displayedCount < totalCount ? displayedCount + "+" : String.valueOf(displayedCount);
        countLabel.setText(countText);
        countLabel.setManaged(!result.issues().isEmpty());
        countLabel.setVisible(!result.issues().isEmpty());

        detailsBox.setManaged(!collapsed);
        detailsBox.setVisible(!collapsed);

        contentBox.getChildren().clear();

        if (result.issues().isEmpty()) {
            renderEmpty();
            return;
        }

        for (final JiraIssue issue : result.issues()) {
            contentBox.getChildren().add(createIssueRow(issue));
        }
    }

    private void renderEmpty() {
        contentBox.getChildren().clear();
        final Label emptyLabel = new Label("No assigned issues");
        emptyLabel.getStyleClass().add("workspace-dashboard-empty");
        contentBox.getChildren().add(emptyLabel);
    }

    private HBox createIssueRow(final JiraIssue issue) {
        final Label keyLabel = new Label(issue.key());
        keyLabel.getStyleClass().add("workspace-jira-key");
        keyLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        keyLabel.setOnMouseClicked(event -> Browser.openWebpage(issue.url()));

        final Label statusLabel = new Label(issue.status());
        statusLabel.getStyleClass().add("workspace-jira-status");
        statusLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        // Apply color class based on status
        final String statusLower = issue.status().toLowerCase();
        if (statusLower.contains("progress") || statusLower.contains("review") || statusLower.contains("w toku")) {
            statusLabel.getStyleClass().add("workspace-jira-status-progress");
        }

        final Label summaryLabel = new Label();
        EmojiLabelHelper.applyEmojiText(summaryLabel, issue.summary());
        summaryLabel.getStyleClass().add("workspace-jira-summary");
        summaryLabel.setMaxWidth(Double.MAX_VALUE);
        summaryLabel.setWrapText(false);
        HBox.setHgrow(summaryLabel, Priority.ALWAYS);

        final HBox row = new HBox(8, keyLabel, statusLabel, summaryLabel);
        row.getStyleClass().add("workspace-jira-item");
        row.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(row, Priority.ALWAYS);
        return row;
    }

    // ── Change tracking ──────────────────────────────────────────

    private void trackChanges(final List<JiraIssue> issues) {
        final boolean enabled = BackendApi.getWorkspaceSettings().jiraChangeNotificationsEnabled();
        DashboardChangeTracker.getInstance().trackChanges(
                DashboardSection.JIRA,
                issues,
                JiraWidget::toTrackableItem,
                enabled
        );
    }

    static TrackableItem toTrackableItem(final JiraIssue issue) {
        final String key = issue.key();
        final String displayName = issue.summary();
        final Map<String, String> fields = new LinkedHashMap<>();
        fields.put("summary", issue.summary());
        fields.put("status", issue.status());
        fields.put("priority", issue.priority());
        fields.put("type", issue.type());
        fields.put("url", issue.url());
        return new TrackableItem(key, displayName, fields);
    }

    private void toggleCollapsed() {
        collapsed = !collapsed;
        updateCollapseButton();
        detailsBox.setManaged(!collapsed);
        detailsBox.setVisible(!collapsed);
    }

    private void updateCollapseButton() {
        collapseButton.setText(null);
        collapseButton.setGraphic(new FontIcon(collapsed ? Feather.CHEVRON_DOWN : Feather.CHEVRON_UP));
    }
}
