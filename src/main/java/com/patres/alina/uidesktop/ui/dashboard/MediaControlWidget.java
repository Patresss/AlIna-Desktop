package com.patres.alina.uidesktop.ui.dashboard;

import atlantafx.base.theme.Styles;
import com.patres.alina.server.integration.MediaControlService;
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
 * Auto-refreshes track info every 10 seconds and hides itself when no player is active.
 */
public class MediaControlWidget extends VBox {

    private final Button playPauseButton = new Button();
    private final Label trackInfoLabel = new Label();
    private boolean currentlyPlaying = false;

    private final Timeline refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(10), event -> refresh())
    );

    public MediaControlWidget() {
        getStyleClass().add("workspace-dashboard");
        setSpacing(10);
        setPadding(new Insets(10, 12, 10, 12));

        // Header
        final FontIcon musicIcon = new FontIcon(Feather.MUSIC);
        final Label titleLabel = new Label("Music");
        titleLabel.setGraphic(musicIcon);
        titleLabel.setGraphicTextGap(6);
        titleLabel.getStyleClass().add("workspace-dashboard-title");

        final HBox header = new HBox(8, titleLabel);
        header.getStyleClass().add("workspace-dashboard-header");

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

        final HBox controlsRow = new HBox(12, prevButton, playPauseButton, nextButton);
        controlsRow.setAlignment(Pos.CENTER);
        controlsRow.getStyleClass().add("workspace-dashboard-content");

        // Track info label
        trackInfoLabel.getStyleClass().add("workspace-dashboard-empty");
        trackInfoLabel.setMaxWidth(Double.MAX_VALUE);
        trackInfoLabel.setWrapText(false);
        HBox.setHgrow(trackInfoLabel, Priority.ALWAYS);

        getChildren().addAll(header, controlsRow, trackInfoLabel);

        // Auto-refresh
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();

        // Initial state check
        refresh();
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
