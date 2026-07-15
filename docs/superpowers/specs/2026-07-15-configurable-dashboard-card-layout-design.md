# Configurable dashboard card layout — design

## Goal

Make dashboard placement predictable and user-configurable without reordering cards according to their current content height. Every card receives a stable numeric order and a setting that controls whether it may share a row at half width. The dashboard also receives a configurable responsive breakpoint.

## Settings model

Add one `DashboardLayoutSettings` aggregate to `WorkspaceSettings` instead of adding individual primitive fields for every card. The aggregate contains:

- `twoColumnBreakpoint`: the dashboard width in logical pixels at which the layout changes from one column to two columns;
- a complete map from each stable `DashboardCardId` string key to `DashboardCardLayoutSettings`;
- one `DashboardCardLayoutSettings` value per card containing `canUseHalfWidth` and `order`.

Known card identifiers and defaults are:

| Card | Default order | Can use half width |
|---|---:|---:|
| Music | 10 | no |
| Tasks | 20 | no |
| Calendar | 30 | yes |
| GitHub Reviews | 40 | yes |
| Jira Issues | 50 | yes |
| Notes / Obsidian | 60 | yes |

The default two-column breakpoint is `680` logical pixels. Widths below the breakpoint use one column. Widths equal to or above the breakpoint use up to two columns.

`DashboardLayoutSettings` normalizes itself into an immutable map containing every known card. Persisting string keys lets normalization ignore unknown future or manually added identifiers without making Jackson enum deserialization fail. A missing aggregate, missing map, or missing card entry receives the defaults above. Orders are clamped to `1–9999`, and breakpoints are clamped to `320–8192`.

This keeps existing JSON configuration files compatible. Loading an old file must reproduce the current visual order and width behaviour without requiring a migration command.

## Dashboard settings UI

Add a numeric breakpoint spinner to the General section:

- label: `Próg dwóch kolumn` / `Two-column breakpoint`;
- description: below this dashboard width, cards use one column;
- allowed UI range: `320–8192` px;
- default: `680` px.

Add two controls to every card section:

- toggle: `Może zajmować pół szerokości` / `Can use half width`;
- numeric spinner: `Kolejność` / `Order`;
- order range: `1–9999`.

The common labels and descriptions are shared translation keys. Reset restores values from the loaded settings. Save creates a complete `DashboardLayoutSettings` value and preserves it when unrelated settings panes rebuild `WorkspaceSettings`.

Saving settings publishes the existing workspace-settings update event. The visible dashboard must update order, pairing, and breakpoint without restarting the application.

## Placement rules

The layout is deterministic and never depends on item count, preferred height, refresh timing, or card type-specific branching.

Before placement:

1. exclude currently unmanaged cards;
2. sort remaining cards by ascending configured `order`;
3. break equal-order ties by the default identifier order in the table above.

### One-column mode

Place every visible card in sorted order. Each card spans the full dashboard width.

### Two-column mode

Read sorted cards from first to last:

1. A card with `canUseHalfWidth = false` always spans both columns.
2. Two consecutive half-width-capable cards share one row, in sorted left-to-right order.
3. A pending half-width-capable card is flushed at full width before a full-width card, so no card crosses another card in visual order.
4. A final unpaired half-width-capable card spans both columns.

Examples:

- Calendar `30` and GitHub `40` share a row.
- If Tasks has order `35` and remains full width, Calendar is flushed full width, Tasks follows full width, and GitHub is considered only after Tasks.
- If GitHub is hidden, an otherwise unpaired Calendar spans the full row.

Cards in a shared row remain top-aligned and keep their natural preferred height. The shorter card must not stretch to the taller card's height. The row itself still advances by the taller card, preserving stable reading order rather than creating masonry behaviour.

## Runtime structure

Introduce a small pure layout planner that accepts ordered card metadata and returns row, column, and column-span placements. The planner owns pairing rules and is independent of JavaFX nodes.

`DashboardContainer` remains responsible for:

- mapping card identifiers to widget nodes;
- reading current layout settings;
- excluding unmanaged widgets;
- applying planner placements to `GridPane`;
- switching layout mode from the configured breakpoint;
- reacting to width, settings, visibility, and managed-state changes.

Remove vertical growth from cards, disable grid fill-height for them, and align them to the top of their cells. Keep exact equal-width column constraints so long content cannot widen one column or cause horizontal clipping.

Managed-state listeners schedule one coalesced layout refresh on the JavaFX application thread. This closes gaps when an integration hides itself after a credential or refresh check without producing repeated immediate rebuilds.

## Compatibility and error handling

- Existing show/hide toggles continue to control whether cards participate.
- Existing card collapse controls, refresh behaviour, result limits, AI actions, and theme classes are unchanged.
- Unknown persisted card identifiers are ignored by the normalized known-card map.
- A card missing from persisted settings receives its identifier defaults.
- Duplicate orders are valid and resolved deterministically.
- Existing split mode uses the actual dashboard width and the configured breakpoint, exactly like normal mode.
- User CSS targeting existing dashboard and grid classes remains valid.

## Verification

Add unit tests for:

- default settings matching the current order and half-width eligibility;
- old JSON without layout settings receiving all defaults;
- partial card maps receiving missing defaults;
- configured breakpoint selection below, at, and above the threshold;
- ascending order and deterministic duplicate-order ties;
- two consecutive eligible cards sharing a row;
- a full-width card flushing a pending eligible card without reordering;
- an odd eligible card spanning both columns;
- hidden cards being excluded;
- top-aligned natural-height cards in an unequal-height shared row;
- settings reconstruction paths preserving the layout aggregate.

Run `./gradlew test` and `./gradlew clean build`. Smoke-test application startup and visually verify:

- the unchanged default layout;
- custom card ordering;
- every card toggled between full and half-width eligibility;
- custom breakpoint behaviour during resize and split mode;
- hidden and dynamically unavailable integrations;
- light and dark themes.

## Acceptance criteria

- Default configuration renders the same card order and pairing as before this feature.
- Changing `order` deterministically changes visual order after saving.
- Card placement never changes merely because its item count or height changes.
- Cards marked as full width never share a row.
- Eligible cards share a row only when adjacent after sorting.
- Short cards are not vertically stretched beside taller cards.
- The configured breakpoint controls the one-column/two-column transition immediately.
- Existing configuration files load without errors and receive safe defaults.
