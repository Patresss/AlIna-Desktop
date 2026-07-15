# Remove Dashboard Summary and Stabilize Expansion

## Goals

- Remove the redundant dashboard summary strip showing task, pull request, and next-event values.
- Ensure collapsing and expanding the dashboard recalculates its full content height before applying the normal height policy.
- Avoid dashboard scrolling whenever all visible widgets fit alongside the reserved chat workspace.

## Dashboard Composition

`DashboardContainer` will contain only the widget area and the bottom collapse handle. The summary strip, summary cards, `DashboardSummary` model, summary-only listeners, summary-only translations, and summary-only CSS rules will be removed.

The active-event label used inside the calendar remains in scope and keeps its existing localized `Now` value.

## Expansion Lifecycle

When the dashboard expands in normal mode:

1. Clear the fixed collapsed height by restoring computed minimum and preferred heights and an unbounded maximum height.
2. Request a fresh JavaFX layout pass.
3. On the following JavaFX cycle, apply CSS and layout to the dashboard content before measuring its preferred height.
4. Feed the fresh content height into the existing `DashboardHeightPolicy`.
5. Apply the resulting fixed viewport height and reset the scroll position to the top.

The queued refresh remains coalesced so multiple height and visibility notifications produce one measurement. Split mode retains its current independent width-based behavior.

## Height Rules

- If all dashboard content fits while preserving the configured minimum chat workspace, show every widget without a dashboard scrollbar.
- If the window is too short, cap the dashboard height using the existing policy and keep scrolling available.
- The collapsed dashboard remains 30 px high and visually transparent except for the primary-colored chevron.

## Cleanup

Remove callback fields and setter methods from dashboard widgets when their only consumer was the deleted summary strip. Preserve data fetching, widget counts, calendar rendering, and integration behavior.

## Verification

- Confirm the summary strip is absent when the dashboard is expanded.
- Exercise expand, collapse, and expand again; the second expansion must show the same complete dashboard layout as the first when the window has enough space.
- Confirm the dashboard scroll position starts at the top after expansion.
- Confirm constrained windows still scroll and leave usable chat space.
- Run focused height-policy and CSS color-contract tests, then run the full Gradle build.
