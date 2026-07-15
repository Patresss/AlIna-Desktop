# Equal Dashboard Row Heights

## Context

The configurable dashboard grid can place two half-width cards in one row. The
current layout keeps both cards at their natural heights, so a short empty card
ends above its taller neighbour. This makes the bento grid look unfinished, as
seen with a short GitHub card next to a taller Calendar card.

## Goal

Cards that share a two-column row must have the same rendered height. The taller
card determines the row height and the shorter card stretches its background and
border to fill that height.

The rule is generic. It applies to every pair produced by
`DashboardGridPlanner`, independently of card type, configured order, content,
or visibility.

## Non-goals

- Do not change card ordering, pairing, visibility, or breakpoint behaviour.
- Do not add persisted settings for height alignment.
- Do not force unrelated rows to have the same height.
- Do not introduce content-based reordering or masonry placement.

## Layout behaviour

`DashboardContainer` continues to place cards directly in `GridPane` cells.
Cards are configured to allow vertical resizing and to fill the height of their
own grid cell. JavaFX already calculates a shared row height from the tallest
preferred-height child, so the shorter sibling can fill that row without manual
height bindings.

The container will:

1. Enable `GridPane` height filling for dashboard cards.
2. Allow card regions to grow up to the calculated row height.
3. Keep grid rows from consuming unrelated surplus vertical space.

In two-column mode, both half-width cards in a row therefore receive the height
of the taller card. A full-width card or an unpaired half-width card occupies its
own row, whose height remains that card's natural preferred height. Single-column
mode also keeps natural per-card heights because every row contains one card.

No CSS or widget-specific logic is required; the card's existing background and
border stretch with its region.

## Testing

The existing layout test that expects different heights will be replaced with a
test proving that a short card stretches to the taller sibling's height. A second
test will verify that a single card in its own row keeps its natural height.

The implementation is complete when:

- Calendar and an empty GitHub card render at equal height when paired.
- Jira and Notes, or any other eligible pair, follow the same rule.
- A full-width or single-column card is not made taller by cards in other rows.
- Existing ordering, breakpoint, and planner tests still pass.
- The full Gradle build succeeds.
