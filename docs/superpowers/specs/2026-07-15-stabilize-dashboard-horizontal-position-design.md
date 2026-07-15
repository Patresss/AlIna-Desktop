# Stabilize Dashboard Horizontal Position

## Goal

Prevent dashboard icons and text from being clipped on the left after the application window is narrowed and expanded again.

## Cause

During responsive resizing, dashboard content can temporarily be wider than the `ScrollPane` viewport. JavaFX can then retain a non-minimum horizontal scroll value even though the horizontal scrollbar is hidden. When the window becomes wide again, the dashboard remains offset to the left.

## Design

- Keep the responsive single-column and two-column layouts unchanged.
- Keep the horizontal scrollbar disabled and preserve all intentional minimum widths inside widget rows.
- Normalize the dashboard `ScrollPane` horizontal value to its minimum whenever the viewport width changes.
- Repeat the normalization after the queued CSS/layout and content-height refresh, ensuring temporary responsive overflow cannot leave a stale offset.
- Reuse the existing coalesced dashboard layout refresh so continuous window dragging does not create an unbounded queue of JavaFX callbacks.
- Apply the same normalization after dashboard collapse-state changes and layout-mode rebuilds.
- Preserve vertical scroll position behavior and the existing user-interaction guard for vertical scrolling.

## Verification

- Resize the application from wide to narrow and back to wide after entering chat text.
- Confirm the music icon, task icon and checkboxes, calendar times, Jira keys, and card borders remain fully visible.
- Confirm the two-column layout still activates at the existing breakpoint.
- Confirm vertical dashboard scrolling still works when content exceeds the available height.
- Run the existing dashboard layout and CSS contract tests, then run the full Gradle build.
