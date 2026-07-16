# Floating Chat Tabs Design

## Goal

Replace the active conversation tab's rectangular input-like treatment with a rounded floating-card treatment that matches the application's dashboard cards and header action groups.

## Product Decisions

- Use the approved **floating card** direction for the active tab.
- Keep tab order, naming, selection, closing, overflow scrolling, and new-tab behavior unchanged.
- Preserve the existing rule that the tab bar is hidden when only one conversation is open.
- Keep inactive tabs visually quiet and reveal their interactive surface on hover.
- Continue showing the close control only while a closable tab is hovered.
- Use existing semantic theme tokens so the component remains compatible with light and dark themes.

## Considered Approaches

### Accent underline

The active state would be indicated by bold text and a short accent line. This is visually light, but the clickable bounds are less obvious and the treatment is less connected to the application's card-based visual language.

### Segmented pill

All tabs would sit inside a shared muted capsule, with the active tab raised on a light inner surface. This provides strong grouping, but adds another enclosing surface to an already dense header.

### Floating card (selected)

The active tab becomes a compact rounded card while inactive tabs remain transparent. It mirrors the dashboard widgets and header controls without wrapping the entire tab row in another container.

## Visual States

### Active

The active tab uses a 13–14 px corner radius, an accent-subtle surface, an accent-muted border, and a restrained theme-derived shadow. Its label uses the accent foreground and semibold or bold weight. The result must read as a compact floating card rather than a rectangular text field.

### Inactive

Inactive tabs have no visible border, shadow, or permanent background. Their labels use the muted foreground. Hovering adds the existing subtle background and promotes the label to the default foreground.

### Close, Add, and Overflow Controls

The close button remains hidden until hover and keeps its compact rounded hover surface. The add and overflow controls retain their existing behavior and dimensions. Their existing 8 px corner radii remain unchanged; these controls do not receive the active card's border or shadow.

## Implementation Boundary

The production change is limited to the conversation-tab rules in `chat-shell.css`, which is the last loaded stylesheet and currently overrides the earlier pill treatment from `index.css`. No Java changes are required because `ChatTabBar` already exposes active and inactive state classes and manages close-button visibility.

The new declarations must use existing theme tokens such as the accent surface, accent border, accent foreground, and default shadow. No literal component colors or new shared theme abstractions are needed.

## Edge Cases

- Long tab names continue to ellipsize within the existing width constraint.
- Multiple tabs continue to overflow into the existing horizontal scrolling behavior.
- Hovering the active tab must not replace its active card surface with the inactive hover surface.
- Dark themes must use semantic token values and avoid a bright border glow.
- Hiding or showing the tab bar as the tab count changes must not affect dashboard or chat layout behavior.

## Verification

### Automated

- Add or update a focused stylesheet contract test for the active floating-card declarations and inactive hover state.
- Run the focused style contract test.
- Run `./gradlew test`.

### Manual UI QA

- Inspect active, inactive, and hovered tabs in a light theme and a dark theme.
- Confirm the active tab reads as a rounded card and not an input field.
- Confirm long labels remain ellipsized and the close button appears only on hover.
- Open enough tabs to exercise both overflow arrows and confirm the selected tab remains visible.
- Add, switch, and close tabs to confirm behavior is unchanged.

## Acceptance Criteria

- The active conversation tab is a rounded floating card aligned with the application's card-based visual language.
- Inactive tabs remain low-emphasis and gain a subtle hover surface.
- Active, hover, close, add, and overflow states are legible in light and dark themes.
- Tab selection, closing, creation, overflow scrolling, and conditional bar visibility remain unchanged.
- The production diff is scoped to tab styling unless verification reveals a concrete defect requiring a minimal additional change.
