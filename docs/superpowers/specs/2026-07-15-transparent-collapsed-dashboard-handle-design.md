# Transparent Collapsed Dashboard Handle

## Goal

Make the collapsed dashboard visually disappear while preserving a clear, convenient way to restore it.

## Design

- Keep the expanded dashboard handle unchanged so it remains visually attached to the dashboard content.
- Add a dedicated `collapsed` style state to the existing bottom handle.
- In the collapsed state, remove the handle background, border, and hover fill.
- Show only the existing chevron, using the theme's primary/accent foreground token for strong contrast.
- Preserve the current chevron size and the full-width clickable hit area.
- Reduce the collapsed dashboard viewport from 56 px to 30 px, leaving enough room for the icon and pointer target without creating a visible empty panel.
- Keep the existing persisted collapse state and keyboard shortcuts unchanged.

## Theme Contract

All visual values that represent colors must use existing theme tokens. No literal hexadecimal, RGB, or RGBA colors may be introduced.

## Verification

- Confirm the collapsed state shows no dashboard card background or outline and only the primary-colored chevron remains visible.
- Confirm the expanded state retains its current appearance.
- Confirm clicking the full-width handle expands and collapses the dashboard.
- Run the CSS color contract tests, dashboard height policy tests, and the full Gradle build.
