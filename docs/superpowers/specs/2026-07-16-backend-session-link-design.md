# Backend session link

## Goal

Make the existing header action open the active provider session for both supported chat backends. OpenCode continues to open its web UI, while Codex opens the matching local task in Codex Desktop. The action must be disabled when the active AlIna chat does not yet have a provider session.

## Product decisions

- Keep one backend-neutral header action instead of hiding it for Codex.
- OpenCode uses its existing HTTP session URL.
- Codex uses the supported `codex://threads/{threadId}` deep link.
- Disable the action while there is no active chat, while availability is being resolved, and before the first provider session has been created.
- Resolve availability asynchronously so the JavaFX thread is never blocked by OpenCode HTTP calls or Codex app-server calls.
- Do not add a terminal-based `codex resume` fallback. Opening a terminal would be surprising and is not equivalent to opening the graphical provider session.

## Considered approaches

### Backend-specific external URI (selected)

Each `AgentRuntime` returns an external URI for an existing session or no value when a session is unavailable. This preserves the provider boundary and lets the UI remain backend-neutral. It also supports Codex Desktop without introducing backend checks into JavaFX code.

### Hide the action for Codex

This matches the current implementation but discards a supported Codex deep-link capability and makes the header change shape when settings change.

### Always enable the action

This is the smallest code change, but an empty chat or an unresolved provider mapping would still produce a silent no-op. That is the current confusing behavior and is intentionally removed.

## Runtime contract

Rename the provider-facing operation from a web-specific name to an external-URI name. The contract returns a URI string when the requested AlIna thread maps to an existing provider session and returns `null` otherwise.

OpenCode retains the current lookup: resolve the OpenCode session mapping, fetch its directory, and build the HTTP web UI URL.

Codex resolves the AlIna thread ID through `chatThreadToCodexThread`. A Codex thread is considered openable only when the runtime has observed it through `thread/start`, `thread/started`, `thread/read`, or `thread/list`. This distinguishes a newly created AlIna placeholder thread from a real Codex thread without relying on a particular Codex ID format. The returned value is `codex://threads/{encodedCodexThreadId}`.

## UI behavior and data flow

The header button receives an `fx:id`, starts disabled, and exposes a small method for updating its availability. Its action and tooltip use backend-neutral names.

`ApplicationWindow` owns the resolved URI for the active tab:

1. A new tab, tab switch, loaded history thread, or backend setting change invalidates the cached URI and disables the action.
2. A virtual thread asks the active runtime for the session URI.
3. The result is applied on the JavaFX thread only if the active tab and refresh generation still match.
4. The first stream event for an unavailable active session triggers another resolution. The provider mapping exists before that event is published, so the action becomes available during the first turn rather than waiting for the entire response.
5. Completion, cancellation, and error events also refresh availability as a final consistency check.
6. Clicking the enabled action opens the cached URI externally; it does not perform network or app-server work on the JavaFX thread.

Rapid tab changes cannot apply a stale URI because each refresh carries a generation and the expected thread ID.

## External URI opening

Add a backend-neutral external URI opener and keep the existing web-opening method as a compatibility delegate for current HTTP callers. Java Desktop dispatches the `codex` scheme to the registered Codex Desktop application. If the operating system has no handler or rejects the URI, the existing error logging records the failure; this change does not add a new notification component.

## Naming and localization

- Rename `openCurrentOpenCodeSession` and `openOpenCodeSession` to agent/backend-neutral equivalents.
- Rename `getAgentSessionWebUrl` and runtime `getSessionWebUrl` methods to external-URI equivalents.
- Replace the OpenCode-specific resource key with a backend-neutral key.
- Keep the visible meaning “Open backend session”, correcting Polish diacritics to “Otwórz sesję backendu”.

## Testing

- Add Codex runtime tests proving that a new AlIna placeholder thread has no URI and an observed Codex thread produces the expected deep link.
- Keep OpenCode behavior covered through the renamed contract and existing URL-building behavior.
- Add or update a lightweight FXML/resource contract test for the backend-neutral button ID, handler, and tooltip key.
- Run the focused tests and the complete `./gradlew test` suite.

## Out of scope

- Installing Codex Desktop.
- Detecting or repairing operating-system URI-handler registration.
- Launching a terminal as a fallback.
- Changing session persistence or conversation history ownership.
- Altering the header layout or icon.
