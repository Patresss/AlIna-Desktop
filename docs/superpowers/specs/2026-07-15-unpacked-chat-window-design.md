# Unpacked Chat Window Design

## Summary

Rebuild the chat workspace so the conversation belongs directly to the application canvas instead of appearing inside an additional card. Keep one floating composer at the bottom as the only elevated surface. Remove the nested focused-input appearance, put the send action first, and delete the persistent keyboard-shortcut hint.

The approved direction is **C. Unpacked conversation with a floating composer**.

## Goals

- Remove the visual card around the complete message area.
- Make the conversation and welcome state feel native to the main workspace.
- Present the composer as one coherent floating surface without a nested input border or focused fill.
- Put the primary send action first in the vertical action rail.
- Keep command state and model selection inside the composer.
- Remove the permanent `Enter sends · Shift+Enter adds a line` hint completely.
- Preserve chat behavior, message rendering, attachments, commands, streaming, and split mode.

## Non-goals

- No backend, API, persistence, message, command, or model-selection changes.
- No redesign of WebView message content or welcome-screen content.
- No change to keyboard behavior: Enter still sends and Shift+Enter still inserts a line.
- No changes to chat tabs, dashboard layout, or header actions.
- No new settings or user-selectable composer variants.

## Approved Visual Direction

### Conversation canvas

`chatContentPane`, `ChatWindow`, and `chatAnswersPane` no longer form a rounded, bordered, shadowed card. Their backgrounds are transparent so the WebView conversation or empty-chat welcome state reads as part of the application workspace. The conversation continues to consume all space not used by the composer.

This treatment applies in both the normal vertical workspace and split mode. Removing the chrome must not change the existing sizing or split-layout ownership in `ApplicationWindow`.

### Floating composer

The bottom area uses a transparent dock with a small outer inset. Inside it, a single rounded `VBox` owns the visible composer background and subtle elevation. The one surface contains, in order:

1. pending image attachments when present;
2. the text input and vertical action rail;
3. the status row with command state on the left and model selection on the right.

The composer uses a theme-provided overlay color and subtle shadow. It does not use a visible outline. The TextArea, its internal ScrollPane, viewport, and content layer remain transparent in normal and focused states. Focusing the input must not create another white or grey rounded rectangle.

The composer retains a compact default height. Longer text remains usable through the TextArea's existing wrapping and scrolling behavior; this redesign does not introduce a new autosizing algorithm.

### Action order

The vertical action rail is ordered by importance:

1. send;
2. attach image;
3. stop or regenerate, according to stream state;
4. clear chat.

Send remains the only accent-filled control. Secondary actions stay visually quiet and retain their existing tooltips, accessible text, handlers, enabled states, and streaming behavior.

### Status row

The status row lives inside the floating composer. It contains only:

- the selected-command status on the leading side;
- the current model selector on the trailing side.

The static shortcut label is removed from `chat-window.fxml`. The unused `chat.composer.shortcut` key is removed from both `Bundle_pl.properties` and `Bundle_en.properties`. Keyboard behavior itself remains unchanged.

## Component and File Responsibilities

- `ApplicationWindow.java`: stop attaching the visual card style to `chatContentPane`; keep all layout and split-mode behavior intact.
- `chat-window.fxml`: add a transparent bottom dock around one composer surface, keep attachments and status within that surface, reorder buttons, and remove the shortcut label.
- `chat-shell.css`: make the conversation canvas transparent, style the bottom dock and single floating surface, and keep every TextArea layer transparent across focus states.
- `Bundle_pl.properties` and `Bundle_en.properties`: remove the obsolete shortcut translation key.
- `ChatWindow.java`: preserve handlers and state flow; adjust layout-only bindings only if the new FXML hierarchy requires it.

No new production class or abstraction is needed.

## Data Flow and Behavior

Message and UI state flow remains unchanged:

- `Browser` continues to own rendered messages and the welcome state.
- `ChatStreamingController` continues to own send, stop, and regenerate state.
- `ChatWindow` continues to own attachments, command selection, model selection, keyboard input, and clear-chat behavior.
- `ApplicationWindow` continues to own tabs, active chat windows, normal mode, and split mode.

The redesign changes node hierarchy and styling only. It does not introduce new events, service calls, thread transitions, or persistence.

## Error and Edge States

- Empty, populated, streaming, stopped, and failed conversations use the same composer placement.
- Pending attachments remain visible within the single composer surface and keep their existing removal behavior.
- The stream-control button retains its current icon, tooltip, and visibility transitions without moving the send action from the first position.
- A long command label or model name must shrink or clip without pushing the action rail outside the composer.
- Narrow and split layouts must keep the composer within the available chat width without horizontal scrolling.
- Light, dark, Atlantafx, and user themes continue to supply semantic colors; no literal palette values are added to component CSS.

## Verification

### Automated

- Run `./gradlew test`.
- Run `./gradlew build`.
- Confirm FXML loads without missing controller fields, resource keys, or handler methods.
- Search production resources to confirm `chat.composer.shortcut` no longer exists.

### Manual UI QA

Check:

- empty and populated conversations;
- TextArea unfocused and focused states;
- send, attach, stop, regenerate, and clear actions;
- pending image previews;
- command selected and unselected states;
- short and long model labels;
- normal mode and split mode at wide and narrow widths;
- one light and one dark theme;
- Enter and Shift+Enter behavior.

## Success Criteria

- The message area no longer reads as a separate card inside the workspace.
- The composer is the only elevated chat surface.
- No nested border or fill appears inside the composer when the TextArea receives focus.
- Send is the first action in the vertical rail.
- The shortcut hint and its translation key are absent.
- Existing chat behavior and split-layout behavior remain functional.
