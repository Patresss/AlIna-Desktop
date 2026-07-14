# Agent interactions in chat

## Goal

Support Codex and OpenCode requests that pause an agent turn and require a user decision or structured input. Keep the interaction attached to the chat history and preserve the running turn until the runtime confirms completion or cancellation.

## Scope

The change covers:

- Codex command, file-change, and additional-permission approvals;
- Codex `item/tool/requestUserInput` questions;
- Codex `mcpServer/elicitation/request` form and URL modes;
- existing OpenCode approvals;
- retryable resolution failures and unroutable Codex requests;
- accurate session versus persistent approval wording;
- streaming UI state while an interaction is pending.

The change does not introduce a general-purpose JSON Schema renderer, persist interaction cards as conversation messages, or add new OpenCode interaction types that the current OpenCode event stream does not expose.

## Architecture

Introduce a shared agent-interaction envelope instead of representing every server request as a permission. It identifies the request, thread, source runtime, interaction kind, title, message, approval scope, and a structured payload used by the renderer.

Supported interaction kinds are:

- `APPROVAL` for allow/deny decisions;
- `USER_INPUT` for one to three Codex questions and their options;
- `MCP_FORM` for MCP primitive form fields;
- `MCP_URL` for an external MCP authorization or data-entry URL.

The existing permission controller remains the routing boundary, but accepts an interaction response containing an action and structured values. Each runtime owns protocol-specific conversion. Codex converts the normalized response to its JSON-RPC response. OpenCode maps approval actions to its HTTP permission endpoint.

The browser receives serialized interaction data through the existing Java-to-JavaScript bridge. Rendering stays in `browser-chat.js`; protocol parsing stays in the server runtime.

## Components

### Shared models

Add an interaction request model and response model under `com.patres.alina.common`. The request contains only UI-relevant data and does not expose the complete Codex JSON-RPC message. The response supports:

- approval actions: once, scoped approval, deny;
- input submission with a map of field IDs to typed JSON values;
- decline or cancel for MCP elicitation.

Resolution distinguishes successful resolution, missing request, and retryable error. Approval scope is explicit: Codex scoped approval is session-only, while OpenCode `always` remains persistent.

### Codex adapter

`CodexAgentRuntime` parses server requests into the shared model and retains the original parameters in its pending-request map. It builds these responses:

- approval decisions accepted by the corresponding Codex request;
- `{ "answers": { questionId: { "answers": [...] } } }` for user input;
- `{ "action": "accept", "content": {...} }` or decline/cancel for MCP elicitation.

Pending entries are removed only after `client.respond` succeeds. If a request cannot be associated with an active chat thread, the runtime sends a JSON-RPC error immediately.

### OpenCode adapter

OpenCode continues to emit approval interactions. Its pending entry is removed only after the permission HTTP request succeeds. Its persistent `always` behavior and wording remain distinct from Codex session approval.

### Chat renderer

Each interaction is an inline chat block:

- approvals show allow once, allow for the advertised scope, and deny;
- user-input questions show radio options and an optional free-text answer; secret text uses a password input;
- MCP forms render strings, numbers or integers, booleans, single-select enums, and multi-select enums, including required and basic min/max constraints;
- MCP URL requests show the message, an open-link command, confirm, decline, and cancel actions.

Submitting disables only that block while the response is in flight. A retryable failure shows the error and re-enables the controls without discarding entered values. A resolved card remains in the history with its final status and disabled controls.

## Data flow

1. Codex or OpenCode produces a request that requires user interaction.
2. The runtime registers the pending request and publishes a normalized interaction event for the chat thread.
3. The chat controller keeps the turn active and asks the browser to render the corresponding block.
4. The browser validates and serializes the user's action and values through its bridge.
5. The controller resolves the request on a virtual thread.
6. The owning runtime sends the protocol-specific response.
7. On success, the runtime removes the pending request and the UI marks the block resolved. The agent continues the same turn.
8. On transport failure, the runtime retains the request and the UI restores the block for retry.

## Streaming state

Receiving an interaction does not call the normal end-of-stream transition. The composer remains read-only and the send action remains disabled so status text cannot be submitted as a prompt. The stream control stays in Stop mode.

After a successful response, the interaction block is resolved and the UI returns to the ordinary running status. Only completion, cancellation, terminal error, or a confirmed denial ends the streaming UI state.

Multiple requests can be displayed, but each request is resolved independently by ID. The turn remains active while the runtime owns it.

## Error handling

- Missing request: mark the block stale and do not retry automatically.
- Runtime or transport error: keep the request registered, show the error, and allow another submission.
- Invalid form values: reject in the browser before calling Java and retain the values.
- Unsupported MCP field schema: show an explicit unsupported-field message and allow cancel or decline; do not submit invented content.
- Unroutable Codex request: reply with a JSON-RPC invalid-request error so Codex does not wait indefinitely.
- Turn cancellation: clear pending requests associated with that stream and let the runtime interruption path finish the turn.

## Testing

Add focused unit tests for:

- Codex request classification and normalized payloads;
- Codex user-input and MCP response construction;
- session versus persistent approval metadata;
- retaining Codex and OpenCode pending requests after response failure;
- removing pending requests after success;
- rejecting or replying to an unroutable Codex request;
- JavaScript form serialization and validation through an extracted, deterministic renderer helper where practical;
- controller state transitions for pending, successful, denied, and failed interactions without starting a second agent turn.

Run `./gradlew test` and use the JavaFX application for a manual smoke test of approval, user-input, retry, and Stop behavior.
