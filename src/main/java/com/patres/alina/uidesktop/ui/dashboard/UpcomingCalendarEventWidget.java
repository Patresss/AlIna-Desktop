package com.patres.alina.uidesktop.ui.dashboard;

import atlantafx.base.theme.Styles;
import com.patres.alina.common.event.WorkspaceSettingsUpdatedEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.settings.UpcomingEventCardSettings;
import com.patres.alina.server.integration.GoogleCalendarAttachment;
import com.patres.alina.server.integration.GoogleCalendarAttendee;
import com.patres.alina.server.integration.GoogleCalendarEvent;
import com.patres.alina.server.integration.GoogleCalendarResult;
import com.patres.alina.server.integration.GoogleCalendarService;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.calendar.CalendarDescriptionText;
import com.patres.alina.uidesktop.ui.calendar.CalendarEventLinkResolver;
import com.patres.alina.uidesktop.ui.calendar.CalendarEventPromptArguments;
import com.patres.alina.uidesktop.ui.calendar.GoogleCalendarFeed;
import com.patres.alina.uidesktop.ui.calendar.GoogleCalendarSnapshot;
import com.patres.alina.uidesktop.ui.calendar.UpcomingCalendarEventSelector;
import com.patres.alina.uidesktop.ui.chat.Browser;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.util.EmojiLabelHelper;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Dense dashboard card for the running or next Google Calendar event today. */
public final class UpcomingCalendarEventWidget extends VBox {

    private static final int RE_AUTH_DELAY_SECONDS = 10;
    private static final int MAX_COLLAPSED_ATTACHMENTS = 3;
    private static final int MIN_EXPANDABLE_DESCRIPTION_LENGTH = 48;
    private static final int URGENT_MINUTES = 5;

    private final GoogleCalendarFeed calendarFeed;
    private final UpcomingCalendarEventSelector selector;
    private final Clock clock;
    private final HBox statusBox = new HBox();
    private final VBox detailsBox = new VBox(4);
    private final VBox contentBox = new VBox(4);
    private final Button refreshButton = new Button();
    private final Button collapseButton = new Button();

    private GoogleCalendarSnapshot snapshot = GoogleCalendarSnapshot.initialLoading();
    private Timeline tickTimeline;
    private boolean collapsed;
    private boolean attendeesExpanded;
    private boolean descriptionExpanded;
    private boolean attachmentsVisible;
    private boolean attachmentsExpanded;
    private String selectedEventKey = "";

    public UpcomingCalendarEventWidget(final GoogleCalendarFeed calendarFeed) {
        this(calendarFeed, Clock.systemDefaultZone());
    }

    UpcomingCalendarEventWidget(final GoogleCalendarFeed calendarFeed, final Clock clock) {
        this.calendarFeed = calendarFeed;
        this.clock = clock;
        this.selector = new UpcomingCalendarEventSelector(clock);

        getStyleClass().addAll("workspace-dashboard", "workspace-upcoming-event");
        setMinWidth(0);

        final Label title = new Label();
        title.textProperty().bind(LanguageManager.createStringBinding("dashboard.upcomingEvent.title"));
        title.setGraphic(new FontIcon(Feather.CLOCK));
        title.setGraphicTextGap(5);
        title.setMinWidth(0);
        title.getStyleClass().add("workspace-dashboard-title");

        configureIconButton(
                refreshButton,
                Feather.REFRESH_CW,
                "dashboard.upcomingEvent.refresh",
                calendarFeed::refreshNow
        );
        configureIconButton(
                collapseButton,
                Feather.CHEVRON_UP,
                "dashboard.upcomingEvent.collapse",
                this::toggleCollapsed
        );

        final Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        final HBox header = new HBox(5, title, spacer, statusBox, refreshButton, collapseButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("workspace-dashboard-header");

        detailsBox.getStyleClass().add("workspace-dashboard-content");
        detailsBox.getChildren().add(contentBox);
        getChildren().addAll(header, detailsBox);

        startTickTimeline();
        calendarFeed.subscribe(this::calendarSnapshotUpdated);
        DefaultEventBus.getInstance().subscribe(
                WorkspaceSettingsUpdatedEvent.class,
                event -> render()
        );
    }

    private void configureIconButton(final Button button,
                                     final Feather icon,
                                     final String tooltipKey,
                                     final Runnable action) {
        button.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT, "workspace-collapse-button");
        button.setGraphic(new FontIcon(icon));
        button.setFocusTraversable(true);
        button.setTooltip(new Tooltip(LanguageManager.getLanguageString(tooltipKey)));
        button.setAccessibleText(LanguageManager.getLanguageString(tooltipKey));
        button.setOnAction(event -> action.run());
    }

    private void startTickTimeline() {
        tickTimeline = new Timeline(new KeyFrame(Duration.seconds(30), event -> render()));
        tickTimeline.setCycleCount(Animation.INDEFINITE);
        tickTimeline.play();
    }

    private void calendarSnapshotUpdated(final GoogleCalendarSnapshot value) {
        snapshot = value;
        render();
    }

    private void render() {
        contentBox.getChildren().clear();
        statusBox.getChildren().clear();
        if (snapshot.loading() || snapshot.latestResult() == null) {
            renderMessage("dashboard.upcomingEvent.loading", "workspace-dashboard-empty");
            return;
        }

        final GoogleCalendarResult result = snapshot.latestResult();
        if (result.authError()) {
            renderAuthError(result.errorMessage());
            return;
        }

        final boolean stale = !result.errorMessage().isEmpty() && hasSuccessfulDataFromToday();
        if (!result.errorMessage().isEmpty() && !stale) {
            renderError(result.errorMessage());
            return;
        }

        final List<GoogleCalendarEvent> events = stale
                ? snapshot.lastSuccessfulEvents()
                : result.events();
        final var selection = selector.select(events);
        if (selection.isEmpty()) {
            resetExpansionIfEventChanged("");
            if (stale) {
                renderStaleWarning();
            }
            renderMessage("dashboard.upcomingEvent.empty", "workspace-dashboard-empty");
            return;
        }

        final UpcomingCalendarEventSelector.Selection selected = selection.get();
        resetExpansionIfEventChanged(eventKey(selected.event()));
        if (stale) {
            renderStaleWarning();
        }
        renderSelection(selected, BackendApi.getWorkspaceSettings().upcomingEventCard());
    }

    private boolean hasSuccessfulDataFromToday() {
        return snapshot.hasSuccessfulSnapshot()
                && LocalDate.ofInstant(snapshot.lastSuccessfulAt(), clock.getZone()).equals(LocalDate.now(clock));
    }

    private void renderSelection(final UpcomingCalendarEventSelector.Selection selection,
                                 final UpcomingEventCardSettings settings) {
        final GoogleCalendarEvent event = selection.event();
        statusBox.getChildren().setAll(createStatusLabel(selection));

        final Label summary = new Label();
        EmojiLabelHelper.applyEmojiText(summary, event.summary());
        summary.setWrapText(true);
        summary.setTextOverrun(OverrunStyle.ELLIPSIS);
        summary.setMaxHeight(40);
        summary.setMinWidth(0);
        summary.setMaxWidth(Double.MAX_VALUE);
        summary.getStyleClass().add("workspace-upcoming-event-summary");
        contentBox.getChildren().add(summary);

        contentBox.getChildren().add(createMetadata(event));

        final List<String> attendeeLabels = event.attendees().stream()
                .map(GoogleCalendarAttendee::label)
                .filter(label -> !label.isBlank())
                .toList();
        if (!attendeeLabels.isEmpty()) {
            contentBox.getChildren().add(createAttendeesPreview(attendeeLabels, settings.attendeePreviewLimit()));
        }

        final String plainDescription = CalendarDescriptionText.toPlainText(event.description());
        if (!plainDescription.isBlank()) {
            contentBox.getChildren().add(createDescriptionSection(
                    plainDescription,
                    settings.descriptionPreviewCharacters()
            ));
        }

        final List<GoogleCalendarAttachment> attachments = event.attachments().stream()
                .filter(attachment -> !attachment.title().isBlank() || !attachment.fileUrl().isBlank())
                .toList();
        if (!settings.showAttachments()) {
            attachmentsVisible = false;
            attachmentsExpanded = false;
        }
        final List<GoogleCalendarAttachment> visibleAttachments = settings.showAttachments()
                ? attachments
                : List.of();
        contentBox.getChildren().add(createActionBar(event, visibleAttachments));
        if (attachmentsVisible && !visibleAttachments.isEmpty()) {
            contentBox.getChildren().add(createAttachmentsSection(visibleAttachments));
        }
    }

    private Label createStatusLabel(final UpcomingCalendarEventSelector.Selection selection) {
        final String key = switch (selection.state()) {
            case RUNNING -> selection.minutes() <= 0
                    ? "dashboard.upcomingEvent.status.endingSoon"
                    : "dashboard.upcomingEvent.status.running";
            case UPCOMING -> selection.minutes() <= 0
                    ? "dashboard.upcomingEvent.status.startingSoon"
                    : "dashboard.upcomingEvent.status.upcoming";
            case ALL_DAY -> "dashboard.upcomingEvent.status.allDay";
        };
        final String text = selection.state() == UpcomingCalendarEventSelector.State.ALL_DAY
                || selection.minutes() <= 0
                ? LanguageManager.getLanguageString(key)
                : LanguageManager.getLanguageString(key, formatDuration(selection.minutes()));
        final Label label = new Label(text);
        label.setMinWidth(Region.USE_PREF_SIZE);
        label.setMaxWidth(Region.USE_PREF_SIZE);
        label.getStyleClass().add("workspace-upcoming-event-status");
        if (selection.state() != UpcomingCalendarEventSelector.State.ALL_DAY
                && selection.minutes() < URGENT_MINUTES) {
            label.getStyleClass().add("workspace-upcoming-event-status-urgent");
        }
        return label;
    }

    private FlowPane createMetadata(final GoogleCalendarEvent event) {
        final FlowPane metadata = new FlowPane(Orientation.HORIZONTAL, 8, 2);
        metadata.setMinWidth(0);
        metadata.getStyleClass().add("workspace-upcoming-event-metadata");

        final String time = event.allDay()
                ? LanguageManager.getLanguageString("dashboard.calendar.allDay")
                : event.startTime() + "–" + event.endTime();
        metadata.getChildren().add(createIconLabel(Feather.CLOCK, time));
        if (!event.location().isBlank()) {
            final Label location = createIconLabel(Feather.MAP_PIN, event.location());
            location.setWrapText(true);
            metadata.getChildren().add(location);
        }
        return metadata;
    }

    private Label createIconLabel(final Feather icon, final String text) {
        final Label label = new Label(text, new FontIcon(icon));
        label.setGraphicTextGap(5);
        label.setMinWidth(0);
        label.getStyleClass().add("workspace-upcoming-event-meta");
        return label;
    }

    private FlowPane createAttendeesPreview(final List<String> attendeeLabels, final int previewLimit) {
        final FlowPane chips = new FlowPane(Orientation.HORIZONTAL, 4, 3);
        chips.setMinWidth(0);
        chips.getStyleClass().add("workspace-upcoming-event-chips");

        final int visibleCount = attendeesExpanded
                ? attendeeLabels.size()
                : Math.min(previewLimit, attendeeLabels.size());
        for (int index = 0; index < visibleCount; index++) {
            final Label chip = new Label(attendeeLabels.get(index));
            chip.getStyleClass().add("workspace-upcoming-event-chip");
            chips.getChildren().add(chip);
        }
        if (visibleCount < attendeeLabels.size()) {
            chips.getChildren().add(createTextButton(
                    LanguageManager.getLanguageString(
                            "dashboard.upcomingEvent.more",
                            attendeeLabels.size() - visibleCount
                    ),
                    () -> {
                        attendeesExpanded = true;
                        render();
                    }
            ));
        } else if (attendeesExpanded && attendeeLabels.size() > previewLimit) {
            chips.getChildren().add(createTextButton(
                    LanguageManager.getLanguageString("dashboard.upcomingEvent.collapseDetails"),
                    () -> {
                        attendeesExpanded = false;
                        render();
                    }
            ));
        }
        return chips;
    }

    private VBox createDescriptionSection(final String plainDescription, final int previewLimit) {
        final VBox section = new VBox(2);
        section.setMinWidth(0);
        section.getStyleClass().add("workspace-upcoming-event-description-section");
        final String preview = CalendarDescriptionText.preview(plainDescription, previewLimit);
        final String normalized = plainDescription.replaceAll("\\s+", " ").strip();
        final boolean truncated = !preview.equals(normalized);
        final boolean expandable = truncated
                || normalized.length() > MIN_EXPANDABLE_DESCRIPTION_LENGTH
                || plainDescription.contains("\n");
        final Label description = new Label(
                descriptionExpanded || !truncated ? plainDescription : preview
        );
        description.setWrapText(descriptionExpanded);
        description.setTextOverrun(OverrunStyle.ELLIPSIS);
        description.setMinWidth(0);
        description.setMaxWidth(Double.MAX_VALUE);
        description.getStyleClass().add("workspace-upcoming-event-description");

        if (descriptionExpanded) {
            section.getChildren().add(description);
            section.getChildren().add(createTextButton(
                    LanguageManager.getLanguageString("dashboard.upcomingEvent.collapseDetails"),
                    this::toggleDescription
            ));
        } else {
            final HBox previewRow = new HBox(3, description);
            previewRow.setAlignment(Pos.CENTER_LEFT);
            previewRow.setMinWidth(0);
            HBox.setHgrow(description, Priority.ALWAYS);
            if (expandable) {
                previewRow.getChildren().add(createTextButton(
                        LanguageManager.getLanguageString("dashboard.upcomingEvent.showMore"),
                        this::toggleDescription
                ));
            }
            section.getChildren().add(previewRow);
        }
        return section;
    }

    private void toggleDescription() {
        descriptionExpanded = !descriptionExpanded;
        render();
    }

    private VBox createAttachmentsSection(final List<GoogleCalendarAttachment> attachments) {
        final VBox section = new VBox(3);
        section.setMinWidth(0);
        section.getStyleClass().add("workspace-upcoming-event-attachments");
        final int visibleCount = attachmentsExpanded
                ? attachments.size()
                : Math.min(MAX_COLLAPSED_ATTACHMENTS, attachments.size());
        final VBox attachmentList = new VBox(4);
        for (int index = 0; index < visibleCount; index++) {
            attachmentList.getChildren().add(createAttachmentButton(attachments.get(index)));
        }
        section.getChildren().add(attachmentList);

        if (visibleCount < attachments.size()) {
            section.getChildren().add(createTextButton(
                    LanguageManager.getLanguageString(
                            "dashboard.upcomingEvent.more",
                            attachments.size() - visibleCount
                    ),
                    () -> {
                        attachmentsExpanded = true;
                        render();
                    }
            ));
        } else if (attachmentsExpanded && attachments.size() > MAX_COLLAPSED_ATTACHMENTS) {
            section.getChildren().add(createTextButton(
                    LanguageManager.getLanguageString("dashboard.upcomingEvent.collapseDetails"),
                    () -> {
                        attachmentsExpanded = false;
                        render();
                    }
            ));
        }
        return section;
    }

    private HBox createActionBar(final GoogleCalendarEvent event,
                                 final List<GoogleCalendarAttachment> attachments) {
        final HBox actions = new HBox(4);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setMinWidth(0);
        actions.getStyleClass().add("workspace-upcoming-event-actions");
        actions.getChildren().add(createPrepareButton(event));
        CalendarEventLinkResolver.resolveJoinUrl(event)
                .map(this::createJoinButton)
                .ifPresent(actions.getChildren()::add);

        final Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actions.getChildren().add(spacer);
        if (!attachments.isEmpty()) {
            actions.getChildren().add(createAttachmentsToggleButton(attachments.size()));
        }
        return actions;
    }

    private Button createPrepareButton(final GoogleCalendarEvent event) {
        final String label = LanguageManager.getLanguageString("dashboard.upcomingEvent.prepare");
        final String prompt = BackendApi.getWorkspaceSettings().calendarAiPrompt();
        final Button prepare = new Button(label, new FontIcon(Feather.CPU));
        prepare.getStyleClass().addAll(Styles.FLAT, Styles.SMALL, "workspace-upcoming-event-prepare");
        prepare.setMnemonicParsing(false);
        prepare.setAccessibleText(label);
        if (prompt == null || prompt.isBlank()) {
            prepare.setDisable(true);
            prepare.setTooltip(new Tooltip(
                    LanguageManager.getLanguageString("dashboard.upcomingEvent.prepare.noPrompt")
            ));
        } else {
            prepare.setOnAction(ignored -> DashboardAiButton.publishPrompt(
                    prompt,
                    CalendarEventPromptArguments.format(event)
            ));
        }
        return prepare;
    }

    private Button createAttachmentsToggleButton(final int attachmentCount) {
        final String accessibleText = LanguageManager.getLanguageString(
                "dashboard.upcomingEvent.attachments",
                attachmentCount
        );
        final Button button = new Button(String.valueOf(attachmentCount), new FontIcon(Feather.PAPERCLIP));
        button.getStyleClass().addAll(
                Styles.FLAT,
                Styles.SMALL,
                "workspace-upcoming-event-attachments-toggle"
        );
        button.setMnemonicParsing(false);
        button.setAccessibleText(accessibleText);
        button.setTooltip(new Tooltip(accessibleText));
        button.setOnAction(ignored -> {
            attachmentsVisible = !attachmentsVisible;
            if (!attachmentsVisible) {
                attachmentsExpanded = false;
            }
            render();
        });
        return button;
    }

    private Button createAttachmentButton(final GoogleCalendarAttachment attachment) {
        final String fallback = LanguageManager.getLanguageString("dashboard.upcomingEvent.attachment");
        final String title = attachment.title().isBlank() ? fallback : attachment.title();
        final Button button = new Button(title, new FontIcon(Feather.PAPERCLIP));
        button.getStyleClass().addAll(Styles.FLAT, Styles.SMALL, "workspace-upcoming-event-attachment");
        button.setMnemonicParsing(false);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        CalendarEventLinkResolver.safeHttpUrl(attachment.fileUrl()).ifPresentOrElse(
                url -> button.setOnAction(event -> Browser.openWebpage(url)),
                () -> {
                    button.setDisable(true);
                    button.setTooltip(new Tooltip(
                            LanguageManager.getLanguageString("dashboard.upcomingEvent.invalidLink")
                    ));
                }
        );
        return button;
    }

    private Button createJoinButton(final String url) {
        final Button join = new Button(
                LanguageManager.getLanguageString("dashboard.upcomingEvent.join"),
                new FontIcon(Feather.VIDEO)
        );
        join.getStyleClass().addAll(Styles.FLAT, Styles.SMALL, "workspace-upcoming-event-join");
        join.setMnemonicParsing(false);
        join.setOnAction(event -> Browser.openWebpage(url));
        join.setAccessibleText(LanguageManager.getLanguageString("dashboard.upcomingEvent.join"));
        return join;
    }

    private Button createTextButton(final String text, final Runnable action) {
        final Button button = new Button(text);
        button.getStyleClass().addAll(Styles.FLAT, Styles.SMALL, "workspace-upcoming-event-more");
        button.setMnemonicParsing(false);
        button.setOnAction(event -> action.run());
        return button;
    }

    private void renderStaleWarning() {
        renderMessage("dashboard.upcomingEvent.stale", "workspace-upcoming-event-stale");
    }

    private void renderError(final String message) {
        final Label error = new Label(LanguageManager.getLanguageString("dashboard.calendar.error", message));
        error.setWrapText(true);
        error.getStyleClass().add("workspace-calendar-error");
        contentBox.getChildren().add(error);
    }

    private void renderAuthError(final String message) {
        renderError(message);
        final Button reAuth = new Button(
                LanguageManager.getLanguageString("dashboard.calendar.reAuth"),
                new FontIcon(Feather.LOG_IN)
        );
        reAuth.getStyleClass().addAll(Styles.SMALL, "workspace-calendar-auth-button");
        reAuth.setOnAction(event -> {
            GoogleCalendarService.refreshAuth();
            reAuth.setText(LanguageManager.getLanguageString("dashboard.calendar.reAuth.progress"));
            reAuth.setDisable(true);
            new Timeline(new KeyFrame(Duration.seconds(RE_AUTH_DELAY_SECONDS), ignored -> {
                reAuth.setText(LanguageManager.getLanguageString("dashboard.calendar.reAuth"));
                reAuth.setDisable(false);
                calendarFeed.refreshNow();
            })).play();
        });
        contentBox.getChildren().add(reAuth);
    }

    private void renderMessage(final String key, final String styleClass) {
        final Label label = new Label(LanguageManager.getLanguageString(key));
        label.setWrapText(true);
        label.getStyleClass().add(styleClass);
        contentBox.getChildren().add(label);
    }

    private void resetExpansionIfEventChanged(final String eventKey) {
        if (Objects.equals(selectedEventKey, eventKey)) {
            return;
        }
        selectedEventKey = eventKey;
        attendeesExpanded = false;
        descriptionExpanded = false;
        attachmentsVisible = false;
        attachmentsExpanded = false;
    }

    private String eventKey(final GoogleCalendarEvent event) {
        return event.summary() + "|" + event.rawStartDateTime() + "|" + event.rawEndDateTime();
    }

    private String formatDuration(final long totalMinutes) {
        if (totalMinutes < 60) {
            return totalMinutes + " min";
        }
        final long hours = totalMinutes / 60;
        final long minutes = totalMinutes % 60;
        return minutes == 0 ? hours + "h" : hours + "h " + minutes + " min";
    }

    private void toggleCollapsed() {
        collapsed = !collapsed;
        detailsBox.setManaged(!collapsed);
        detailsBox.setVisible(!collapsed);
        collapseButton.setGraphic(new FontIcon(collapsed ? Feather.CHEVRON_DOWN : Feather.CHEVRON_UP));
        if (collapsed) {
            if (!getStyleClass().contains("workspace-dashboard-collapsed")) {
                getStyleClass().add("workspace-dashboard-collapsed");
            }
        } else {
            getStyleClass().remove("workspace-dashboard-collapsed");
        }
    }
}
