# Balanced dashboard masonry — design

## Goal

Replace the row-coupled dashboard grid with a generic, content-aware layout. Empty or short cards must keep their natural height instead of stretching to match a tall neighbour. The same mechanism must remain visually balanced when any widget, such as Calendar or Jira, becomes much taller than the others.

## Scope

- Keep Music and Tasks as full-width cards at the top of the dashboard.
- Arrange Calendar, GitHub Reviews, Jira Issues, and Notes as masonry cards below them.
- Preserve the current dashboard collapse control, widget visibility settings, card collapse controls, refresh behaviour, result limits, and theme styling.
- Do not add widget-specific placement rules, nested scrolling, new result-fetching behaviour, or new settings.

## Layout behaviour

### Wide mode

At and above the existing two-column breakpoint:

1. Music and Tasks remain in semantic order and span the full available width when visible.
2. All managed secondary cards participate in one two-column masonry region.
3. Both columns have exactly the same width, separated by the existing 10 px gap.
4. Cards keep their preferred height for the calculated column width. A card is never stretched to the height of a card in the other column.
5. A single visible secondary card spans the full width. No secondary card leaves a permanently reserved empty track.

The masonry algorithm is deterministic and widget-agnostic:

1. Read managed cards in semantic order: Calendar, GitHub Reviews, Jira Issues, Notes.
2. Measure each card's preferred height at the final column width.
3. Plan cards from tallest to shortest, retaining semantic order as the tie-breaker for equal heights.
4. Assign each planned card to the column with the smaller accumulated height, including the vertical gap between cards.
5. Resolve equal accumulated heights in favour of the left column.
6. Lay out the assigned cards in their original semantic order within each column.

Consequently, one tall Jira card may occupy one column while Calendar, GitHub, and Notes occupy the other. If Jira is shorter, another card can be placed beneath it. The algorithm balances total column height rather than card count.

### Narrow and split mode

Below the existing two-column breakpoint, all visible cards use one column in semantic order:

1. Music
2. Tasks
3. Calendar
4. GitHub Reviews
5. Jira Issues
6. Notes

Every card spans the available width and retains its natural height. Resizing from wide to narrow and back must rebuild the same deterministic placement without clipping or preserving stale coordinates.

### Dynamic changes

The masonry region recalculates placement when:

- its width or layout mode changes;
- a widget becomes visible or hidden;
- a card is expanded or collapsed;
- refreshed content changes a card's preferred height.

Reflow happens through the JavaFX layout lifecycle. It must not poll continuously or mutate widget content. A card may move between columns only after one of these inputs changes.

## Empty and large states

- Existing empty-state content stays compact and uses the card's natural preferred height.
- Empty cards remain visible when their integration is configured, so users can distinguish an empty result from a disabled integration.
- Existing Jira, GitHub, Tasks, and Notes result limits continue to control list size. This layout change does not introduce a second limit or an internal scrollbar.
- Whitespace may remain below the shorter column, but it remains outside cards. No empty card is expanded to fill it.

## Implementation structure

Introduce a focused dashboard masonry region responsible only for measuring and positioning managed child cards. It exposes a one-column or two-column mode and a configurable gap. It does not know widget types, data models, settings, or refresh logic.

`DashboardContainer` remains responsible for:

- full-width Music and Tasks placement;
- semantic ordering of secondary cards;
- switching layout mode at the existing breakpoint;
- widget visibility and the global dashboard collapse state.

Remove vertical growth constraints that currently force dashboard cards to fill a shared grid row. Cards retain zero minimum width and an unbounded maximum width so their rows can truncate correctly within equal-width columns.

Keep the height-balancing calculation independent from JavaFX nodes where practical: a small deterministic planner accepts measured heights and returns column assignments. The planner uses longest-processing-time-first balancing, which prevents a late tall card such as Jira from being stacked onto a column merely because both columns were equal before measuring it. The layout region then applies those assignments to nodes. This makes balancing behaviour directly unit-testable and scales to future widgets without exhaustive assignment searches.

## Failure and compatibility behaviour

- Integration loading and fetch errors continue to be handled by their existing widgets.
- A hidden or unmanaged widget is excluded from measurement and placement.
- Zero visible secondary widgets produce a zero-height masonry region.
- Invalid or unavailable preferred heights are clamped to a non-negative value so one widget cannot corrupt the entire layout.
- Existing user CSS targeting `.workspace-dashboard` remains valid. Container-level grid classes may be retained as compatibility aliases if removing them would break theme overrides.

## Verification

Add deterministic tests for:

- equal-width two-column tracks after subtracting the gap;
- a tall first card followed by three short cards;
- a tall Jira-equivalent card balancing against the other cards without widget-specific rules;
- empty cards of equal height alternating between columns deterministically;
- hidden cards being excluded;
- one secondary card spanning the full width;
- single-column semantic order;
- wide → narrow → wide resize cycles.

Run `./gradlew test` and `./gradlew build`. Visually verify both themes with:

- an empty Calendar and empty GitHub card;
- a tall Calendar and empty GitHub card;
- a tall Jira card with short remaining cards;
- hidden integrations;
- normal and split-window widths.

## Acceptance criteria

- No card height is determined by a card in the other column.
- Wide mode uses equal-width columns and balances their measured total heights.
- The layout works for current and future secondary cards without widget-specific branching.
- Empty cards look compact rather than broken or unfinished.
- Long content remains clipped or truncated according to the existing widget styles and settings.
- Split mode never clips the dashboard horizontally and restores semantic one-column order.
