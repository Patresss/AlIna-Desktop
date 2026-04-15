package com.patres.alina.uidesktop.ui.dashboard;

import atlantafx.base.theme.Styles;
import com.patres.alina.common.tracking.DashboardChangeTracker;
import com.patres.alina.common.tracking.DashboardSection;
import com.patres.alina.common.tracking.TrackableItem;
import com.patres.alina.server.integration.GitHubPullRequest;
import com.patres.alina.server.integration.GitHubPullRequestResult;
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
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GitHubWidget extends VBox {

    private final Label titleLabel = new Label();
    private final Label countLabel = new Label();
    private final Button collapseButton = new Button();
    private final VBox contentBox = new VBox(2);
    private final VBox detailsBox = new VBox(2);

    private boolean collapsed = false;
    private String githubToken;

    private Timeline refreshTimeline;

    public GitHubWidget() {
        getStyleClass().add("workspace-dashboard");

        final FontIcon githubIcon = new FontIcon(Feather.GITHUB);
        githubIcon.getStyleClass().add("workspace-dashboard-title");
        titleLabel.setText("GitHub Reviews");
        titleLabel.setGraphic(githubIcon);
        titleLabel.setGraphicTextGap(5);
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

        setSpacing(2);
        getChildren().addAll(header, detailsBox);

        updateCollapseButton();
        renderEmpty();

        setManaged(false);
        setVisible(false);
    }

    public void refresh(final String githubToken) {
        this.githubToken = githubToken;
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        final int refreshSeconds = BackendApi.getWorkspaceSettings().dashboardGithubRefreshSeconds();
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(refreshSeconds), event -> refreshAsync()));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
        refreshAsync();
    }

    private void refreshAsync() {
        final String token = this.githubToken;
        if (token == null || token.isBlank()) {
            Platform.runLater(() -> {
                setManaged(false);
                setVisible(false);
            });
            return;
        }

        Thread.startVirtualThread(() -> {
            final int maxResults = BackendApi.getWorkspaceSettings().dashboardGithubPrLimit();
            final GitHubPullRequestResult result = BackendApi.fetchGitHubPendingReviews(token, maxResults);
            trackChanges(result);
            Platform.runLater(() -> render(result));
        });
    }

    private void render(final GitHubPullRequestResult result) {
        setManaged(true);
        setVisible(true);

        final int displayedCount = result.pullRequests().size();
        final int totalCount = result.totalCount();
        
        // Format: "10" or "10+" if there are more
        final String countText = displayedCount < totalCount ? displayedCount + "+" : String.valueOf(displayedCount);
        countLabel.setText(countText);
        countLabel.setManaged(!result.pullRequests().isEmpty());
        countLabel.setVisible(!result.pullRequests().isEmpty());

        detailsBox.setManaged(!collapsed);
        detailsBox.setVisible(!collapsed);

        contentBox.getChildren().clear();

        if (result.pullRequests().isEmpty()) {
            renderEmpty();
            return;
        }

        for (final GitHubPullRequest pr : result.pullRequests()) {
            contentBox.getChildren().add(createPrRow(pr));
        }
    }

    private void renderEmpty() {
        contentBox.getChildren().clear();
        final Label emptyLabel = new Label("No pending reviews");
        emptyLabel.getStyleClass().add("workspace-dashboard-empty");
        contentBox.getChildren().add(emptyLabel);
    }

    private HBox createPrRow(final GitHubPullRequest pr) {
        final Label numberLabel = new Label("#" + pr.number());
        numberLabel.getStyleClass().addAll("workspace-pr-number", "workspace-pr-link");
        numberLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        numberLabel.setOnMouseClicked(event -> Browser.openWebpage(pr.url()));

        final String repoName = extractRepoName(pr.repository());
        final Label repoLabel = new Label("[" + repoName + "]");
        repoLabel.getStyleClass().add("workspace-pr-repo");
        repoLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        final Label prTitleLabel = new Label();
        EmojiLabelHelper.applyEmojiText(prTitleLabel, pr.title());
        prTitleLabel.getStyleClass().add("workspace-pr-title");
        prTitleLabel.setMaxWidth(Double.MAX_VALUE);
        prTitleLabel.setWrapText(false);
        prTitleLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        HBox.setHgrow(prTitleLabel, Priority.ALWAYS);
        prTitleLabel.setOnMouseClicked(event -> EmojiLabelHelper.toggleWrap(prTitleLabel));

        final HBox row = new HBox(8, numberLabel, repoLabel, prTitleLabel);

        if (pr.draft()) {
            final Label draftLabel = new Label("(draft)");
            draftLabel.getStyleClass().add("workspace-pr-draft");
            row.getChildren().add(draftLabel);
        }

        row.getStyleClass().add("workspace-pr-item");
        row.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(row, Priority.ALWAYS);
        return row;
    }

    // ── Change tracking ──────────────────────────────────────────

    private void trackChanges(final GitHubPullRequestResult result) {
        final boolean enabled = BackendApi.getWorkspaceSettings().githubChangeNotificationsEnabled();
        DashboardChangeTracker.getInstance().trackChanges(
                DashboardSection.GITHUB,
                result.pullRequests(),
                GitHubWidget::toTrackableItem,
                enabled,
                result.fetchError()
        );
    }

    static TrackableItem toTrackableItem(final GitHubPullRequest pr) {
        final String key = "#" + pr.number();
        final String repoName = extractRepoName(pr.repository());
        final String displayName = "[" + repoName + "] " + pr.title();
        final Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", pr.title());
        fields.put("author", pr.author());
        fields.put("draft", String.valueOf(pr.draft()));
        fields.put("repository", pr.repository());
        fields.put("url", pr.url());
        return new TrackableItem(key, displayName, fields);
    }

    // ── Collapse toggle ──────────────────────────────────────────

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

    private static String extractRepoName(final String fullRepoName) {
        if (fullRepoName == null || fullRepoName.isBlank()) {
            return fullRepoName;
        }
        final int slashIndex = fullRepoName.lastIndexOf('/');
        return slashIndex >= 0 ? fullRepoName.substring(slashIndex + 1) : fullRepoName;
    }
}
