# Stabilize Dashboard Horizontal Position

## Goal

Prevent dashboard icons and text from being clipped on the left after the application window is narrowed and expanded again.

## Cause

`SideExpandButton` changes the Stage's X position and width sequentially, which can temporarily make dashboard content wider than the `ScrollPane` viewport. `HbarPolicy.NEVER` hides the horizontal scrollbar but leaves the default horizontal value range active. The JavaFX skin can therefore apply a new horizontal offset after a one-shot reset has already run.

## Design

- Keep the responsive single-column and two-column layouts unchanged.
- Keep the horizontal scrollbar disabled and preserve all intentional minimum widths inside widget rows.
- Disable the horizontal axis at the control level by setting `hmin`, `hmax`, and `hvalue` to `0`.
- Remove the previous immediate and deferred horizontal-reset listeners because the control can no longer represent a non-zero horizontal position.
- After `SideExpandButton` finishes changing Stage bounds, request the existing dashboard CSS/layout and height refresh on the next JavaFX cycle.
- Preserve vertical scroll position behavior and the existing user-interaction guard for vertical scrolling.

## Verification

- Resize the application from wide to narrow and back to wide after entering chat text.
- Confirm the music icon, task icon and checkboxes, calendar times, Jira keys, and card borders remain fully visible.
- Confirm the two-column layout still activates at the existing breakpoint.
- Confirm vertical dashboard scrolling still works when content exceeds the available height.
- Run the existing dashboard layout and CSS contract tests, then run the full Gradle build.
