# Unified Header Surface Design

## Context

The conversation view, welcome screen, and floating composer now use a shared visual language: soft theme-provided surfaces, rounded geometry, restrained elevation, and semantic accent colors. The native JavaFX application header still reads as a separate toolbar made of several compact panels. Its calendar countdown and action controls should become part of the same surface family without changing their behavior.

The approved direction is **A. Unified surface**: one floating header surface containing a quiet calendar summary and three compact action groups.

## Goals

- Make the native application header visually consistent with the refreshed chat and composer.
- Present the header as one calm, rounded, lightly elevated surface.
- Improve the visual hierarchy of the next-event countdown and action icons.
- Preserve all existing actions, menus, tooltips, accessibility text, toggle state, and calendar refresh behavior.
- Remain usable at the default application width, in narrower windows, and with both light and dark themes.

## Non-goals

- No changes to chat HTML, JavaScript, WebView CSS, message rendering, or composer behavior.
- No changes to calendar selection, refresh intervals, countdown calculations, or localization behavior.
- No new header actions and no removal of existing actions.
- No backend, persistence, workspace-setting, or event-bus changes.
- No unrelated application-shell refactoring.

## Approved Visual Direction

### Floating unified surface

The `HeaderBar` sits inside a small padded dock so the application background remains visible around it. The bar uses the theme overlay surface, a 16 px radius, a subtle theme-derived shadow, and no full-width bottom divider. This makes it visually related to the floating composer and assistant message surfaces while retaining its role as the top-level application header.

The dock owns external spacing. The header owns its background, radius, elevation, and compact internal padding. Neither component introduces literal palette colors.

### Next-event presentation

The existing event countdown becomes the quiet leading area of the shared surface rather than a separate filled card. It contains:

- a small calendar icon using the accent foreground token;
- the event summary in the default foreground color and semibold weight;
- the countdown in the muted foreground color.

The event summary and countdown retain their current text and behavior. Long summaries truncate with an ellipsis within a bounded leading area so they cannot crowd out header actions. The urgent state continues to use the semantic danger foreground token for the event text and countdown.

When no current or upcoming event is available, the countdown remains unmanaged and invisible as it does today. The trailing action area stays aligned correctly inside the unified surface.

### Action groups and controls

The current action order and grouping remain unchanged:

1. new session and history;
2. about, OpenCode session, and settings;
3. split mode and always-on-top.

Each group becomes a quiet inset segment using the subtle background and border tokens. Controls retain a 30 px square target, a 9 px inner radius, and the current icons and tooltips. Neutral controls use the muted foreground token. Hover uses the overlay surface and default foreground token without moving surrounding content.

The settings menu button receives a restrained accent treatment using the subtle accent background and accent foreground. This gives the frequently referenced configuration menu a stable visual anchor without making it look selected.

Selected toggle buttons use the accent emphasis background and emphasis foreground icon. Focused controls retain a visible theme-compatible focus treatment. Disabled and hidden states continue to rely on the existing JavaFX control behavior.

## Layout and Responsiveness

The header remains a leading/trailing `HeaderBar` layout. The calendar component is width-bounded and its summary label can shrink and ellipsize. The action groups retain their intrinsic width and must remain fully reachable. This establishes a clear compression order: event text yields space before action controls do.

The layout must remain balanced in these states:

- default 760 px application width;
- a narrower manually resized window;
- split mode and normal mode;
- calendar event present and absent;
- split-mode action visible and hidden.

No action wraps onto a second row. The header dock and internal padding may become compact enough to preserve the single-row composition, but control targets must not shrink below their specified dimensions.

## Components and Implementation Boundary

The implementation is limited to the native application shell:

- `AssistantAppLauncher` wraps the `HeaderBar` in a styled dock that provides external spacing.
- `chat-shell.css` defines the dock, unified surface, event presentation, action segments, hover, selected, and accent states.
- `HeaderEventCountdown` adds the decorative calendar icon and explicit shrink/truncation constraints while preserving data refresh and countdown logic.
- `header-bar-button-box.fxml` adds a dedicated style class to the settings action if needed for its accent treatment. Existing handlers, grouping, order, and accessibility metadata remain unchanged.

`index.css` currently contains older generic header countdown rules. Header-specific presentation should be owned by the later-loaded `chat-shell.css`; implementation should avoid creating another competing override block or changing unrelated legacy styles.

## Data Flow and Behavior

Data flow remains unchanged:

1. `AssistantAppLauncher` creates the `HeaderBar`, the `ApplicationHeaderButtonBox`, and the `HeaderEventCountdown`.
2. `HeaderEventCountdown` fetches today's events through the existing calendar service.
3. Its existing timers select the active or next event and update the summary, countdown, visibility, and urgent style class.
4. Header controls continue to invoke the existing `ApplicationWindow` handlers and persist toggle state through the existing workspace settings API.

The new icon and layout constraints are presentational only. They introduce no new service call, timer, event subscription, model, or stored setting.

## Error and Edge States

- Calendar authentication or fetch errors continue to hide the event component without affecting header actions.
- A missing or blank event summary renders safely and does not expand the leading area.
- Very long event names ellipsize instead of overlapping or hiding action controls.
- Urgent events remain identifiable in both light and dark themes through semantic danger styling.
- When the dashboard is hidden, the split-mode button remains unmanaged and its group closes the resulting space naturally.
- Menu popup behavior, keyboard focus, tooltips, and accessible labels remain available after the visual change.

## Verification

### Automated

- Run the focused stylesheet contract tests, including `ApplicationStylesheetContractTest`.
- Run `./gradlew test` with the repository's supported JDK.
- Run `./gradlew build` if the test task succeeds.
- Confirm the production diff contains no chat HTML, JavaScript, or WebView CSS changes.

### Manual UI QA

Inspect the running application with:

- the configured light and dark Calm Command Center themes;
- a current event, an upcoming event, an urgent event, a long event title, and no event;
- default width and a narrower resized window;
- split-mode button visible and hidden;
- hover, keyboard focus, selected pin, selected split mode, and the settings menu open.

Confirm that the header surface, message surfaces, and composer read as one interface family and that no control shifts, clips, or becomes unreachable.

## Acceptance Criteria

- The header appears as one padded, rounded, lightly elevated surface.
- The event countdown reads as the leading content of that surface, with an accent calendar icon and safe ellipsis behavior.
- Existing action groups remain recognizable but visually quieter inside the shared surface.
- Settings has a restrained accent treatment, while selected toggles have an unambiguous active state.
- Header behavior and action wiring are unchanged.
- The layout remains usable in narrow and default-width windows with and without an event.
- Light and dark themes use semantic tokens with no component-level literal palette colors.
- Chat HTML, JavaScript, and WebView CSS remain unchanged.
