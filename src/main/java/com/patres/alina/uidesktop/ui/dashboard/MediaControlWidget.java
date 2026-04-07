package com.patres.alina.uidesktop.ui.dashboard;

import atlantafx.base.theme.Styles;
import com.patres.alina.server.integration.MediaControlService;
import com.patres.alina.uidesktop.backend.BackendApi;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A compact media-controls widget for the dashboard.
 * Displays playback buttons (previous, play/pause, next) and the currently playing track.
 * Controls macOS media players (Spotify / Apple Music) via {@link MediaControlService}.
 * Auto-refreshes track info based on settings.
 */
public class MediaControlWidget extends VBox {

    private final Button playPauseButton = new Button();
    private final Label trackInfoLabel = new Label();
    private boolean currentlyPlaying = false;

    private Timeline refreshTimeline;

    public MediaControlWidget() {
        getStyleClass().add("workspace-dashboard");
        setPadding(new Insets(10, 12, 10, 12));

        // Header with title and controls in one line
        final FontIcon musicIcon = new FontIcon(Feather.MUSIC);
        final Label titleLabel = new Label("Music");
        titleLabel.setGraphic(musicIcon);
        titleLabel.setGraphicTextGap(6);
        titleLabel.getStyleClass().add("workspace-dashboard-title");

        // Control buttons
        final Button prevButton = createControlButton(Feather.SKIP_BACK);
        prevButton.setOnAction(event -> executeInBackground(MediaControlService::previousTrack));

        playPauseButton.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT);
        playPauseButton.setGraphic(new FontIcon(Feather.PLAY));
        playPauseButton.setOnAction(event -> executeInBackground(() -> {
            MediaControlService.playPause();
            // Brief delay so the player state has time to change before we query it
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            refreshTrackInfo();
        }));

        final Button nextButton = createControlButton(Feather.SKIP_FORWARD);
        nextButton.setOnAction(event -> executeInBackground(MediaControlService::nextTrack));

        final HBox controlsBox = new HBox(8, prevButton, playPauseButton, nextButton);
        controlsBox.setAlignment(Pos.CENTER_LEFT);

        // Track info label - będzie obok przycisków
        trackInfoLabel.getStyleClass().add("workspace-dashboard-empty");
        trackInfoLabel.setMaxWidth(Double.MAX_VALUE);
        trackInfoLabel.setWrapText(false);
        HBox.setHgrow(trackInfoLabel, Priority.ALWAYS);

        // Header row with title, controls, and track info all in one line
        final HBox header = new HBox(12, titleLabel, controlsBox, trackInfoLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("workspace-dashboard-header");

        getChildren().add(header);

        // Auto-refresh with settings
        initializeRefreshTimer();

        // Initial state check
        refresh();
    }

    private void initializeRefreshTimer() {
        final int refreshSeconds = BackendApi.getWorkspaceSettings().dashboardMediaRefreshSeconds();
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(refreshSeconds), event -> refresh()));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
    }

    /**
     * Refreshes the widget: updates track info and visibility.
     * Safe to call from any thread — schedules UI updates on the FX Application Thread.
     */
    public void refresh() {
        Thread.startVirtualThread(this::refreshTrackInfo);
    }

    private void refreshTrackInfo() {
        String player = MediaControlService.getActivePlayer();
        boolean playerActive = player != null;
        boolean playing = playerActive && MediaControlService.isPlaying();
        String nowPlaying = playerActive ? MediaControlService.getNowPlaying() : null;

        Platform.runLater(() -> {
            setManaged(playerActive);
            setVisible(playerActive);

            if (!playerActive) {
                return;
            }

            currentlyPlaying = playing;
            playPauseButton.setGraphic(new FontIcon(playing ? Feather.PAUSE : Feather.PLAY));

            if (nowPlaying != null && !nowPlaying.isBlank()) {
                trackInfoLabel.setText(nowPlaying);
                trackInfoLabel.setManaged(true);
                trackInfoLabel.setVisible(true);
            } else {
                trackInfoLabel.setText("");
                trackInfoLabel.setManaged(false);
                trackInfoLabel.setVisible(false);
            }
        });
    }

    private Button createControlButton(Feather icon) {
        final Button button = new Button();
        button.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT);
        button.setGraphic(new FontIcon(icon));
        return button;
    }

    private void executeInBackground(Runnable action) {
        Thread.startVirtualThread(action);
    }
}
