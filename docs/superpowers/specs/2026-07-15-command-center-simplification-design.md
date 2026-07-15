# Calm Command Center — simplification amendment

## Goal

Remove redundant product and greeting chrome so the workspace starts with useful information, while making calendar state and chat roles easier to scan.

## Native header

- Remove the AlIna icon, product name, and decorative divider from the leading title-bar area.
- Retain only the current-event countdown as useful contextual information.

## Dashboard

- Remove the complete greeting header, including its title, subtitle, date, and visible global collapse control.
- Start the dashboard with the summary strip, followed by Music, Tasks, and the supporting bento cards.
- Preserve keyboard-based dashboard collapse and expansion.

## Current calendar event

- Use a rounded accent rail on the left edge as the persistent current-state marker.
- Do not apply a persistent background fill; a lightweight hover state remains available as interaction feedback.

## Chat messages

- Assistant messages remain part of the document surface: no bubble, fill, border, or hover outline.
- User messages align to the right in a compact neutral bubble with a subtle fill and border.
- Avoid using the accent color for the whole user bubble so ordinary conversation does not look like a warning or selected state.
- Continue using theme variables only; no component-level literal colors.

## Verification

- Run the CSS palette contract and the complete Gradle build.
- Launch the application and visually check the title bar, dashboard hierarchy, current event, and chat surface.
