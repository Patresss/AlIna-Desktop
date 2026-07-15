# Dashboard bottom handle and restored chat cards

## Status

This specification supersedes `2026-07-15-dashboard-toggle-soft-chat-design.md`. The header toggle and soft conversation cards from that design are explicitly rejected.

## Goal

Restore the dashboard control to the dashboard surface itself and return Browser messages to the recognizable card-based conversation style that existed before the Calm Command Center chat redesign.

## Dashboard collapse handle

- Remove the dashboard toggle from the native trailing header controls.
- Add a slim, full-width collapse handle as the final child of `DashboardContainer`, below all dashboard cards.
- Center a single chevron icon inside the handle.
- Show an upward chevron while expanded and a downward chevron while collapsed.
- Clicking anywhere on the handle uses the existing persisted dashboard collapse state.
- When collapsed, hide the summary and widget grid but retain the dashboard container and handle, producing a narrow strip that can restore the dashboard.
- Preserve `Shift+Up` and `Shift+Down`; both keyboard and pointer paths update the same state and icon.
- Reuse the existing `workspace-collapse-bar` and `workspace-collapse-bar-icon` theme-token styles.

## Restored Browser conversation cards

### Shared message card

- Restore the previous message geometry: subtle theme background, three-pixel semantic left rail, 10-pixel radius, compact vertical padding, eight-pixel vertical rhythm, and a maximum width of 95%.
- Restore the earlier entry animation and lightweight hover elevation.
- Implement all shadows with theme variables instead of literal `rgba` values.

### User and assistant roles

- User messages remain right-aligned and use the earlier subtle background treatment with the primary accent rail.
- Assistant messages use the alternate accent rail from the earlier design.
- Do not use the rejected asymmetric speech bubbles or nearly full-width neutral assistant panels.
- Commentary returns to the normal assistant message class so intermediate AI text follows the same card rhythm as before.

### Specialized states

- Restore the earlier rail-based treatment for info, warning, success, danger, reasoning, and activity messages.
- Preserve current interaction, streaming, process-detail, permission, todo, copy-action, and image behavior.
- Keep accessibility improvements such as exposing message actions through both hover and keyboard focus.

## Theme contract

- Preserve the earlier visual layout without restoring its hardcoded colors.
- Replace historical `rgba` shadows and `#fff` foregrounds with existing Browser theme variables.
- Keep `CssColorContractTest` passing for `browser-chat.css`.

## State and failure handling

- The bottom handle remains visible even when the dashboard content is collapsed.
- Persisted collapsed state is applied at startup and reflected by the chevron.
- Disabling dashboards globally continues to hide the entire dashboard, including the handle.
- The change does not alter message data, backend calls, or streaming lifecycle.

## Verification

- Confirm the rejected native-header dashboard toggle no longer exists in FXML or controller code.
- Run the component color-contract test and the complete Gradle build.
- Launch the application and verify the handle in expanded and collapsed states.
- Visually compare a user message, normal assistant response, and tool activity against the pre-redesign card structure in the repository baseline.
