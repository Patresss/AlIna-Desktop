# Welcome Screen Surface Refresh Design

## Context

The empty-chat welcome screen already has the correct information architecture: the animated AlIna logo, note count, instructional subtitle, rotating tip, recent sessions, pinned commands, and the floating composer. The screen should keep that structure and behavior. This change only aligns its visual treatment with the recently refreshed composer and message surfaces.

## Goals

- Preserve the current centered layout and content order.
- Make the welcome screen feel like part of the same surface family as the floating composer and redesigned chat messages.
- Improve visual hierarchy through restrained surfaces, spacing, typography, and interaction states.
- Keep all existing actions, data, truncation behavior, localization, and particle animation intact.
- Remain legible in light and dark themes and at narrow WebView widths.

## Non-goals

- No changes to the welcome-screen information architecture.
- No new sections, labels, icons, commands, or session metadata.
- No changes to `browser-chat.js`, Java controllers, localization strings, or data flow.
- No redesign of the particle logo or composer.
- No refactoring of unrelated welcome-screen code.

## Approved Visual Direction

The approved direction combines three treatments while retaining the existing order:

1. **Tip — filled accent surface.** The tip uses the theme's subtle accent background and muted accent border. Its star and prefix remain visually emphasized. A slightly larger radius and balanced padding make it read as a compact informational surface rather than a thin banner.
2. **Recent sessions — soft elevated surfaces.** Recent-session chips use the overlay background, a subtle neutral border, and a light shadow. They remain pill-shaped and keep their current three-item centered row and truncation behavior.
3. **Commands — quiet outlined controls.** Command chips remain transparent with a muted accent outline. They have no persistent shadow or filled background. Hover and keyboard focus introduce the subtle accent fill already used elsewhere in the interface.

The animated logo, note count, subtitle, section labels, and overall centered composition remain visually recognizable. Their sizing and spacing may be tuned only enough to create a more deliberate vertical rhythm.

## Component Treatment

### Welcome container and header

`welcome-screen` continues to center its content and reserve the same empty-state area above the composer. Its gap and padding may be adjusted slightly to reduce the disconnected feeling between the logo, subtitle, and actions. The particle canvas dimensions and rendering remain unchanged.

The subtitle remains a single instructional sentence. It receives no card or border. Its type weight and muted color should support the logo without competing with it.

### Tip

The existing `.welcome-tip` markup remains unchanged. Styling uses:

- `--color-accent-subtle` for the background;
- `--color-accent-muted` for the full border;
- `--color-accent-fg` for the star and prefix;
- a radius consistent with compact message surfaces;
- padding and gaps that keep long localized tips readable.

The tip remains content-sized up to its current maximum width and left-aligned internally.

### Recent sessions

`.welcome-chip-recent` remains a pill button with the existing return icon, session name, maximum width, ellipsis behavior, and tooltip logic. Its visual surface uses:

- `--color-bg-overlay` with `--color-bg-default` as fallback;
- a subtle neutral theme border;
- a restrained neutral shadow;
- a slightly stronger hover elevation and border response.

The treatment must remain quieter than the composer so the input still anchors the page.

### Pinned commands

Base `.welcome-chip` command buttons remain transparent and outlined with `--color-accent-muted`. Hover, active, and `:focus-visible` states use the subtle accent fill and visible accent outline. The controls remain pill-shaped, compact, and centered.

Recent-session overrides must remain more specific than base command styling so the two categories preserve distinct surfaces.

### Section labels and spacing

The uppercase section labels remain. Letter spacing, weight, and muted foreground color should match the calm label treatment used elsewhere in the application. Vertical gaps should clearly associate each label with its row without adding containers or dividers.

## Interaction and Accessibility

- All existing click handlers remain unchanged.
- Pointer hover must provide visible feedback without moving surrounding content.
- `:focus-visible` must be at least as clear as hover for keyboard users.
- Button text continues to truncate safely and exposes the existing tooltip behavior.
- Motion is limited to the existing screen entrance and small button hover transitions.

## Responsive Behavior

The current flex wrapping remains the primary narrow-width behavior. At small WebView widths:

- tip text may wrap naturally;
- session and command chips may wrap onto additional rows;
- chip maximum widths must prevent horizontal overflow;
- padding and gaps may reduce modestly;
- no element changes order or becomes hidden.

## Data Flow and Error Handling

There are no data-flow changes. `browser-chat.js` continues to create and populate the welcome screen from localized text, recent-thread JSON, command JSON, and the note-count update. Existing JSON parsing fallbacks and empty-section handling remain unchanged.

Because the change is CSS-only, it introduces no new runtime failure paths. Missing optional theme tokens must fall back to existing compatible tokens where necessary.

## Implementation Scope

The implementation is limited to the welcome-screen section of:

- `src/main/resources/com/patres/alina/uidesktop/ui/chat/browser-chat.css`

No production JavaScript, Java, FXML, or localization file should change.

## Verification

- Run the stylesheet contract test to ensure the browser stylesheet remains theme-token based.
- Run the full Gradle test/build suite using the repository's supported JDK.
- Confirm the production diff contains only `browser-chat.css`.
- Inspect the empty-chat state in the application at the normal width and a narrow width.
- Check both the configured light and dark themes for contrast, borders, shadows, hover, focus, truncation, and wrapping.
- Verify recent-session and command clicks still invoke their existing handlers.

## Acceptance Criteria

- The welcome screen retains its existing structure, content order, and functionality.
- The tip uses the approved filled accent treatment.
- Recent sessions use approved soft elevated surfaces.
- Commands use approved quiet accent outlines.
- The result visually belongs to the same family as the floating composer and refreshed message surfaces.
- Light, dark, normal-width, and narrow-width states remain readable and free of horizontal overflow.
