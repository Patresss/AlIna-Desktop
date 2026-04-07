package com.patres.alina.uidesktop.ui.dashboard;

import atlantafx.base.theme.Styles;
import com.patres.alina.server.integration.GitHubPullRequest;
import com.patres.alina.server.integration.GitHubService;
import com.patres.alina.uidesktop.ui.chat.Browser;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
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
    private final VBox contentBox = new VBox(6);
    private final VBox detailsBox = new VBox(10);

    private boolean collapsed = false;
    private String githubToken;

    private final Timeline refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(60), event -> refreshAsync())
    );

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

        setSpacing(10);
        setPadding(new Insets(10, 12, 10, 12));
        getChildren().addAll(header, detailsBox);

        updateCollapseButton();
        renderEmpty();

        setManaged(false);
        setVisible(false);

        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
    }

    public void refresh(final String githubToken) {
        this.githubToken = githubToken;
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
            final List<GitHubPullRequest> pullRequests = GitHubService.fetchPendingReviews(token);
            Platform.runLater(() -> render(pullRequests));
        });
    }

    private void render(final List<GitHubPullRequest> pullRequests) {
        setManaged(true);
        setVisible(true);

        countLabel.setText(String.valueOf(pullRequests.size()));
        countLabel.setManaged(!pullRequests.isEmpty());
        countLabel.setVisible(!pullRequests.isEmpty());

        detailsBox.setManaged(!collapsed);
        detailsBox.setVisible(!collapsed);

        contentBox.getChildren().clear();

        if (pullRequests.isEmpty()) {
            renderEmpty();
            return;
        }

        for (final GitHubPullRequest pr : pullRequests) {
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

        final FontIcon linkIcon = new FontIcon(Feather.EXTERNAL_LINK);
        linkIcon.getStyleClass().add("workspace-pr-link-icon");

        final Label prTitleLabel = new Label(pr.title());
        prTitleLabel.getStyleClass().add("workspace-pr-title");
        prTitleLabel.setGraphic(linkIcon);
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
