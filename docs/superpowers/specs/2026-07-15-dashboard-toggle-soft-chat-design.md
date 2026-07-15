# Dashboard toggle and soft chat cards

## Goal

Restore an always-accessible dashboard visibility control and make Browser conversations read as a modern chat without returning to visually heavy message panels.

## Dashboard control

- Add a dashboard toggle to the trailing native header controls, next to the split-view control.
- Use the dashboard icon from the existing Ikonli Material icon set.
- Selected means that the dashboard is expanded; unselected means that it is collapsed.
- Clicking the control calls the existing `ApplicationWindow.expandDashboard()` and `collapseDashboard()` operations, so persistence stays in the existing dashboard state path.
- Keep the toggle synchronized when the dashboard is changed through `Shift+Up`, `Shift+Down`, persisted workspace settings, or another application action.
- Hide the control when dashboards are disabled globally, matching the split-view control.
- Change the tooltip between the existing localized expand and collapse labels based on the current state.

## Chat hierarchy

### User messages

- Keep messages right-aligned and content-width sized.
- Use a restrained accent-subtle fill and accent-muted outline so the user's role is immediately visible without looking like an alert.
- Limit width to 76% of the conversation column and use a softly asymmetric corner shape.

### Assistant messages

- Render normal assistant text as a neutral soft card using the active theme's subtle background and subtle border tokens.
- Keep the card left-aligned, nearly full width, with comfortable internal padding and rounded corners.
- Use a slightly stronger border only on hover so copy actions remain discoverable without permanent visual noise.

### Process and tool messages

- Keep activity, reasoning, permission, and status messages lighter than normal assistant replies.
- Preserve their semantic left rails and specialized status colors.
- Do not give process entries the full neutral assistant-card treatment; they remain compact supporting rows between conversational messages.
- Keep completed-process details collapsible inside the final assistant response.

## Theme contract

- Use only Browser theme variables such as `--color-bg-subtle`, `--color-border-subtle`, and accent variables.
- Do not add literal component colors.
- Preserve the existing JavaFX-to-Browser palette synchronization for all selectable themes.

## State and failure handling

- Updating the toggle from application state must not recursively re-run the collapse or expand operation.
- If dashboards are disabled, the control is unmanaged and invisible.
- Existing keyboard shortcuts continue working when the header control is hidden.
- No message rendering or streaming data flow changes are required; this is a presentation change over the existing role and process classes.

## Verification

- Do not introduce a JavaFX test harness solely for this wiring; verify toggle synchronization through the focused live-application checks below.
- Run the Browser/JavaFX component color-contract test.
- Run `./gradlew build`.
- Launch the application and verify expanded and collapsed dashboard states, header-toggle synchronization, normal user and assistant messages, tool activity, and at least one alternate theme.
