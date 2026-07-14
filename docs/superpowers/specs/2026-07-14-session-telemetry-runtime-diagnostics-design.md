# Session telemetry and runtime diagnostics

## Goal

Add a provider-backed operational view for the active agent session. A developer should be able to see reliable turn metrics, context use, and runtime health without opening application logs. The feature remains local to AlIna and does not send analytics or tracking data anywhere.

## Product decisions

- Use the telemetry already owned by Codex and OpenCode. Do not create a local telemetry journal or a second copy of conversation history.
- Display only values explicitly returned by the active runtime. Do not estimate costs, rate limits, token categories, or context usage.
- Omit unavailable values from compact UI. In the diagnostics panel, distinguish unavailable, unsupported, and failed data.
- Put detailed diagnostics in a session-specific right panel. Keep the response footer and context indicator compact.
- Build one provider-neutral UI model. Codex and OpenCode adapters translate their native responses and events into that model.

## Scope

The first release covers:

- a telemetry footer on assistant responses;
- live updates for the current turn;
- telemetry reconstructed from provider history when the provider exposes it;
- a context-use indicator next to the model selector;
- a right-side session and runtime diagnostics panel;
- runtime health, version, transport, process, working directory, MCP, hooks, and provider warnings where supported;
- model rerouting and context-compaction warnings;
- focused unit and integration tests for both runtime adapters and the UI bridge.

The first release does not cover:

- local persistence of telemetry;
- a normalized or raw event inspector;
- diagnostic bundle export or redaction;
- per-tool duration and command-level metrics;
- charts, historical trends, or cross-session aggregation;
- local pricing tables or calculated cost estimates;
- external analytics or telemetry collection.

## Provider capabilities and limitations

The implementation targets the installed Codex `0.144.3` app-server schema and OpenCode `1.17.9` API while remaining tolerant of missing optional fields.

### Codex

During an active turn, `thread/tokenUsage/updated` exposes last-turn and cumulative token breakdowns plus an optional model context window. `model/rerouted` exposes the previous model, replacement model, and reason. Turn and item notifications expose completion, errors, and context-compaction items. Account rate-limit reads and updates may expose reliable rate-limit windows.

Provider history is less complete. `thread/read` returns turns with `startedAt`, `completedAt`, `durationMs`, status, errors, and items. The thread includes `cliVersion` and `modelProvider`, while context-compaction remains visible as a turn item. The current schema does not guarantee detailed token usage or the exact model for each historical turn. Those values must therefore disappear after reopening when Codex cannot return them.

### OpenCode

OpenCode assistant message metadata contains `providerID`, `modelID`, agent, cost, token categories, and created/completed timestamps. These values can be reconstructed from `/session/:id/message` after reopening. Runtime diagnostics use `/global/health` and supported MCP, LSP, formatter, and configuration/status endpoints.

### Capability reporting

Each diagnostic section reports one of four states: available, unavailable, unsupported, or failed. A missing optional provider field is unavailable, not zero. A missing endpoint is unsupported, not an unhealthy runtime. A transport or parse failure is failed and includes a short user-facing reason.

## Domain model

Introduce a shared immutable `TurnTelemetry` model with optional provider-backed values:

- provider turn and message identifiers;
- backend, model, model provider, and agent;
- start time, completion time, and duration;
- input, output, reasoning, cached-read, cached-write, and total tokens;
- model context-window size and derived utilization when both operands exist;
- provider-reported cost;
- turn status, context-compaction marker, and model-reroute details.

The derived context percentage is calculated only from compatible provider values. For Codex it uses `tokenUsage.last.totalTokens / modelContextWindow`. For OpenCode it uses the provider-reported message token total and the matching model context limit returned by provider metadata. If either operand is absent, the percentage is unavailable. Token categories are not blindly summed when a provider already returns an authoritative total, because cache accounting semantics can differ.

Numeric availability is represented explicitly rather than with sentinel zero values. In particular, a provider-reported cost of zero is different from the current Codex adapter's hardcoded `0.0`, which means no cost data was supplied.

`ChatMessageResponseModel` gains a provider message identifier and optional `TurnTelemetry`. Existing consumers continue to render messages without telemetry. For Codex history, telemetry from a turn is attached to the final assistant message in that turn. OpenCode telemetry is attached directly from the assistant message metadata.

Introduce a `SessionDiagnostics` response containing:

- the current session telemetry summary;
- the existing `AgentRuntimeStatus` data;
- component diagnostics for MCP and hooks, plus additional runtime components supported by the provider;
- account rate limits when returned by the provider;
- structured warnings with source, severity, code, and concise message;
- the time at which diagnostics were fetched.

This response is transient and is never written to AlIna storage.

## Runtime adapters

### Codex adapter

The active stream keeps the latest provider telemetry snapshot for the turn. Token-usage notifications replace the matching snapshot rather than incrementing counters. Reroute and compaction events update structured flags. Turn completion publishes the final snapshot through the existing streaming boundary.

History mapping changes from a flat item scan to turn-aware mapping so duration, status, compaction, and errors can be associated with the correct final assistant message. It does not synthesize missing historical tokens or model identifiers.

The diagnostics query reuses the running app-server client and requests independent sections for runtime/account status, MCP server status, hooks, and rate limits. Failure in one request does not discard successful sections.

### OpenCode adapter

The stream maps the full assistant `info.tokens` object instead of reducing it to one total. It records provider-reported cost, model, agent, and message timestamps. The history mapper uses the same mapping logic so live and reopened messages have equivalent telemetry when OpenCode returns the same fields.

Diagnostics query health and supported component endpoints independently. Endpoint absence is mapped to unsupported. Connection and schema failures are mapped to failed without making the entire panel unavailable.

## Data flow

### Live turn

1. The UI starts a normal agent turn.
2. The runtime creates an in-memory telemetry snapshot for the provider turn.
3. Native usage, reroute, compaction, and completion events update that snapshot.
4. Telemetry updates are published for the matching AlIna chat thread.
5. The context indicator and open diagnostics panel refresh in place.
6. Completion includes the final `TurnTelemetry`; the assistant footer is finalized with available fields.

### Reopened history

1. AlIna requests messages from the active runtime as it does today.
2. The runtime reads provider-owned message and turn history.
3. The adapter builds `ChatMessageResponseModel` instances with whatever historical telemetry the provider returns.
4. The browser renders the same footer component used for live completion.
5. Metrics absent from provider history are omitted.

### Diagnostics panel

1. The user clicks the context/model control.
2. The right panel opens immediately with the last known session values and a loading state for runtime sections.
3. A virtual thread fetches diagnostic sections independently with bounded timeouts.
4. Results are applied only if the chat tab and refresh generation still match.
5. Manual refresh repeats the query without blocking chat input or streaming.

## User interface

### Response footer

Assistant responses show a subtle metadata line such as `gpt-5.4 · 18.4 s · 12.8k tokens`. Cost is appended only when the provider supplies an authoritative cost field. Missing elements are omitted; zero is displayed only when zero is a meaningful provider value.

The footer is rendered by one browser-chat function for both live and historical messages. It remains visually secondary to answer content and does not create a separate card.

### Context control

The current model label becomes a clickable control with a compact context ring. The ring uses neutral styling below 70 percent, warning styling from 70 through 89 percent, and critical styling from 90 percent. Unknown utilization renders as an empty ring without a percentage. A tooltip states the exact used and available token values when known.

### Right diagnostics panel

The panel belongs to the active `ChatWindow`, opens to approximately 360 pixels, and can be closed without losing fetched state. It contains unframed, dense sections:

- Session: backend, model, agent, context, last-turn duration, and status.
- Tokens: input, output, reasoning, cache read/write, and authoritative total.
- Runtime: health, version, transport, process, base URL, and working directory.
- Extensions: summarized MCP and hook status with problematic entries called out.
- Limits: provider rate-limit windows and reset time when available.
- Warnings: rerouting, compaction, unhealthy components, schema failures, and unavailable provider data that materially affects interpretation.

The header provides refresh and close icon buttons with tooltips plus the last refresh timestamp. During streaming, session values update without reopening the panel. Switching chat tabs displays the selected session's panel state rather than retaining data from the previous tab.

No raw JSON, charts, instructional copy, or diagnostic export appears in this release.

## Error handling and concurrency

- Diagnostics run off the JavaFX thread.
- Each provider request has a bounded timeout and produces an independent section result.
- Refreshes carry a generation identifier. Late responses from an older refresh or previously active tab are discarded.
- Unknown fields are ignored. Missing required structures produce a failed section and a log entry instead of breaking message rendering.
- Live telemetry events are routed by provider thread/turn identifiers and ignored when they do not match the active stream.
- Unavailable data never becomes numeric zero by default.
- The panel exposes concise errors; detailed exceptions and provider payload context remain in application logs.
- Existing send, cancel, interaction approval, and history flows continue to work when telemetry endpoints fail.

## Testing

Add focused tests for:

- Codex live token usage, context window, rerouting, compaction, and completion mapping;
- Codex turn-aware history mapping with partial telemetry;
- OpenCode live and historical mapping of model, agent, times, token categories, and cost;
- authoritative totals and context-utilization calculation;
- omission of unavailable values and correct handling of meaningful zero values;
- runtime diagnostics with complete, partial, unsupported, timed-out, and malformed responses;
- independent failure of MCP, hooks, limits, and health sections;
- refresh generation protection during tab switches;
- response footer formatting for complete and partial telemetry;
- context indicator thresholds and unknown state.

Run `./gradlew build`, `node --check` for browser JavaScript, XML validation for changed FXML, and a JavaFX smoke test covering live completion, history reopen, panel refresh, and tab switching.
