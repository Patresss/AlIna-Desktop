package com.patres.alina.uidesktop.ui.dashboard;

import atlantafx.base.theme.Styles;
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

import java.util.List;

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
        final String repoName = extractRepoName(pr.repository());
        final Label repoLabel = new Label(repoName);
        repoLabel.getStyleClass().add("workspace-pr-repo");
        repoLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        final Label prTitleLabel = new Label();
        EmojiLabelHelper.applyEmojiText(prTitleLabel, pr.title());
        prTitleLabel.getStyleClass().add("workspace-pr-title");
        prTitleLabel.setMaxWidth(Double.MAX_VALUE);
        prTitleLabel.setWrapText(false);
        HBox.setHgrow(prTitleLabel, Priority.ALWAYS);
        prTitleLabel.setOnMouseClicked(event -> Browser.openWebpage(pr.url()));

        final HBox row = new HBox(8, repoLabel, prTitleLabel);

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
