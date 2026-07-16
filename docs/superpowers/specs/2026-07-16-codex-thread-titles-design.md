# Codex thread titles

## Goal

Show the user-facing title owned by Codex instead of a UUID in AlIna's conversation history and chat tabs. While Codex has not assigned a title, show a short preview of the first user message. Keep manual rename behavior and use the UUID only as the final fallback.

## Findings

Codex App Server exposes three relevant thread fields:

- `name`: an optional user-facing title, including titles assigned by Codex or set by a client;
- `preview`: usually the first user message;
- `id`: the UUIDv7 thread identifier.

Observed `thread/list` responses contain both named threads and threads whose `name` is still `null`. The current Codex mapper reads `name`, then the unsupported `title` field, then `id`, so unnamed threads display as UUIDs even though `preview` is available.

Codex App Server 0.144.5 sends live title changes as `thread/name/updated` with the value in `params.threadName`. AlIna currently reads `params.name` or `params.thread.name`, so it misses the current notification shape.

The UI subscriber for `ChatThreadTitleUpdatedEvent` also writes the received title back through `BackendApi.renameChatThread`. Runtime rename operations publish the same event after a successful write, so this write-back is unnecessary and can recursively trigger more rename operations.

## Product decisions

- Prefer Codex's non-blank `name` without modifying or regenerating it.
- When `name` is absent, derive a display-only fallback from `preview`.
- Normalize preview whitespace to a single line and limit the fallback to 80 Unicode code points, appending an ellipsis when truncated.
- Use `id` only when both `name` and the normalized `preview` are blank.
- Do not persist the preview fallback as a Codex name. A later Codex-generated title must remain distinguishable and replace it naturally.
- Preserve manual rename through the existing `thread/name/set` request.
- Treat title-update events as notifications to the UI, not commands to write the title back.

## Considered approaches

### Codex name with preview fallback (selected)

Use `name`, then a compact `preview`, then `id`. This displays Codex's own title whenever it exists, updates naturally when Codex assigns one later, and eliminates UUID-heavy history without another model request.

### Codex name with UUID fallback

Use only `name` and `id`. This is strictly provider-owned, but current threads whose `name` is still `null` remain unreadable until another Codex surface assigns a title.

### Generate titles in AlIna

Ask a model to summarize each thread and write the result through `thread/name/set`. This would provide titles eagerly but duplicates Codex behavior, adds latency and model usage, and creates competing title ownership.

## Runtime mapping

Keep the title resolution at the Codex runtime boundary, where provider JSON is converted to the shared `ChatThread` model. The resolved display name follows this exact order:

1. non-blank `thread.name`, unchanged;
2. normalized and shortened `thread.preview`;
3. `thread.id`.

The shared `ChatThread` contract and JavaFX history card remain unchanged. Other backends keep their existing naming behavior.

## Live update flow

For `thread/name/updated`, resolve the Codex thread ID through the existing Codex-to-AlIna mapping and read the title in this order:

1. `params.threadName`, matching the current protocol;
2. `params.name`, for compatibility with the previously assumed shape;
3. `params.thread.name`, for compatibility with nested payloads.

Publish `ChatThreadTitleUpdatedEvent` only for a mapped thread and a non-blank title.

`ApplicationWindow` handles the event on the JavaFX application thread and updates the matching chat tab. It does not call `BackendApi.renameChatThread`. Conversation history continues to load its source of truth through `thread/list`; reopening or reloading the history therefore shows the persisted Codex name.

Manual rename remains a command path: the history card sends the request, the runtime calls `thread/name/set`, and publishes one immediate `ChatThreadTitleUpdatedEvent` only after that request succeeds. A later App Server notification for the same title is harmless because UI title assignment is idempotent.

## Error handling

- A notification with no non-blank value in any supported title field is ignored.
- An update for an unknown Codex thread is ignored because it cannot be routed safely to an AlIna tab.
- A failed manual `thread/name/set` request remains logged and must not publish a success title update.
- Malformed or absent `preview` values fall through to the UUID rather than failing the entire history response.
- Preview normalization must not split a Unicode surrogate pair when applying the 80-code-point limit.

## Testing

- Add focused Codex runtime tests with a mocked App Server client for `name`, `preview`, blank values, whitespace normalization, truncation, Unicode, and UUID fallback.
- Add a notification test proving that `thread/name/updated.params.threadName` publishes the mapped `ChatThreadTitleUpdatedEvent`.
- Keep compatibility coverage for the older `name` and nested `thread.name` notification shapes.
- Add or update an `ApplicationWindow` contract test proving that the title event updates UI without calling the backend rename path.
- Run the focused Codex and UI contract tests, then run `./gradlew test`.

## Acceptance criteria

- A Codex thread with `name` displays that exact name in history and tabs.
- A thread without `name` displays a compact first-message preview instead of a UUID when a preview exists.
- A later Codex title notification replaces the preview in the corresponding open tab without a write-back loop.
- Manual rename continues to persist through Codex and update visible UI.
- Threads lacking both a name and preview still display their UUID.
- OpenCode thread naming behavior is unchanged.

## Out of scope

- Generating a second title in AlIna.
- Backfilling or renaming existing Codex threads from their previews.
- Changing Codex's title-generation timing or prompts.
- Redesigning the history cards or chat-tab layout.
- Changing conversation persistence or thread identifiers.
