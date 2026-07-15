# Stabilize Dashboard Horizontal Position

## Goal

Prevent dashboard icons and text from being clipped on the left after the application window is narrowed and expanded again.

## Cause

Split mode binds the dashboard and chat `minWidth` and `prefWidth` properties to a share of the wide split container. Calling `unbind()` while returning to normal mode leaves the last calculated values in those properties. The vertically stacked workspace therefore still contains children whose minimum width belongs to the wider split layout. Because the parent `VBox` is center-aligned, those oversized children extend beyond both sides of the viewport and their left edge is clipped.

The horizontal `ScrollPane` range can make the symptom more visible, but it is not the source of the full-workspace shift shown after a split-to-normal transition.

## Design

- Keep the responsive single-column and two-column layouts unchanged.
- Keep the horizontal scrollbar disabled and preserve all intentional minimum widths inside widget rows.
- Disable the horizontal axis at the control level by setting `hmin`, `hmax`, and `hvalue` to `0`.
- Before calculating any split layout, unbind both panes and restore their neutral width contract: computed preferred width and zero minimum width.
- When leaving split mode, apply the same neutral width contract before reattaching the panes to the normal vertical workspace.
- After both a split-mode transition and a `SideExpandButton` Stage resize, request the existing dashboard CSS/layout and height refresh on the next JavaFX cycle.
- Preserve vertical scroll position behavior and the existing user-interaction guard for vertical scrolling.

## Verification

- Resize the application from wide to narrow and back to wide after entering chat text.
- Repeat the sequence with automatic split mode enabled and with the header split toggle.
- Confirm a split-to-normal transition does not retain either pane's previous minimum or preferred width.
- Confirm the music icon, task icon and checkboxes, calendar times, Jira keys, and card borders remain fully visible.
- Confirm the two-column layout still activates at the existing breakpoint.
- Confirm vertical dashboard scrolling still works when content exceeds the available height.
- Run the existing dashboard layout and CSS contract tests, then run the full Gradle build.
