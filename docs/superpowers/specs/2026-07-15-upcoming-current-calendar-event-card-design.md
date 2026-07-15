# Upcoming/current Google Calendar event card — design

## Goal

Add a separately configurable dashboard card that presents the most relevant Google Calendar event for today: first a currently running timed event, otherwise the next timed event, with an all-day event as a fallback. The card must make the time status, title, participants, description, location, conference link, and optional attachments useful without making the dashboard permanently tall.

The user selected the hierarchical card layout (visual option A). The existing “Today” calendar card remains available as a separate overview.

## Scope

The change includes:

- one new dashboard card with current/upcoming selection and a live countdown;
- participants, full event description, and Calendar attachment metadata in the Google Calendar model;
- one shared calendar feed for the existing calendar card, the new card, and the header countdown;
- independent visibility, layout, and preview settings for the new card;
- Polish and English copy, theme styling, tests, and documentation updates.

The card searches only events returned for the current local day. It does not search tomorrow or later dates. It does not download, preview, upload, or modify attachments, and it does not edit Calendar events.

## Event selection

Selection is implemented outside JavaFX in a focused `UpcomingCalendarEventSelector` that accepts a `Clock`. This keeps time-sensitive behavior deterministic and independently testable.

For each update or 30-second clock tick, the selector uses this priority:

1. Timed events that have started and whose end is still in the future. If several overlap, select the one ending soonest.
2. Timed events whose start is still in the future. Select the earliest start.
3. An all-day event from today, using the order returned by the Calendar API.
4. No event.

Ended timed events are never selected. Malformed timed events are ignored rather than treated as all-day events. All-day events remain eligible, satisfying the requirement that every Google Calendar event type can be represented, but they cannot hide a more actionable timed event.

The presentation status is:

- `Trwa · jeszcze {duration}` / `In progress · {duration} remaining` for a running timed event;
- `Za {duration}` / `In {duration}` for a future timed event;
- `Cały dzień` / `All day` for an all-day fallback.

Durations below one minute use “za chwilę” or “kończy się” equivalents instead of displaying zero minutes. The card recalculates selection and labels every 30 seconds without fetching Calendar again.

## Card presentation

The stable layout identifier is `DashboardCardId.UPCOMING_EVENT`, with key `upcomingEvent`, default order `25`, and half-width support enabled. This places it between Tasks (`20`) and Calendar (`30`) for users who have not customized its order.

The card follows the existing dashboard surface, header, collapse, loading, empty, error, and responsive-width patterns. Its visible hierarchy is:

1. card title and refresh/collapse controls;
2. current/upcoming status pill;
3. event title;
4. exact start–end time and location;
5. participants, when present;
6. description, when present;
7. actions and attachments, when present.

The card header title is “Następne spotkanie” in Polish and “Next meeting” in English. The status pill makes the current state explicit even though every Google Calendar event, including one without attendees or a conferencing link, can be selected.

### Participants

Participants are read from the event `attendees` array. Every returned attendee is included; `displayName` is preferred and email is the fallback label. Blank entries are discarded. The visible participant count is the size of this normalized list. Invitation response status is intentionally not shown in this first version.

The collapsed preview shows the first configured number of participant chips, defaulting to four, followed by a `+N więcej` / `+N more` control when necessary. Activating it expands all chips inline and replaces the control with `Zwiń` / `Collapse`. The section is omitted when the normalized list is empty.

### Description

The full Calendar description is retained in the model and rendered as plain text in JavaFX; it is never interpreted as HTML or executed in a WebView. Whitespace is normalized for the preview while the expanded form preserves meaningful line breaks. The preview is truncated at the nearest word boundary at or before the configured character limit, defaulting to 240 characters. If the description is longer, `Pokaż więcej` / `Show more` expands it inline and `Zwiń` / `Collapse` restores the preview. The section is omitted for blank descriptions.

### Location, conference link, and attachments

Location is rendered as text and can wrap at narrow widths. A prominent `Dołącz` / `Join` action appears when the existing conference-link resolver finds a Hangout/Meet link, a video `conferenceData` entry point, a supported conferencing URL in the description, or an HTTP(S) location.

Calendar attachments are optional. Each normalized attachment contains title, file URL, and MIME type. The UI uses a local paperclip icon and title; it does not fetch remote attachment icons. The collapsed section shows up to three attachments and then `+N więcej` / `+N more`; it can expand and collapse inline. The section is omitted when attachments are disabled in settings or absent from the event.

Conference and attachment actions only open syntactically valid `http` or `https` URLs. Other schemes and malformed values are displayed as unavailable and are never passed to the operating system.

### Expansion state

Participant, description, and attachment expansion states are independent. A 30-second time tick does not collapse them. When the selected event changes, all three reset to collapsed so expanded content from the previous event cannot appear associated with the new one.

## Google Calendar data

`GoogleCalendarCli.EVENT_FIELDS` is extended to request only the added fields needed by the UI:

- `attendees(displayName,email)`;
- `attachments(fileUrl,title,mimeType)`;
- the already requested summary, description, start, end, location, Hangout link, and conference data.

`GoogleCalendarEvent` gains the full description plus immutable lists of focused `GoogleCalendarAttendee` and `GoogleCalendarAttachment` records. The existing extracted description video URL is preserved because it already participates in conference-link resolution. Missing arrays produce empty immutable lists; missing scalar values produce empty strings. Parsing one incomplete attendee or attachment does not fail the entire Calendar response.

## Shared calendar feed

`AssistantAppLauncher` creates one `GoogleCalendarFeed` and passes it to `ApplicationWindow` and `HeaderEventCountdown`. `ApplicationWindow` passes the same instance to `GoogleCalendarWidget` and `UpcomingCalendarEventWidget`.

The feed has one responsibility: asynchronously refresh today’s Calendar result on the configured interval and publish immutable snapshots to its subscribers. It:

- owns the refresh timeline;
- coalesces simultaneous manual and scheduled refresh requests into one in-flight request;
- stores the latest fetch result;
- stores the last successful event list and success timestamp;
- reschedules itself when workspace settings change;
- exposes `refreshNow()` for card refresh controls;
- stops during application shutdown.

The feed does not select an upcoming event, filter the existing calendar list, send notifications, track Calendar changes, or render UI. Those responsibilities stay with their current consumers or the new selector.

The existing Calendar widget no longer owns a duplicate refresh timeline. It receives feed snapshots, applies its existing hide-all-day and current/future filters, tracks changes, and sends notifications once per new successful snapshot. The header countdown also receives the shared event list instead of fetching independently. The new card does not inherit the existing Calendar card’s display filters; it always uses the selection rules in this document.

## Settings and dashboard layout

Add an `UpcomingEventCardSettings` record to `WorkspaceSettings` with:

- `visible`, default `true`;
- `attendeePreviewLimit`, default `4`, normalized to `1..12`;
- `descriptionPreviewCharacters`, default `240`, normalized to `80..2000`;
- `showAttachments`, default `true`.

Keeping these settings in a focused record avoids adding another cluster of loosely related primitive fields to the already large `WorkspaceSettings` constructor. A missing or null nested value is replaced with defaults, preserving compatibility with existing JSON. `DashboardLayoutSettings` automatically supplies the new card’s default layout when the persisted card map lacks `upcomingEvent`.

The Dashboard Settings pane adds a “Next meeting” section directly after Google Calendar with controls for visibility, half-width eligibility, order, attendee preview count, description preview length, and attachment visibility. The shared Calendar refresh interval remains in the Google Calendar section and is not duplicated.

Saving settings publishes the existing workspace-settings update event. Visibility, placement, limits, attachment rendering, and refresh cadence update without restarting the application.

## Loading, empty, and failure states

Before the first result, the card shows the standard loading state. A successful result with no eligible event shows the compact message `Brak kolejnych wydarzeń dzisiaj` / `No more events today`.

If the first fetch fails, the card shows the standard wrapped Calendar error. An authentication failure also offers the existing re-authentication action. If a refresh fails after at least one successful result, the new card keeps using the last successful list, shows a subtle stale-data warning, and continues local time selection. This avoids an empty card during a transient failure while making the stale state visible. A later successful snapshot clears the warning.

The existing Calendar overview may continue to replace its content with its current error state; the stale-data presentation is a new-card behavior, not an unrelated redesign. The header may continue using the last successful list while it remains relevant today.

## Styling and accessibility

New style classes live with the dashboard styles in `workspace.css` and use existing theme variables rather than fixed light/dark colors. The card remains readable at full and half width, supports wrapping rather than horizontal scrolling, and does not force the paired grid row to stretch unexpectedly.

Buttons receive accessible text or tooltips, participant chips are labels rather than focusable controls, and expand/collapse controls remain keyboard reachable. Status is expressed with text, not color alone. Polish and English resource bundles receive matching keys.

## Testing

Automated coverage includes:

- parser tests for description, attendees, attachments, missing arrays and partial nested objects;
- selector tests with a fixed `Clock` for running, upcoming, overlapping, ended, all-day, malformed, and empty inputs;
- feed tests for initial loading, interval refresh, manual refresh, in-flight coalescing, last-success retention, settings rescheduling, and shutdown;
- preview tests for word-boundary truncation, limits, Unicode text, and empty values;
- card behavior tests for independent expansion, reset on event change, empty/error/auth/stale states, and URL-scheme rejection;
- settings tests for defaults, normalization, old JSON without the nested settings object, and the new dashboard layout identifier;
- dashboard planner tests including the default `UPCOMING_EVENT` placement;
- PL/EN resource and CSS style-contract tests.

Verification runs `./gradlew test`, followed by `./gradlew build`. Manual JavaFX verification covers full-width, half-width, split-mode, light-theme, dark-theme, long-content, no-content, and expanded-section layouts.

## Documentation

The Polish and English About pages are updated to mention the separate next/current meeting card, inline details, attachments, and its Dashboard Settings controls.

## Non-goals

- Searching beyond the current local day.
- Editing events or attendee responses.
- Downloading or previewing attachment contents.
- Adding a second Calendar account or changing authentication scope.
- Reworking the visual design of the existing “Today” calendar card.

## API references

The design was checked against the current official Google Calendar API documentation. `events.list` supports the existing read-only Calendar scope and returns Event resources; Event resources expose attendee display names/emails and attachment titles, URLs, and MIME types:

- [Google Calendar API — Events: list](https://developers.google.com/workspace/calendar/api/v3/reference/events/list)
- [Google Calendar API — Events resource](https://developers.google.com/workspace/calendar/api/v3/reference/events)
