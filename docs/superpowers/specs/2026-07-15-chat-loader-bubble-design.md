# Chat Loader Bubble Design

## Context

The chat loading indicator currently appears as three accent dots inside an oversized, nearly square assistant-message surface. It can also reappear with visible dots that do not animate.

Two implementation details cause these problems:

- the loader element carries `chat-message assistant loader`, so the more specific `.chat-message.assistant` rules override parts of the less specific `.loader` presentation;
- dot animation is attached while the loader is hidden, and `showLoader()` only adds `active`, allowing JavaFX WebView to expose a paused or stale animation phase after repeated hide/show cycles.

The approved direction is **A. Wave in a compact assistant bubble**.

## Goals

- Present the loader as a small message bubble that belongs to the assistant-message family.
- Keep the indicator visual-only, with no loading label.
- Make the three-dot wave visibly animated every time the loader is shown.
- Preserve assistant and user alignment, automatic scrolling, and existing Java-to-JavaScript calls.
- Use only existing semantic theme tokens and remain readable in light and dark themes.

## Non-goals

- No changes to message streaming, agent activity, backend state, or timing.
- No loading text, elapsed-time display, cancellation action, or progress percentage.
- No replacement of the existing loader DOM structure.
- No changes to ordinary message, activity, reasoning, commentary, todo, or composer presentation.
- No new animation library or Java-side animation control.

## Approved Visual Direction

### Compact assistant bubble

The loader remains a chat entry but receives an explicit `.chat-message.loader` treatment so ordinary assistant-message sizing cannot override it. The bubble is content-sized, horizontally compact, and visibly wider than it is tall. It uses:

- the overlay background with the default background as fallback;
- a subtle theme border;
- the same rounded family as assistant messages at a smaller scale;
- a restrained theme-derived shadow;
- compact horizontal padding and no message-action space.

The target visual size is approximately 38–44 px of inner width and 26–30 px of total height. The bubble remains aligned to the leading edge for assistant loading and to the trailing edge when `user-message` is present.

### Three-dot wave

The bubble contains three 5 px accent dots with a 5 px gap. Each dot follows the same short wave animation with a staggered delay:

- resting dots use reduced scale and opacity;
- the active dot rises approximately 4 px, reaches full opacity, and returns to rest;
- the full cycle lasts approximately 900 ms;
- the second and third dots start approximately 120 ms and 240 ms after the first.

The animation uses the semantic accent foreground token. It does not translate or resize the outer bubble, so surrounding conversation content remains stable.

## Animation Lifecycle

Animation is declared only for `.loader.active` dots. Hidden loader dots have their base visual state but no running animation.

Each call to `showLoader()` performs this lifecycle:

1. remove `active` from the loader;
2. force one layout read to commit the inactive state;
3. add `active` again;
4. clear `user-message` for the ordinary assistant state;
5. preserve the existing scroll-to-bottom behavior.

Removing and re-adding `active` within the same JavaScript turn restarts the CSS keyframes without a visible flicker. `showLoaderForUserMessage()` continues to call `showLoader()` and then adds `user-message`. `hideLoader()` continues to remove both state classes.

This lifecycle handles first display, repeated display, a redundant show call while already visible, and switching between assistant and user alignment.

## Components and Implementation Boundary

The change is limited to:

- `browser-chat.css` for explicit loader sizing, surface styling, dot sizing, the active-only wave, and stagger delays;
- `browser-chat.js` for deterministic animation restart in `showLoader()`.

`browser-chat.html` retains the existing loader element and its three child dots. Java controllers, WebView bridge methods, language resources, backend services, and models remain unchanged.

## Error and Edge States

- Repeated `showLoader()` calls restart rather than duplicate animation.
- Calling `hideLoader()` when already hidden remains harmless.
- Switching between assistant and user loaders preserves the correct margin alignment.
- Theme changes update the surface, border, shadow, and dot colors through existing tokens.
- The compact bubble does not inherit ordinary message action padding or maximum-width behavior.
- WebView animation throttling while the application is unfocused may delay frames, but refocusing or showing the loader restarts the active keyframes deterministically.

## Verification

### Automated

- Run `ApplicationStylesheetContractTest` to confirm the loader uses no literal component palette.
- Run `./gradlew test` with the repository's supported JDK.
- Run `./gradlew build` after tests pass.
- Confirm production changes are limited to `browser-chat.css` and `browser-chat.js`.

### Manual UI QA

- Show and hide the assistant loader repeatedly for at least ten cycles.
- Call show again while the loader is already active and confirm the wave restarts.
- Switch between assistant and user loader alignment.
- Leave the application unfocused, return to it, and show the loader again.
- Inspect the bubble in one light and one dark theme.
- Confirm nearby messages do not shift while dots animate.
- Confirm scrolling still keeps the active loader reachable.

## Acceptance Criteria

- The loader appears as a compact assistant-style message bubble rather than a large square card.
- Only the dots animate; the bubble remains stable.
- Every show cycle starts a visible three-dot wave.
- Loader alignment and scrolling behavior remain unchanged.
- Light and dark themes use semantic tokens without literal component colors.
- No unrelated chat or backend behavior changes.
