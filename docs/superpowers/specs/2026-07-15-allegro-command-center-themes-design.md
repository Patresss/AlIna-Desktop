# Allegro Command Center Themes Design

## Summary

Add two new built-in themes, `Allegro Command Center` and `Allegro Command Center Dark`, to AlIna Desktop. They preserve the Calm Command Center component language and Inter typography while applying a restrained Allegro palette in light and dark modes.

The approved direction is **A. Restrained accent**: neutral work surfaces dominate, while Allegro orange identifies focus, selection, links, active states, and primary actions.

## Goals

- Add a new light/dark theme pair without replacing any existing theme.
- Preserve the current Command Center layout, spacing, radii, shadows, and Inter typography.
- Anchor the palettes in Allegro's official orange, neutral silver, and black.
- Keep dashboard, JavaFX chat chrome, WebView chat content, settings, modals, and context menus visually coherent.
- Maintain readable contrast for ordinary text, links, neutral surfaces, and semantic status colors.
- Make both themes available through the existing theme selector and preferences flow.

## Non-goals

- Do not modify or replace `Calm Command Center` or `Calm Command Center Dark`.
- Do not repurpose the legacy `Allegro` and `Allegro Dark` classes or stylesheets as the new selectable themes.
- Do not change component layout, behavior, animation, application logic, persisted settings, or data flow.
- Do not introduce Allegro logos, imagery, marketplace-specific components, or Open Sans.
- Do not add the Allegro brand manual PDF to the repository.

## Brand Sources

The official [Allegro Brand Manual](https://assets.allegrostatic.com/display-pl/repozytorium/logo/Brandbook_Allegro_Wersja%20Skrocona.pdf) defines these palette anchors:

- leading orange: `#ff5a00`;
- neutral silver: `#e6e6e6`;
- additional black: `#000000`.

The official [Allegro Advertising Specification](https://assets.allegrostatic.com/display-pl/documents/Advertising%20specification%20Allegro%20EN.pdf) publishes button templates that use white text on a `#ff5a00` background.

The brand manual specifies Open Sans, but this theme deliberately retains AlIna's Inter typography as approved. Allegro engineering material also confirms that the current native application supports [light and dark themes](https://blog.allegro.tech/2026/02/battle-testing-lynx-js-at-allegro.html). The dark palette in this design is therefore an accessibility-oriented AlIna adaptation anchored in the official brand colors, not a claim that every derived tone is an independently published brand color.

## Visual Direction

The themes retain the calm workstation character of the existing Command Center. Orange is a navigational and interaction signal rather than a large decorative fill. It appears in focus indicators, selected and active controls, links, compact card markers, and primary actions. Large application, card, chat, and modal surfaces remain neutral.

Semantic success, warning, danger, and information colors remain visually distinct. They must not be collapsed into the brand accent. Filled accent controls, focus rings, and small brand accents use the official `#ff5a00` directly. Their foreground remains white, matching Allegro's official button specification.

White on `#ff5a00` has a contrast ratio of approximately 3.13:1. It clears the 3:1 non-text threshold used by the icon-only chat primary action, but not the 4.5:1 threshold for normal text. Exact Allegro button fidelity is the explicitly approved exception; ordinary accent text continues to use accessible foreground variants.

## Theme Architecture

### New project themes

Add four new files:

- `AllegroCommandCenterTheme.java`;
- `AllegroCommandCenterDarkTheme.java`;
- `allegro-command-center-theme.css`;
- `allegro-command-center-dark-theme.css`.

The Java classes implement Atlantafx `Theme`, expose the approved names, return the corresponding classpath stylesheet, return `null` for BSS, and report the correct dark-mode flag.

The new light stylesheet imports the legacy `allegro-theme.css` control foundation. The new dark stylesheet imports `allegro-dark-theme.css`. Each new stylesheet then overrides semantic and scale tokens in `.root`, following the same overlay pattern as the Calm Command Center themes. The legacy files remain unchanged and are implementation foundations only.

Register both new theme classes in:

- `ThemeRepository.internalThemes`, so users can select and persist them;
- `ThemeManager.PROJECT_THEMES`, so `SamplerTheme` treats their resources as internal project themes.

No selection or settings code changes are required because the existing repository-driven menus and settings controls already enumerate available themes.

### Token contract

Component styles continue to consume existing Atlantafx semantic tokens. The new themes introduce no component-specific palette token and no literal colors in component stylesheets.

All semantic color declarations required by `Browser.updateCssColors()` remain within the first 250 lines of each new stylesheet because `SamplerTheme.parseColors()` intentionally stops after that limit. This keeps JavaFX and WebView colors on the same source of truth.

The root font family remains `Inter`.

## Palette Specification

The implementation uses the exact surface, foreground, border, and accent roles below. To remove interpretation from the scale construction, the complete base and accent scales are also fixed explicitly.

### Light

| Role | Value | Purpose |
|---|---:|---|
| `-color-dark` | `#000000` | Official black anchor |
| `-color-light` | `#ffffff` | Emphasis foreground and overlay base |
| `-color-bg-default` | `#f7f7f8` | Application background |
| `-color-bg-overlay` | `#ffffff` | Cards, menus, and modals |
| `-color-bg-subtle` | `#f2f2f3` | Quiet secondary surfaces |
| `-color-bg-inset` | `#e6e6e6` | Official neutral anchor |
| `-color-fg-default` | `#1f1f21` | Primary text |
| `-color-fg-muted` | `#66666a` | Secondary text |
| `-color-fg-subtle` | `#8b8b90` | Tertiary text |
| `-color-border-default` | `#ceced1` | Strong boundary |
| `-color-border-muted` | `#d9d9db` | Standard boundary |
| `-color-border-subtle` | `#e6e6e6` | Quiet boundary |
| `-color-accent-5` | `#ff5a00` | Official orange anchor and focus/selection signal |
| `-color-accent-fg` | `#b33c00` | Readable accent text on light surfaces |
| `-color-accent-emphasis` | `#ff5a00` | Official primary-action fill with white foreground |
| `-color-accent-subtle` | `#ffede3` | Quiet selected/active surface |

Light scales:

- `-color-base-0..9`: `#f7f7f8`, `#f2f2f3`, `#e6e6e6`, `#ceced1`, `#b3b3b7`, `#8b8b90`, `#66666a`, `#444447`, `#1f1f21`, `#000000`;
- `-color-accent-0..9`: `#fff7f2`, `#ffede3`, `#ffdecc`, `#ffbd99`, `#ff9c66`, `#ff5a00`, `#d94b00`, `#b33c00`, `#8c2e00`, `#661f00`;
- neutral semantic roles: emphasis-plus `#444447`, emphasis `#66666a`, muted `#b3b3b7`, subtle `#f2f2f3`;
- `-color-accent-muted`: `rgba(255, 90, 0, 0.28)`.

### Dark

| Role | Value | Purpose |
|---|---:|---|
| `-color-dark` | `#090909` | Deep inset surface derived from official black |
| `-color-light` | `#f7f7f8` | Primary light anchor |
| `-color-bg-default` | `#111112` | Application background |
| `-color-bg-overlay` | `#1b1b1c` | Cards, menus, and modals |
| `-color-bg-subtle` | `#202022` | Quiet secondary surfaces |
| `-color-bg-inset` | `#090909` | Recessed surface |
| `-color-fg-default` | `#f2f2f3` | Primary text |
| `-color-fg-muted` | `#a0a0a4` | Secondary text |
| `-color-fg-subtle` | `#737377` | Tertiary text |
| `-color-border-default` | `#3b3b3e` | Strong boundary |
| `-color-border-muted` | `#303033` | Standard boundary |
| `-color-border-subtle` | `#29292b` | Quiet boundary |
| `-color-accent-5` | `#ff5a00` | Official orange anchor and focus/selection signal |
| `-color-accent-fg` | `#ff9c66` | Readable accent text on dark surfaces |
| `-color-accent-emphasis` | `#ff5a00` | Official primary-action fill with white foreground |
| `-color-accent-subtle` | `#301a0e` | Quiet selected/active surface |

Dark scales:

- `-color-base-0..9`: `#f7f7f8`, `#e6e6e6`, `#c4c4c7`, `#a0a0a4`, `#737377`, `#56565a`, `#3b3b3e`, `#29292b`, `#1b1b1c`, `#000000`;
- `-color-accent-0..9`: `#ff9c66`, `#301a0e`, `#45200c`, `#612700`, `#8c3600`, `#ff5a00`, `#ff7b33`, `#ff9c66`, `#ffbd99`, `#ffdecc`;
- neutral semantic roles: emphasis-plus `#c4c4c7`, emphasis `#a0a0a4`, muted `#56565a`, subtle `#202022`;
- `-color-accent-muted`: `rgba(255, 90, 0, 0.32)`.

The semantic info, success, warning, and danger families reuse the accessible values already established by the corresponding Calm Command Center light or dark theme. Their values belong in the new concrete theme stylesheets, keeping component CSS palette-free.

Alpha-based muted status tokens use the same values as the corresponding Calm Command Center theme.

## Component Behavior

No component-specific rules are added. Existing semantic styling yields the intended behavior:

- primary actions and active states receive accent emphasis;
- focus rings, links, selection indicators, and compact markers receive brand orange or its accessible foreground variant;
- cards, chat surfaces, modal surfaces, and dashboard backgrounds remain neutral;
- status states retain their existing semantic hues;
- the JavaFX WebView receives parsed theme tokens through the existing theme-change event path.

This avoids a theme that resembles an Allegro marketplace screen and instead produces an Allegro-accented AlIna workspace.

## Data Flow and Error Handling

There are no data-flow changes. Theme selection continues through `ThemeRepository`, `ThemeManager`, `SamplerTheme`, and the existing preferences model. Theme-change events continue to update JavaFX and WebView content.

Missing or invalid classpath resources are prevented by focused resource tests. Existing resource handling and logging behavior remain unchanged; this feature introduces no new runtime recovery path.

## Verification

### Automated

Add a focused `AllegroCommandCenterThemeTest` that verifies:

- the exact light and dark names;
- correct `isDarkMode()` values;
- both classpath stylesheet resources exist;
- `ThemeRepository` offers both names;
- `ThemeManager.PROJECT_THEMES` contains both classes;
- parsed semantic tokens include the official `#ff5a00` accent anchor and the expected light/dark background values.

Keep the existing application stylesheet literal-color contract unchanged. Run:

- `./gradlew test`.

### Manual UI QA

For both themes, verify:

- selection and persistence through settings and the chat theme menu;
- dashboard cards, active rows, badges, and primary actions;
- JavaFX chat tabs and composer;
- WebView user/assistant messages, links, tool cards, and streaming state;
- settings, modals, history, command surfaces, and context menus;
- hover, focus, selection, disabled, success, warning, danger, and information states;
- readable contrast on standard and narrow layouts, including verification of the approved primary-action contrast exception.

## Success Criteria

- Two new selectable built-in themes appear as `Allegro Command Center` and `Allegro Command Center Dark`.
- Every currently selectable Calm, Atlantafx, and user theme remains available and unchanged.
- The legacy Allegro classes and stylesheets remain unchanged as non-selectable implementation foundations.
- The official orange, silver, and black anchors are represented exactly in the concrete palettes.
- Primary accent controls use the official `#ff5a00` with a white foreground in both themes.
- Orange remains restrained and interaction-focused rather than becoming a large surface fill.
- JavaFX and WebView use the same active theme tokens.
- Inter and all existing Command Center layout and behavior remain unchanged.
- The automated test suite passes.
