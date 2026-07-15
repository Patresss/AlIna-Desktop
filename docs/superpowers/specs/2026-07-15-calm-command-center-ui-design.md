# Calm Command Center UI/UX Design

## Summary

Refresh AlIna Desktop as a calm, modern command center for daily work. The workspace dashboard remains the primary surface, while chat becomes a context-aware workbench within the same visual system. The change preserves existing integrations, storage, chat behavior, split mode, and backend APIs.

The approved direction is **A. Calm Command Center**.

## Goals

- Make the dashboard the obvious daily starting point.
- Improve readability, information hierarchy, and scan speed at the default 760 px window width.
- Give dashboard widgets, chat, composer, tabs, modals, and settings one coherent component language.
- Preserve light, dark, third-party Atlantafx, and user-provided themes.
- Ensure component styles never hardcode color values.
- Preserve existing behavior and data sources while improving presentation.

## Non-goals

- No backend API, persistence, or integration changes.
- No new remote data sources or background polling.
- No replacement of JavaFX, Atlantafx, or the WebView chat renderer.
- No new user-configurable layout builder.
- No redesign of product branding assets or application icons.

## Visual Direction

The interface uses a restrained workstation aesthetic: layered surfaces, a clear typographic scale, compact status badges, soft borders, and one dominant accent supplied by the active theme. Inter remains the application typeface. Spacing follows a compact 4/8 px rhythm, with larger gaps used to separate sections rather than adding decorative dividers.

The design avoids excessive glass effects, saturated fills, and large repeated message bubbles. Emphasis comes from hierarchy, spacing, weight, and semantic states.

## Application Layout

### Header

The header presents application/workspace context on the leading side and groups global actions on the trailing side. The event countdown remains visible but becomes a compact contextual status rather than a competing headline. Global icon buttons receive consistent sizing, focus states, selected states, and tooltips.

### Dashboard

`DashboardContainer` remains the owner of dashboard visibility and collapse behavior. It gains:

- a workspace/day header;
- a concise summary based on data already loaded by child widgets;
- a responsive card grid;
- a single collapse control and clearer expanded/collapsed states.

At the default 760 px application width, the expanded dashboard uses two columns. The tasks card spans both columns; calendar, GitHub, Jira, Obsidian, and media form the supporting two-column layer below it. When the available dashboard width falls below 680 px, the supporting layer switches to one column. The same rule applies when the application enters split mode.

In the normal vertical layout, the expanded dashboard receives at most 52% of the available content height and scrolls internally when required. This guarantees that the chat composer and a useful portion of the conversation remain available. In split mode, dashboard and chat use the full available height side by side. A collapsed dashboard yields its content space to chat.

The responsive decision belongs to the dashboard layout container. Individual widgets remain focused on rendering their own data and do not calculate application layout.

### Chat Workspace

The chat area is visually distinct from the dashboard without looking like a separate application. Tabs sit directly above the conversation surface and remain hidden for a single conversation. The active tab uses a clear indicator; inactive tabs stay quiet.

The composer is a single rounded work surface containing the input, attachment state, command/context information, model selector, and primary stream action. Send and stop occupy the same primary action slot according to runtime state. Destructive or infrequent actions, including clearing chat, are visually demoted.

## Component Design

### Dashboard Cards

Every dashboard card uses the same anatomy:

1. icon and title;
2. optional count or state badge;
3. primary content rows;
4. contextual action area;
5. local empty, loading, or error state.

Rows use stable alignment for labels and metadata. AI actions appear on pointer hover and keyboard focus, while remaining reachable without a pointer. Task completion, current calendar events, Jira states, and review states use semantic theme colors rather than bespoke per-widget colors.

### Chat Messages

User messages become compact, accent-associated blocks. Assistant responses use a reading surface closer to a document than a large chat bubble. Tool calls, plans, todo lists, approvals, errors, and status messages retain distinct cards because their behavior differs from ordinary prose.

The WebView continues to render the conversation. Its CSS adopts the same spacing, surface, border, typography, and semantic color rules as JavaFX.

### Composer

The composer provides:

- a generous but compact text input;
- integrated attachment previews;
- command/context and model metadata within the same surface;
- one prominent send/stop control;
- visible focus and disabled states;
- preserved keyboard behavior and streaming controls.

### Modals, History, and Settings

Modal surfaces receive a consistent header, content section rhythm, and footer/actions area. Settings are grouped into titled sections with related controls presented together. Conversation history and command surfaces use the same cards, row states, and typography as the dashboard.

## Theme and CSS Architecture

### Color ownership

Concrete color values such as hex, `rgb()`, `rgba()`, or named colors such as `white` and `black` belong only to concrete theme stylesheets. Component styles consume looked-up colors from the active theme. `transparent`, `inherit`, and `currentColor` remain valid because they do not introduce palette values.

`index.css`, the new component stylesheets, `context-menu.css`, and `browser-chat.css` use only semantic theme tokens such as:

- `-color-bg-default`, `-color-bg-subtle`, `-color-bg-inset`;
- `-color-fg-default`, `-color-fg-muted`, `-color-fg-subtle`;
- `-color-border-default`, `-color-border-muted`, `-color-border-subtle`;
- `-color-accent-*`, `-color-success-*`, `-color-warning-*`, `-color-danger-*`, and `-color-info-*`.

JavaFX custom tokens that currently contain hardcoded glass and shadow colors in `index.css` are removed and expressed through existing standard theme tokens. This refresh does not introduce a new AlIna-specific color token.

This keeps third-party Atlantafx and user themes compatible because the refreshed components primarily rely on the standard Atlantafx token contract.

### File responsibilities

- `index.css`: application shell and shared control behavior.
- `workspace.css`: dashboard container, cards, widgets, rows, and responsive states.
- `chat-shell.css`: JavaFX tabs, composer, attachments, and chat chrome.
- `settings.css`: modal, settings, history, command, and form presentation.
- `browser-chat.css`: WebView conversation content.
- concrete theme stylesheets: literal color values and theme-specific token overrides.

`ThemeManager` loads application stylesheets in a deterministic order after the active user-agent theme. The duplicate direct addition of `index.css` during application launch is removed so a stylesheet is applied exactly once.

### WebView token flow

`Browser.updateCssColors()` already parses colors from the active theme and publishes them as CSS custom properties. The refreshed WebView styles continue through that path. The implementation extends parsing only if a newly used standard token is not currently matched; it does not create a second palette in JavaScript.

## Data Flow and State

Existing widgets continue to fetch and render their data through `BackendApi`. No extra backend calls are introduced for the dashboard summary. The UI introduces a small immutable `DashboardSummary` value containing optional open-task count, pull-request count, and next-event time. The task, GitHub, and calendar widgets publish these values after their normal refresh; `DashboardContainer` merges the latest values and hides any metric that has not been supplied. The local date and greeting come from the system clock.

Runtime state flows remain unchanged:

- workspace events refresh affected widgets;
- chat events update the active `ChatWindow` and WebView;
- theme events refresh both JavaFX and WebView colors;
- width changes update only the dashboard layout mode;
- send/stream state updates the composer's primary action.

Each widget owns its content state. `DashboardContainer` owns only composition, responsive placement, overall visibility, and collapse state.

## Empty, Loading, and Error States

- Missing information is not displayed as a zero metric.
- Loading is local to the widget that is refreshing.
- Integration failures render a concise inline state inside the affected card and do not collapse the whole dashboard.
- Long labels use truncation by default and expose the full value through existing expansion or tooltip behavior.
- Disabled actions remain visible when their location matters, with an explanatory tooltip where practical.
- Existing backend failures and logs remain unchanged; this project changes presentation, not recovery policy.

## Accessibility and Interaction Rules

- All icon-only actions have tooltips or accessible labels.
- Hover-only actions are also exposed by keyboard focus.
- Focus indicators use the active theme accent and are not removed.
- Text and interactive targets maintain readable contrast through semantic theme tokens.
- The design supports mouse and existing keyboard shortcuts.
- Motion is brief and functional. Theme transition behavior remains intact, and no continuous decorative animation is added.

## Verification

### Automated

- Run `./gradlew test` and `./gradlew build`.
- Add a CSS contract test for refreshed application stylesheets. It verifies that component CSS contains no literal hex, `rgb()`, `rgba()`, or named palette colors.
- Add focused tests for extracted responsive layout decisions if the breakpoint logic is implemented outside JavaFX node code.

Minified third-party assets, syntax-highlighting themes, images, and concrete theme stylesheets are outside the literal-color scan.

### Manual UI QA

Check:

- one light and one dark built-in theme;
- themes offered by the application selector, including at least one third-party Atlantafx theme;
- default width, narrow width, expanded width, and split mode;
- expanded and collapsed dashboard;
- empty, populated, loading, and integration-error cards;
- one and multiple chat tabs;
- message streaming, stopping, commands, attachments, tool/plan cards, and approvals;
- settings, history, commands, and context menus;
- keyboard traversal and visible focus states.

## Success Criteria

- The dashboard is immediately recognizable as the primary daily workspace.
- Tasks, next calendar context, and integration state can be scanned without competing visual noise.
- Chat and dashboard feel like one product while retaining distinct purposes.
- Default, expanded, and split layouts remain usable without clipping or horizontal scrolling.
- Refreshed component CSS contains no literal color values.
- Switching supported themes updates JavaFX and WebView consistently.
- Existing behavior, integrations, persistence, shortcuts, and chat streaming remain functional.
