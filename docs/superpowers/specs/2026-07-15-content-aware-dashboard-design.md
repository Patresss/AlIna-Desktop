# Content-aware Calm Command Center — design amendment

## Goal

Make the dashboard use the vertical space that its visible content actually needs, while preserving a usable chat composer and keeping the existing Calm Command Center visual language.

## Layout

- In the normal vertical layout the dashboard height is the smaller of its preferred content height and the window space left after reserving a minimum chat workspace.
- The dashboard keeps an internal scrollbar only when the window is too short to show both the dashboard content and the minimum chat workspace.
- Height is recalculated after window resizing, dashboard width changes, widget visibility changes, and card expansion or collapse.
- Split mode remains independently sized and continues to fill its column.

## Information order

- Remove the calendar date eyebrow from the command-center header; retain the greeting, context subtitle, and global collapse action.
- Place the Music card first, full width, followed by Tasks, then the paired Calendar/GitHub and Jira/Notes cards.
- The same semantic order applies in the single-column layout.

## Calendar active state

- Replace the isolated left-side marker with one subtle, fully rounded accent border around the current event.
- Continue to use theme palette tokens from the active theme; component CSS contains no literal colors.

## Verification

- Unit-test the content-height policy for fitting, constrained, and minimum-height cases.
- Run the complete Gradle build and visually verify the normal layout at the current window size.
