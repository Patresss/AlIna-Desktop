# Chat Surface Dashboard Style Design

## Summary

Restyle every dashboard widget so it belongs to the same visual family as the current Browser Web chat while preserving the existing JavaFX dashboard structure and behavior. The approved direction is **A. Shared surface family**: overlay surfaces, 16 px rounded cards, restrained elevation, accent icons, compact pills, and subtle row hover states.

The dashboard keeps its current bento layout, configured card widths and ordering, collapse behavior, data refresh, and split-mode behavior. No animations are added.

## Goals

- Make Music, Tasks, Calendar, GitHub, Jira, and Obsidian visibly consistent with the WebView conversation surface.
- Reuse the chat's surface hierarchy: overlay cards, subtle nested rows, semantic tints, pills, and restrained hover feedback.
- Preserve the dashboard's compact information density and scan speed.
- Keep light, dark, third-party Atlantafx, and user-provided themes compatible.
- Consolidate dashboard component styling in `workspace.css`.

## Non-goals

- No changes to dashboard data, backend calls, refresh intervals, persistence, settings, or integration behavior.
- No changes to the bento grid, responsive breakpoint, card ordering, half-width settings, height policy, or split-mode sizing.
- No changes to widget collapse behavior or the overall dashboard collapse handle.
- No JavaScript transfer from `browser-chat.js` into the JavaFX dashboard.
- No entry, hover, expand, collapse, or continuous animation.
- No redesign of chat, composer, tabs, settings, application header, or other application surfaces.

## Approved Visual Direction

### Shared surface language

Dashboard cards mirror ordinary assistant-message surfaces:

- `-color-bg-overlay` provides the primary card surface;
- 16 px rounded geometry matches `.chat-message`;
- a soft theme-derived drop shadow provides restrained elevation;
- hover increases elevation without translating or resizing the card;
- component styles contain no literal palette colors.

The dashboard remains denser than a conversation message. It reuses the visual language rather than copying the chat card's content padding or reading width literally.

### Card anatomy

All six widgets keep their current node hierarchy and use the same visual anatomy:

1. accent-colored icon and compact title;
2. optional count or state pill;
3. primary content rows;
4. existing contextual actions;
5. local empty, loading, or error state.

Cards use consistent padding and header spacing. Their content continues to determine card height through the existing layout and height-policy code.

### Headers and badges

Widget titles remain compact and bold. Their icons use `-color-accent-fg`, matching accent details in the chat. Count badges use the existing accent-subtle/accent-foreground pair and fully rounded pill geometry.

Collapse and media controls remain in their current positions. Their default state is quiet; hover uses a subtle theme surface. Focus indicators provided by JavaFX and Atlantafx are preserved.

### Content rows

Task, pull-request, Jira, calendar, and Obsidian rows are transparent by default. Hover uses `-color-bg-subtle` with 8–10 px rounded geometry, corresponding to compact process and todo rows in the chat.

Typography and metadata hierarchy remain unchanged in meaning:

- primary labels use `-color-fg-default`;
- secondary metadata uses `-color-fg-muted`;
- identifiers and actionable metadata keep the accent foreground;
- pills and status badges retain their current semantic purpose.

No row gains a persistent nested card background. This avoids the visually heavy literal-chat-card option and keeps lists easy to scan.

### Semantic and special states

Jira status, current calendar state, task completion, integration errors, and empty states continue using the existing semantic theme tokens. Styling may refine background, border, radius, and spacing, but it must not change state meaning or interaction behavior.

The overall dashboard collapse handle becomes a compact surface related to chat activity panels: overlay or subtle background, rounded geometry, low visual weight, and accent feedback on hover. It does not animate.

The dashboard scrollbar adopts the narrow, rounded, unobtrusive treatment used by the WebView chat while remaining a JavaFX scrollbar.

## CSS Architecture

### File ownership

- `workspace.css` is the single source of truth for dashboard container, cards, headers, rows, badges, widget-specific states, scrollbar, and collapse handle.
- The legacy dashboard override block in `index.css` is removed so it cannot compete with `workspace.css` or reappear when stylesheet order changes.
- `browser-chat.css` and `browser-chat.js` remain unchanged.
- Java, FXML, language resources, settings models, and backend code remain unchanged unless verification exposes an existing style-class defect that prevents the approved CSS from applying. Such a defect is outside this styling pass and must be reported rather than silently expanding scope.

`ThemeManager` already loads `index.css` before `workspace.css`. Consolidation removes dependence on that cascade order for dashboard appearance.

### Chat-to-JavaFX mapping

The implementation maps WebView CSS variables to their existing JavaFX looked-up-color equivalents:

- `var(--color-bg-overlay)` → `-color-bg-overlay`;
- `var(--color-bg-subtle)` → `-color-bg-subtle`;
- `var(--color-fg-default)` → `-color-fg-default`;
- `var(--color-fg-muted)` → `-color-fg-muted`;
- `var(--color-accent-fg)` → `-color-accent-fg`;
- semantic success, warning, danger, and info variables → their existing JavaFX `-color-*` tokens.

JavaFX effects use the corresponding theme border token rather than introducing a literal shadow color.

## Behavior and Data Flow

Runtime behavior remains unchanged:

1. widgets fetch and render data through their existing `BackendApi` calls;
2. existing events and timers refresh the same widgets;
3. `DashboardContainer` retains layout, visibility, and overall collapse ownership;
4. each widget retains its local content and collapse behavior;
5. JavaFX CSS alone determines the revised appearance.

The JavaScript in `browser-chat.js` is a design reference only. Its DOM construction, expand/collapse helpers, and animation behavior are not applicable to JavaFX dashboard nodes and are not copied.

## Edge and Error States

- Empty cards remain readable and do not collapse unexpectedly.
- Loading and integration errors stay local to the affected widget.
- Long titles continue using existing truncation, wrapping, and expansion behavior.
- Current and upcoming calendar entries remain distinguishable.
- Jira status badges remain legible in every semantic state.
- Cards remain usable in one-column, two-column, narrow, and split layouts.
- Hover elevation must not alter card size or grid placement.
- Keyboard focus remains visible on buttons, checkboxes, and links.
- The design must remain legible in at least one light and one dark built-in theme.

## Verification

### Automated

- Run `./gradlew test`.
- Run `./gradlew build`.
- Keep `ApplicationStylesheetContractTest` passing so component CSS contains no literal palette colors.
- Keep calendar timeline and dashboard layout/height contract tests passing.
- Confirm that the implementation diff does not modify `browser-chat.css`, `browser-chat.js`, Java, FXML, language resources, or backend code.

### Manual UI QA

Check in a freshly started application:

- Music, Tasks, Calendar, GitHub, Jira, and Obsidian cards;
- populated, empty, loading, and integration-error states where available;
- default and hover appearance for cards and rows;
- buttons, checkboxes, links, pills, and visible keyboard focus;
- expanded and collapsed widget states;
- expanded and collapsed overall dashboard states;
- one-column, two-column, narrow-window, and split-mode layouts;
- one light and one dark built-in theme.

## Success Criteria

- Dashboard cards and WebView chat messages visibly belong to one surface family.
- Dashboard information density and layout remain unchanged.
- All widgets use consistent card, header, badge, row, and hover styling.
- There are no dashboard animations.
- Dashboard styling has one authoritative component stylesheet.
- Supported themes continue to drive all component colors.
- Existing dashboard behavior and automated tests remain functional.
