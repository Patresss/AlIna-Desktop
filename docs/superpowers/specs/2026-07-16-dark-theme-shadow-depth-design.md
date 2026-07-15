# Dark Theme Shadow Depth Design

## Problem

Shared JavaFX and WebView styles use border colors as shadow colors. Dark themes
make those borders lighter than the surrounding background, so a blurred shadow
looks like a gray glow around messages, cards, floating controls, and panels.
The issue affects every dark theme, not only Allegro Command Center Dark.

## Goal

Keep a restrained sense of elevation in dark mode without changing borders,
accent colors, surface colors, or light-theme appearance. Dark-theme shadows
must read as depth rather than emitted light.

## Design

### Shared theme mode

JavaFX already applies the `:dark` pseudo-class to the scene root from
`SamplerTheme.isDarkMode()`. Shared JavaFX styles will use that state instead of
checking theme names.

The chat WebView will mirror the same `isDarkMode()` value onto its document
root. Theme changes will update both parsed color variables and the mode marker,
so switching themes does not require a page reload. If no theme is available,
the WebView retains light-mode behavior.

### Semantic shadow color

Component styles will stop using border tokens directly for elevation effects.
They will use a shared shadow color instead:

- Light mode keeps the existing border-derived shadow appearance.
- Dark mode derives the shadow from the theme's inset background color, which
  is darker than elevated surfaces across supported dark themes.
- Borders continue using their original border tokens and remain visible.

The WebView stylesheet will define the equivalent CSS custom property from the
parsed border token in light mode and the parsed inset-background token in dark
mode. Each value retains a border-color fallback for compatibility with
external themes that omit optional tokens.

### Shadow geometry

Dark-mode overrides will reduce every non-empty elevation-shadow blur radius
declared in the app-owned WebView and JavaFX component styles to 75 percent of
its current value, rounded to the nearest whole CSS pixel. Offsets and spread
stay unchanged. This covers chat messages, action/tool cards, floating
controls, the composer, workspace surfaces, settings surfaces, and context
menus. Effects owned by imported theme libraries and small functional shadows
that are already absent or transparent stay unchanged; their color still
benefits from the semantic shadow token where they consume it.

The light-theme shadow geometry remains unchanged.

## Alternatives Considered

### Change only the shadow color

This is the smallest change, but the current large blur radii can still make
dark cards feel hazy even when the color is corrected.

### Remove dark-theme shadows

Borders alone would eliminate the glow, but the interface would lose useful
depth cues and floating controls would blend into the background.

### Add per-theme shadow values

Per-theme values offer maximum control but duplicate the same policy across all
dark themes and fail to cover user-provided dark themes automatically.

## Compatibility

Dark-mode behavior is driven exclusively by `Theme.isDarkMode()`, so built-in
and external dark themes receive the same correction. Existing theme color
parsing remains compatible, and missing optional WebView values fall back to
current border tokens.

## Verification

- Add focused coverage for propagating dark and light mode to the WebView.
- Add CSS contract checks for semantic shadow usage and dark-mode overrides.
- Run the complete Gradle test suite with JDK 25.
- Visually verify a representative built-in dark theme and a light theme,
  checking messages, tool cards, header controls, composer, settings, and
  context menus.

## Non-goals

- Redesigning card borders, backgrounds, radii, or spacing.
- Changing Allegro orange or any other accent palette.
- Introducing theme-name-specific selectors.
- Flattening the interface by removing all elevation effects.
