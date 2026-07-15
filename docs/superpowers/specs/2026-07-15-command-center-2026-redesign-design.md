# Command Center 2026 Redesign

## Context

The first Calm Command Center implementation improved hierarchy but constrained the dashboard to roughly one third of the window and placed supporting widgets in independent vertical columns. This clipped the daily overview and allowed collapsed cards to create uneven column edges. It also added component styles without exposing the selected visual direction as a concrete, selectable application theme.

The user explicitly requested an autonomous redesign without further questions. This document records the chosen direction before implementation.

## Chosen Direction

Use an aligned bento grid rather than independent masonry columns. Tasks span the full width. Calendar and GitHub share one grid row; Jira and Obsidian share the next; media spans the full width below them. Cards in a two-column row fill the same row height, so collapsing either card never shifts the following row out of alignment. Below the responsive breakpoint, the dashboard becomes a single ordered column.

## Dashboard Height and Scrolling

In the normal vertical workspace, an expanded dashboard receives approximately 58% of the available content height, capped at 62%. This is sufficient to show the normal daily overview at the application's common window sizes while leaving the chat and composer usable. The dashboard remains internally scrollable only when expanded widget content genuinely exceeds that space. A collapsed dashboard becomes a compact command bar. Split mode continues to use the full available height.

The dashboard opens at its top position and preserves user scrolling after the first explicit scroll gesture.

## Visual Language

The command center becomes a quiet, high-contrast work canvas:

- a compact greeting and status rail instead of three oversized metric boxes;
- one continuous summary surface with subtle internal divisions;
- aligned cards with larger radii, calmer borders, and consistent internal padding;
- full-width priority tasks with supporting widgets arranged as a balanced bento grid;
- stronger typography for values and quieter labels and metadata;
- no decorative gradients, glass effects, or literal colors in component styles.

## Concrete Theme

Add a selectable `Calm Command Center` light theme. Its concrete CSS owns all literal palette values and imports the complete existing Allegro control foundation. It overrides the standard Atlantafx semantic tokens with a neutral cool canvas, ink typography, and a restrained blue accent.

Register the theme in `ThemeRepository` and `ThemeManager.PROJECT_THEMES`. Component files (`index.css`, `workspace.css`, `chat-shell.css`, `settings.css`, `context-menu.css`, and `browser-chat.css`) continue to consume only semantic tokens.

## Component Boundaries

- `DashboardContainer` owns responsive placement and overall collapse state.
- `GridPane` owns aligned two-column rows; widgets do not calculate their neighbors' positions.
- Each widget retains its current data fetching, local collapse behavior, loading, error, and empty states.
- `ApplicationWindow` owns the vertical allocation between dashboard and chat.
- The new theme class and CSS own palette selection; component CSS owns layout and appearance without concrete colors.

## Verification

- Test responsive layout decisions at the existing breakpoint.
- Add or extend tests to prove the concrete theme is registered and the CSS resource exists.
- Keep the component color-contract test green.
- Run `./gradlew test` and `./gradlew build`.
- Visually verify the populated dashboard, a collapsed supporting card next to an expanded card, the whole-dashboard collapsed state, and the selectable new theme.

## Success Criteria

- The normal populated dashboard is no longer visibly cut at the previous one-third boundary.
- Two-column rows remain aligned when either widget is collapsed.
- `Calm Command Center` appears in the application theme selector.
- The active component styles contain no literal palette colors.
- Chat, composer, dashboard shortcuts, integrations, and split mode retain their behavior.
