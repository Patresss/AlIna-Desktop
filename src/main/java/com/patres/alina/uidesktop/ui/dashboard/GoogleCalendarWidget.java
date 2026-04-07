package com.patres.alina.uidesktop.ui.dashboard;

import atlantafx.base.theme.Styles;
import com.patres.alina.server.integration.GoogleCalendarEvent;
import com.patres.alina.server.integration.GoogleCalendarResult;
import com.patres.alina.server.integration.GoogleCalendarService;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.chat.Browser;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Dashboard widget displaying today's Google Calendar events.
 * Uses the gws CLI + gcloud for authentication.
 * Shows a re-authenticate button when the token is expired.
 */
public class GoogleCalendarWidget extends VBox {

    private final Label titleLabel = new Label();
    private final Label countLabel = new Label();
    private final Button collapseButton = new Button();
    private final VBox contentBox = new VBox(2);
    private final VBox detailsBox = new VBox(2);

    private boolean collapsed = false;
    private Timeline refreshTimeline;

    public GoogleCalendarWidget() {
        getStyleClass().add("workspace-dashboard");

        final FontIcon calendarIcon = new FontIcon(Feather.CALENDAR);
        calendarIcon.getStyleClass().add("workspace-dashboard-title");
        titleLabel.setText("Today");
        titleLabel.setGraphic(calendarIcon);
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
        setPadding(new Insets(6, 0, 4, 0));
        getChildren().addAll(header, detailsBox);

        updateCollapseButton();
        renderLoading();
    }

    public void refresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        final int refreshSeconds = BackendApi.getWorkspaceSettings().dashboardCalendarRefreshSeconds();
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(refreshSeconds), event -> refreshAsync()));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
        refreshAsync();
    }

    private void refreshAsync() {
        Thread.startVirtualThread(() -> {
            final GoogleCalendarResult result = GoogleCalendarService.fetchTodayEvents();
            Platform.runLater(() -> render(result));
        });
    }

    private void render(final GoogleCalendarResult result) {
        contentBox.getChildren().clear();

        if (result.authError()) {
            renderAuthError(result.errorMessage());
            return;
        }

        if (!result.errorMessage().isEmpty()) {
            renderError(result.errorMessage());
            return;
        }

        final WorkspaceSettings settings = BackendApi.getWorkspaceSettings();
        final List<GoogleCalendarEvent> filtered = filterEvents(result.events(), settings);

        final int eventCount = filtered.size();
        countLabel.setText(String.valueOf(eventCount));
        countLabel.setManaged(eventCount > 0);
        countLabel.setVisible(eventCount > 0);

        detailsBox.setManaged(!collapsed);
        detailsBox.setVisible(!collapsed);

        if (filtered.isEmpty()) {
            renderEmpty();
            return;
        }

        for (final GoogleCalendarEvent event : filtered) {
            contentBox.getChildren().add(createEventRow(event));
        }
    }

    private List<GoogleCalendarEvent> filterEvents(final List<GoogleCalendarEvent> events, final WorkspaceSettings settings) {
        final boolean hideAllDay = settings.calendarHideAllDayEvents();
        final boolean onlyCurrentAndFuture = settings.calendarShowOnlyCurrentAndFuture();

        return events.stream()
                .filter(event -> !hideAllDay || !event.allDay())
                .filter(event -> !onlyCurrentAndFuture || isCurrentOrFuture(event))
                .toList();
    }

    private boolean isCurrentOrFuture(final GoogleCalendarEvent event) {
        if (event.allDay()) {
            return true; // all-day events are always "current"
        }
        final String rawEnd = event.rawEndDateTime();
        if (rawEnd == null || rawEnd.isBlank()) {
            return true;
        }
        try {
            final OffsetDateTime endTime = OffsetDateTime.parse(rawEnd);
            return endTime.isAfter(OffsetDateTime.now(ZoneId.systemDefault()));
        } catch (final Exception e) {
            return true;
        }
    }

    private void renderLoading() {
        contentBox.getChildren().clear();
        final Label loadingLabel = new Label("Loading calendar...");
        loadingLabel.getStyleClass().add("workspace-dashboard-empty");
        contentBox.getChildren().add(loadingLabel);
    }

    private void renderEmpty() {
        contentBox.getChildren().clear();
        final Label emptyLabel = new Label("No events today");
        emptyLabel.getStyleClass().add("workspace-dashboard-empty");
        contentBox.getChildren().add(emptyLabel);
    }

    private void renderError(final String message) {
        contentBox.getChildren().clear();
        final Label errorLabel = new Label("Error: " + message);
        errorLabel.getStyleClass().add("workspace-calendar-error");
        errorLabel.setWrapText(true);
        contentBox.getChildren().add(errorLabel);
    }

    private void renderAuthError(final String message) {
        contentBox.getChildren().clear();

        final Label errorLabel = new Label(message);
        errorLabel.getStyleClass().add("workspace-calendar-error");
        errorLabel.setWrapText(true);

        final Button reAuthButton = new Button("Re-authenticate");
        reAuthButton.getStyleClass().addAll(Styles.SMALL, "workspace-calendar-auth-button");
        reAuthButton.setGraphic(new FontIcon(Feather.LOG_IN));
        reAuthButton.setOnAction(event -> {
            GoogleCalendarService.refreshAuth();
            reAuthButton.setText("Authenticating...");
            reAuthButton.setDisable(true);
            // Retry after a delay to give user time to authenticate
            new Timeline(new KeyFrame(Duration.seconds(10), e -> {
                reAuthButton.setText("Re-authenticate");
                reAuthButton.setDisable(false);
                refreshAsync();
            })).play();
        });

        contentBox.getChildren().addAll(errorLabel, reAuthButton);
    }

    private HBox createEventRow(final GoogleCalendarEvent event) {
        final Label timeLabel;
        if (event.allDay()) {
            timeLabel = new Label("All day");
        } else {
            timeLabel = new Label(event.startTime() + " - " + event.endTime());
        }
        timeLabel.getStyleClass().add("workspace-calendar-time");
        timeLabel.setMinWidth(Region.USE_PREF_SIZE);

        // Current-event dot: overlaid on the left of the time label so it doesn't shift layout
        final StackPane timeSlot;
        if (isCurrentEvent(event)) {
            final Label dotLabel = new Label("\u2022");
            dotLabel.getStyleClass().add("workspace-calendar-dot-active");
            timeSlot = new StackPane(timeLabel, dotLabel);
            StackPane.setAlignment(dotLabel, Pos.CENTER_LEFT);
            // Shift dot to the left, outside the time label bounds
            dotLabel.setTranslateX(-10);
        } else {
            timeSlot = new StackPane(timeLabel);
        }
        timeSlot.setMinWidth(Region.USE_PREF_SIZE);

        // Video icon when the event has any conference/meeting URL; placeholder otherwise
        final String meetUrl = resolveClickUrl(event);
        final Region videoSlot;
        if (!meetUrl.isEmpty()) {
            final FontIcon videoIcon = new FontIcon(Feather.VIDEO);
            videoIcon.getStyleClass().add("workspace-calendar-video-icon");
            final Label videoButton = new Label();
            videoButton.setGraphic(videoIcon);
            videoButton.getStyleClass().add("workspace-calendar-video-button");
            videoButton.setOnMouseClicked(e -> Browser.openWebpage(meetUrl));
            videoSlot = videoButton;
        } else {
            final Region placeholder = new Region();
            placeholder.setMinWidth(16);
            placeholder.setPrefWidth(16);
            placeholder.setMaxWidth(16);
            videoSlot = placeholder;
        }

        final Label summaryLabel = new Label(event.summary());
        summaryLabel.getStyleClass().add("workspace-calendar-summary");
        summaryLabel.setMaxWidth(Double.MAX_VALUE);
        summaryLabel.setWrapText(false);
        HBox.setHgrow(summaryLabel, Priority.ALWAYS);

        // Make summary clickable if there's a conference/meeting URL
        if (!meetUrl.isEmpty()) {
            summaryLabel.getStyleClass().add("workspace-calendar-clickable");
            summaryLabel.setOnMouseClicked(e -> Browser.openWebpage(meetUrl));
        }

        final HBox row = new HBox(8, timeSlot, videoSlot, summaryLabel);
        row.getStyleClass().add("workspace-calendar-item");
        row.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(row, Priority.ALWAYS);
        return row;
    }

    private boolean isCurrentEvent(final GoogleCalendarEvent event) {
        if (event.allDay()) {
            return false;
        }
        final String rawStart = event.rawStartDateTime();
        final String rawEnd = event.rawEndDateTime();
        if (rawStart == null || rawStart.isBlank() || rawEnd == null || rawEnd.isBlank()) {
            return false;
        }
        try {
            final OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());
            final OffsetDateTime start = OffsetDateTime.parse(rawStart);
            final OffsetDateTime end = OffsetDateTime.parse(rawEnd);
            return !now.isBefore(start) && now.isBefore(end);
        } catch (final Exception e) {
            return false;
        }
    }

    private static String resolveClickUrl(final GoogleCalendarEvent event) {
        // Prefer hangout/meet link
        if (event.hangoutLink() != null && !event.hangoutLink().isBlank()) {
            return event.hangoutLink();
        }
        // Fall back to location if it looks like a URL
        if (event.location() != null && event.location().startsWith("http")) {
            // Location may contain multiple comma-separated parts; take the first URL
            final String firstPart = event.location().split(",")[0].trim();
            if (firstPart.startsWith("http")) {
                return firstPart;
            }
        }
        return "";
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
