package com.patres.alina.uidesktop.ui.dashboard;

import atlantafx.base.theme.Styles;
import com.patres.alina.common.event.ChatNotificationEvent;
import com.patres.alina.common.event.Event;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.common.tracking.DashboardChangeTracker;
import com.patres.alina.common.tracking.DashboardSection;
import com.patres.alina.common.tracking.TrackableItem;
import com.patres.alina.server.integration.GoogleCalendarEvent;
import com.patres.alina.server.integration.GoogleCalendarResult;
import com.patres.alina.server.integration.GoogleCalendarService;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.chat.Browser;
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

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Dashboard widget displaying today's Google Calendar events.
 * Uses the gws CLI + gcloud for authentication.
 * Shows a re-authenticate button when the token is expired.
 */
public class GoogleCalendarWidget extends VBox {

    private static final int UPCOMING_THRESHOLD_MINUTES = 15;
    private static final int URGENT_THRESHOLD_MINUTES = 5;
    private static final int RE_AUTH_DELAY_SECONDS = 10;
    private static final int VIDEO_SLOT_WIDTH = 16;

    private static final String STYLE_DASHBOARD = "workspace-dashboard";
    private static final String STYLE_DASHBOARD_TITLE = "workspace-dashboard-title";
    private static final String STYLE_DASHBOARD_COUNT = "workspace-dashboard-count";
    private static final String STYLE_DASHBOARD_HEADER = "workspace-dashboard-header";
    private static final String STYLE_DASHBOARD_CONTENT = "workspace-dashboard-content";
    private static final String STYLE_DASHBOARD_EMPTY = "workspace-dashboard-empty";
    private static final String STYLE_TASK_LIST = "workspace-task-list";
    private static final String STYLE_COLLAPSE_BUTTON = "workspace-collapse-button";
    private static final String STYLE_CALENDAR_ITEM = "workspace-calendar-item";
    private static final String STYLE_CALENDAR_ITEM_CURRENT = "workspace-calendar-item-current";
    private static final String STYLE_CALENDAR_TIME = "workspace-calendar-time";
    private static final String STYLE_CALENDAR_REMAINING = "workspace-calendar-remaining";
    private static final String STYLE_CALENDAR_UPCOMING = "workspace-calendar-upcoming";
    private static final String STYLE_CALENDAR_TIME_URGENT = "workspace-calendar-time-urgent";
    private static final String STYLE_CALENDAR_VIDEO_ICON = "workspace-calendar-video-icon";
    private static final String STYLE_CALENDAR_VIDEO_BUTTON = "workspace-calendar-video-button";
    private static final String STYLE_CALENDAR_SUMMARY = "workspace-calendar-summary";
    private static final String STYLE_CALENDAR_CLICKABLE = "workspace-calendar-clickable";
    private static final String STYLE_CALENDAR_ERROR = "workspace-calendar-error";
    private static final String STYLE_CALENDAR_AUTH_BUTTON = "workspace-calendar-auth-button";

    private final Label titleLabel = new Label();
    private final Label countLabel = new Label();
    private final Button collapseButton = new Button();
    private final VBox contentBox = new VBox(2);
    private final VBox detailsBox = new VBox(2);

    private boolean collapsed = false;
    private Timeline refreshTimeline;
    private final Set<String> notifiedEventKeys = new HashSet<>();

    public GoogleCalendarWidget() {
        getStyleClass().add(STYLE_DASHBOARD);

        final FontIcon calendarIcon = new FontIcon(Feather.CALENDAR);
        calendarIcon.getStyleClass().add(STYLE_DASHBOARD_TITLE);
        titleLabel.textProperty().bind(LanguageManager.createStringBinding("dashboard.calendar.title"));
        titleLabel.setGraphic(calendarIcon);
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

        setSpacing(4);
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
            if (!result.authError() && result.errorMessage().isEmpty()) {
                trackChanges(result.events());
            }
            Platform.runLater(() -> render(result));
        });
    }

    // ── Rendering ────────────────────────────────────────────────

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

        checkAndSendNotifications(filtered, settings);
    }

    private void renderLoading() {
        contentBox.getChildren().clear();
        final Label loadingLabel = new Label(LanguageManager.getLanguageString("dashboard.calendar.loading"));
        loadingLabel.getStyleClass().add(STYLE_DASHBOARD_EMPTY);
        contentBox.getChildren().add(loadingLabel);
    }

    private void renderEmpty() {
        contentBox.getChildren().clear();
        final Label emptyLabel = new Label(LanguageManager.getLanguageString("dashboard.calendar.empty"));
        emptyLabel.getStyleClass().add(STYLE_DASHBOARD_EMPTY);
        contentBox.getChildren().add(emptyLabel);
    }

    private void renderError(final String message) {
        contentBox.getChildren().clear();
        final Label errorLabel = new Label(LanguageManager.getLanguageString("dashboard.calendar.error", message));
        errorLabel.getStyleClass().add(STYLE_CALENDAR_ERROR);
        errorLabel.setWrapText(true);
        contentBox.getChildren().add(errorLabel);
    }

    private void renderAuthError(final String message) {
        contentBox.getChildren().clear();

        final Label errorLabel = new Label(message);
        errorLabel.getStyleClass().add(STYLE_CALENDAR_ERROR);
        errorLabel.setWrapText(true);

        final Button reAuthButton = new Button(LanguageManager.getLanguageString("dashboard.calendar.reAuth"));
        reAuthButton.getStyleClass().addAll(Styles.SMALL, STYLE_CALENDAR_AUTH_BUTTON);
        reAuthButton.setGraphic(new FontIcon(Feather.LOG_IN));
        reAuthButton.setOnAction(event -> {
            GoogleCalendarService.refreshAuth();
            reAuthButton.setText(LanguageManager.getLanguageString("dashboard.calendar.reAuth.progress"));
            reAuthButton.setDisable(true);
            new Timeline(new KeyFrame(Duration.seconds(RE_AUTH_DELAY_SECONDS), e -> {
                reAuthButton.setText(LanguageManager.getLanguageString("dashboard.calendar.reAuth"));
                reAuthButton.setDisable(false);
                refreshAsync();
            })).play();
        });

        contentBox.getChildren().addAll(errorLabel, reAuthButton);
    }

    // ── Event row ────────────────────────────────────────────────

    private HBox createEventRow(final GoogleCalendarEvent event) {
        final long remainingMinutes = getRemainingMinutes(event);
        final long minutesUntilStart = getMinutesUntilStart(event);
        final boolean isCurrent = remainingMinutes >= 0;
        final boolean isUpcomingSoon = !isCurrent && minutesUntilStart >= 0 && minutesUntilStart <= UPCOMING_THRESHOLD_MINUTES;

        final VBox timeColumn = createTimeColumn(event, isCurrent, isUpcomingSoon, remainingMinutes, minutesUntilStart);
        final Region videoSlot = createVideoSlot(event);
        final Label summaryLabel = createSummaryLabel(event);

        final HBox row = new HBox(8, timeColumn, videoSlot, summaryLabel);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add(STYLE_CALENDAR_ITEM);
        row.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(row, Priority.ALWAYS);

        if (isCurrent) {
            row.getStyleClass().add(STYLE_CALENDAR_ITEM_CURRENT);
        }

        return row;
    }

    private VBox createTimeColumn(final GoogleCalendarEvent event, final boolean isCurrent, final boolean isUpcomingSoon,
                                  final long remainingMinutes, final long minutesUntilStart) {
        final Label timeLabel = new Label(formatTimeText(event));
        timeLabel.getStyleClass().add(STYLE_CALENDAR_TIME);
        timeLabel.setMinWidth(Region.USE_PREF_SIZE);

        final VBox timeColumn = new VBox(0);
        timeColumn.setAlignment(Pos.CENTER_RIGHT);
        timeColumn.getChildren().add(timeLabel);

        if (isCurrent) {
            timeColumn.getChildren().add(createTimeInfoLabel(
                    formatRemainingText(remainingMinutes),
                    STYLE_CALENDAR_REMAINING,
                    remainingMinutes < URGENT_THRESHOLD_MINUTES
            ));
        } else if (isUpcomingSoon) {
            timeColumn.getChildren().add(createTimeInfoLabel(
                    formatUpcomingText(minutesUntilStart),
                    STYLE_CALENDAR_UPCOMING,
                    minutesUntilStart < URGENT_THRESHOLD_MINUTES
            ));
        }

        return timeColumn;
    }

    private Label createTimeInfoLabel(final String text, final String styleClass, final boolean urgent) {
        final Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        if (urgent) {
            label.getStyleClass().add(STYLE_CALENDAR_TIME_URGENT);
        }
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }

    private Region createVideoSlot(final GoogleCalendarEvent event) {
        final String meetUrl = resolveClickUrl(event);
        if (!meetUrl.isEmpty()) {
            final FontIcon videoIcon = new FontIcon(Feather.VIDEO);
            videoIcon.getStyleClass().add(STYLE_CALENDAR_VIDEO_ICON);
            final Label videoButton = new Label();
            videoButton.setGraphic(videoIcon);
            videoButton.getStyleClass().add(STYLE_CALENDAR_VIDEO_BUTTON);
            videoButton.setOnMouseClicked(e -> Browser.openWebpage(meetUrl));
            return videoButton;
        }
        final Region placeholder = new Region();
        placeholder.setMinWidth(VIDEO_SLOT_WIDTH);
        placeholder.setPrefWidth(VIDEO_SLOT_WIDTH);
        placeholder.setMaxWidth(VIDEO_SLOT_WIDTH);
        return placeholder;
    }

    private Label createSummaryLabel(final GoogleCalendarEvent event) {
        final Label summaryLabel = new Label();
        EmojiLabelHelper.applyEmojiText(summaryLabel, event.summary());
        summaryLabel.getStyleClass().add(STYLE_CALENDAR_SUMMARY);
        summaryLabel.setMaxWidth(Double.MAX_VALUE);
        summaryLabel.setWrapText(false);
        HBox.setHgrow(summaryLabel, Priority.ALWAYS);

        final String meetUrl = resolveClickUrl(event);
        if (!meetUrl.isEmpty()) {
            summaryLabel.getStyleClass().add(STYLE_CALENDAR_CLICKABLE);
            summaryLabel.setOnMouseClicked(e -> Browser.openWebpage(meetUrl));
        }

        return summaryLabel;
    }

    // ── Text formatting (i18n) ───────────────────────────────────

    private String formatTimeText(final GoogleCalendarEvent event) {
        if (event.allDay()) {
            return LanguageManager.getLanguageString("dashboard.calendar.allDay");
        }
        return event.startTime() + " - " + event.endTime();
    }

    private String formatRemainingText(final long minutes) {
        if (minutes <= 0) {
            return LanguageManager.getLanguageString("dashboard.calendar.remainingLessThanOne");
        }
        return LanguageManager.getLanguageString("dashboard.calendar.remaining", minutes);
    }

    private String formatUpcomingText(final long minutes) {
        if (minutes <= 0) {
            return LanguageManager.getLanguageString("dashboard.calendar.upcomingLessThanOne");
        }
        return LanguageManager.getLanguageString("dashboard.calendar.upcoming", minutes);
    }

    // ── Notifications ─────────────────────────────────────────────

    private void checkAndSendNotifications(final List<GoogleCalendarEvent> events, final WorkspaceSettings settings) {
        if (!settings.calendarNotificationsEnabled()) {
            return;
        }
        final int minutesBefore = settings.calendarNotificationMinutesBefore();

        for (final GoogleCalendarEvent event : events) {
            if (event.allDay()) {
                continue;
            }
            final long minutesUntilStart = getMinutesUntilStart(event);
            if (minutesUntilStart < 0 || minutesUntilStart > minutesBefore) {
                continue;
            }

            final String eventKey = buildEventKey(event);
            if (notifiedEventKeys.contains(eventKey)) {
                continue;
            }
            notifiedEventKeys.add(eventKey);
            Event.publish(new ChatNotificationEvent(formatNotificationMessage(event, minutesUntilStart)));
        }
    }

    private String buildEventKey(final GoogleCalendarEvent event) {
        return event.summary() + "|" + event.rawStartDateTime();
    }

    private String formatNotificationMessage(final GoogleCalendarEvent event, final long minutesUntilStart) {
        final String meetUrl = resolveClickUrl(event);
        final boolean hasLink = !meetUrl.isEmpty();
        final String summary = event.summary().strip();

        if (minutesUntilStart <= 0) {
            return hasLink
                    ? LanguageManager.getLanguageString("dashboard.calendar.notificationNowWithLink", summary, meetUrl)
                    : LanguageManager.getLanguageString("dashboard.calendar.notificationNow", summary);
        }
        return hasLink
                ? LanguageManager.getLanguageString("dashboard.calendar.notificationWithLink", summary, minutesUntilStart, meetUrl)
                : LanguageManager.getLanguageString("dashboard.calendar.notification", summary, minutesUntilStart);
    }

    // ── Event filtering ──────────────────────────────────────────

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
            return true;
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

    // ── Time calculations ────────────────────────────────────────

    /**
     * Returns the number of minutes remaining for the event, or -1 if it's not the current event.
     */
    private long getRemainingMinutes(final GoogleCalendarEvent event) {
        if (event.allDay()) {
            return -1;
        }
        final String rawStart = event.rawStartDateTime();
        final String rawEnd = event.rawEndDateTime();
        if (rawStart == null || rawStart.isBlank() || rawEnd == null || rawEnd.isBlank()) {
            return -1;
        }
        try {
            final OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());
            final OffsetDateTime start = OffsetDateTime.parse(rawStart);
            final OffsetDateTime end = OffsetDateTime.parse(rawEnd);
            if (now.isBefore(start) || !now.isBefore(end)) {
                return -1;
            }
            return ChronoUnit.MINUTES.between(now, end);
        } catch (final Exception e) {
            return -1;
        }
    }

    /**
     * Returns the number of minutes until the event starts, or -1 if it has already started or is all-day.
     */
    private long getMinutesUntilStart(final GoogleCalendarEvent event) {
        if (event.allDay()) {
            return -1;
        }
        final String rawStart = event.rawStartDateTime();
        if (rawStart == null || rawStart.isBlank()) {
            return -1;
        }
        try {
            final OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());
            final OffsetDateTime start = OffsetDateTime.parse(rawStart);
            if (!now.isBefore(start)) {
                return -1;
            }
            return ChronoUnit.MINUTES.between(now, start);
        } catch (final Exception e) {
            return -1;
        }
    }

    // ── URL resolution ───────────────────────────────────────────

    private static String resolveClickUrl(final GoogleCalendarEvent event) {
        if (event.hangoutLink() != null && !event.hangoutLink().isBlank()) {
            return event.hangoutLink();
        }
        if (event.conferenceUri() != null && !event.conferenceUri().isBlank()) {
            return event.conferenceUri();
        }
        if (event.descriptionVideoUrl() != null && !event.descriptionVideoUrl().isBlank()) {
            return event.descriptionVideoUrl();
        }
        if (event.location() != null && event.location().startsWith("http")) {
            final String firstPart = event.location().split(",")[0].trim();
            if (firstPart.startsWith("http")) {
                return firstPart;
            }
        }
        return "";
    }

    // ── Change tracking ─────────────────────────────────────────

    private void trackChanges(final List<GoogleCalendarEvent> events) {
        final boolean enabled = BackendApi.getWorkspaceSettings().calendarChangeNotificationsEnabled();
        DashboardChangeTracker.getInstance().trackChanges(
                DashboardSection.CALENDAR,
                events,
                GoogleCalendarWidget::toTrackableItem,
                enabled,
                false
        );
    }

    static TrackableItem toTrackableItem(final GoogleCalendarEvent event) {
        final String key = event.summary();
        final String time = event.allDay() ? "" : event.startTime();
        final String displayName = time.isEmpty()
                ? event.summary()
                : time + " " + event.summary();
        final Map<String, String> fields = new LinkedHashMap<>();
        fields.put("startTime", event.startTime());
        fields.put("endTime", event.endTime());
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
}
