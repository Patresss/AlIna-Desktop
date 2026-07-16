# Primary join action for the upcoming meeting card

## Goal

Make “Join” the visually primary action in the upcoming meeting card and make “Prepare me” secondary. Joining the imminent meeting is the direct, time-sensitive action and should carry the stronger visual emphasis.

## Product decisions

- Keep the existing action order: “Prepare me” followed by “Join”.
- Keep both icons, labels, click behavior, accessibility text, spacing, and availability rules unchanged.
- Move the current bordered accent treatment from “Prepare me” to “Join”.
- Move the current muted flat treatment from “Join” to “Prepare me”.
- Leave the attachments toggle on its current secondary treatment.
- Do not introduce new shared primary/secondary button abstractions for this isolated card-level change.

## Considered approaches

### Swap the existing component styles (selected)

Reassign the existing CSS declarations between the two semantic selectors. This produces the requested hierarchy with the smallest behavioral risk and preserves the card layout.

### Swap styles and button order

Moving “Join” to the first position would add structural emphasis, but it changes scanning order and was not requested. The stronger style is sufficient.

### Introduce generic action hierarchy classes

Generic primary and secondary classes could be reused later, but no current requirement needs that abstraction. Adding it now would broaden the change unnecessarily.

## Styling

`.workspace-upcoming-event-join` receives the current primary treatment: transparent background, default border, rounded corners, accent foreground, 11px semibold text, 4px by 8px padding, accent-subtle hover background, accent-muted hover border, and accent icon.

`.workspace-upcoming-event-prepare` receives the current secondary treatment: muted foreground, 10px semibold text, 4px by 7px padding, subtle hover background, default foreground on hover, and muted icon.

`.workspace-upcoming-event-attachments-toggle` remains grouped with the secondary visual treatment. Disabled “Prepare me” behavior remains owned by JavaFX and the existing missing-prompt tooltip.

## Testing

Update the upcoming-event style contract test so it verifies that the primary declarations belong to the join selector and that prepare remains grouped with the secondary declarations. Run the focused style contract test and the full Gradle test suite.

## Out of scope

- Changing button order or action behavior.
- Changing labels or localization.
- Changing the meeting-link resolver.
- Redesigning other dashboard actions.
