# Message Surface Family Design

## Summary

Redesign every top-level conversation entry so sent messages, assistant responses, tool activity, reasoning, commentary, permissions, semantic notifications, and todo state form one calm visual family. The approved direction is **A. Surface family**, derived from the floating composer: soft theme-provided surfaces, restrained elevation, rounded geometry, and hierarchy expressed through width, alignment, and semantic tint rather than repeated colored rails.

## Goals

- Make the full conversation visually consistent with the new floating composer.
- Distinguish user messages, assistant responses, agent process, and interactive states without making every entry an identical card.
- Reduce the full-width stack of grey panels and colored left borders visible in long conversations.
- Preserve readable Markdown, code, images, actions, streaming, todo state, and interactive agent prompts.
- Keep tool calls, reasoning, and commentary compact until the user expands them.
- Preserve theme compatibility by using only existing semantic color tokens.

## Non-goals

- No changes to message models, persistence, WebView DOM creation, Java-to-JavaScript calls, or backend behavior.
- No grouping algorithm for consecutive messages.
- No avatars, timestamps, or persistent `You`/`AlIna` labels.
- No new message actions or changes to copy behavior.
- No rewrite of Markdown, code-block, callout, table, or image rendering.
- No new animation beyond the existing brief entry transition.

## Approved Visual System

### Shared surface language

Top-level conversation entries use 16 px rounded geometry and theme-provided backgrounds. Ordinary message cards have restrained elevation and no decorative left border. The existing slide-in animation remains brief. Hover may increase elevation slightly, but it must not move surrounding content.

Role is communicated without text labels:

- user messages align to the trailing edge, use a soft accent background, and occupy at most 78% of the conversation width;
- ordinary assistant messages align to the leading edge, use the overlay surface associated with the composer, and occupy at most 94% of the width;
- process entries use compact, low-elevation strips;
- interactive and semantic entries use the same geometry with a restrained state tint.

### User messages

`.chat-message.user` becomes a compact trailing card with width determined by its content up to a 78% maximum. It uses `--color-accent-subtle`, carries no colored rail, and uses less elevation than assistant content. Long text wraps naturally without horizontal scrolling.

User image galleries remain inside the same card above the text. Thumbnail expansion continues to use the existing behavior. A message containing images but no text remains a valid compact card.

### Assistant messages

`.chat-message.assistant` becomes a broad reading surface with `var(--color-bg-overlay, var(--color-bg-default))`, matching the composer when the active theme exposes the overlay token and falling back safely to the established WebView background token. It has no role rail or gradient. Markdown structure provides internal hierarchy; headings, callouts, code blocks, lists, tables, and inline code retain their specialized styles.

Short assistant messages do not need to fill the complete row, but long document-like responses may use up to 94% of available width.

### Tool activity and agent process

`.chat-message.activity-message`, `.chat-message.reasoning-message`, and `.chat-message.commentary-message` become compact strips capped below the ordinary assistant width. Their top-level shadow is removed. They use a subtle or tinted background and rounded geometry consistent with the composer. Existing summary, count, chevron, expanded body, stale/active states, and details behavior remain unchanged.

Nested `.assistant-process` content follows the same compact treatment. Expanding a process entry reveals its current body without changing event flow or reconstructing the DOM.

### Permissions and semantic states

`.chat-message.permission-message` remains visually prominent enough to signal required action. It uses the ordinary surface geometry and subtle elevation while preserving its badge, body, buttons, disabled state, resolution status, and error presentation.

`.accent`, `.info`, `.warning`, `.success`, and `.danger` retain semantic meaning through the corresponding existing `--color-*-subtle`, `--color-*-muted`, and `--color-*-fg` theme tokens. They no longer reintroduce the generic colored left rail. The state must remain recognizable in light and dark themes without literal palette values.

### Todo state

The streaming todo panel and finalized todo disclosure are part of the same family even though they are not ordinary `.chat-message` nodes. Their existing sticky/finalized behavior, progress, item states, priorities, and expansion remain unchanged. Their outer background, radius, elevation, and border treatment align with the new surfaces.

### Actions and focus

Message actions remain hidden by default and appear on pointer hover. They must also appear when the message contains keyboard focus through `:focus-within`. Existing copy buttons, copied feedback, accessible labels, and event handling remain unchanged.

Focus visibility on interactive controls is preserved. The redesign does not hide outlines on permission buttons, disclosure toggles, code-copy controls, or image interactions.

## Implementation Boundary

The redesign is CSS-only and belongs in `browser-chat.css`. Existing DOM classes already provide the necessary distinctions:

- `user` and `assistant` for ordinary roles;
- `activity-message`, `reasoning-message`, and `commentary-message` for agent process;
- `permission-message` for interactive approval;
- `accent`, `info`, `warning`, `success`, and `danger` for semantic state;
- `loader`, `todo-sticky`, and `todo-finalized` for streaming support.

No Java, JavaScript, HTML-template, FXML, language-resource, or model changes are required. Styles should refine the existing selectors in place rather than append a second competing override section.

## Data Flow and Behavior

Message flow remains unchanged:

1. `ChatWindow` passes message content, role, style, and optional images to `Browser`.
2. `Browser` calls the existing JavaScript functions.
3. `browser-chat.js` creates the same DOM and classes, enhances code blocks, attaches message actions, and manages stream/process state.
4. `browser-chat.css` alone determines the revised presentation.

No new state, event, service call, bridge method, or localization string is introduced.

## Edge and Error States

- Very long user messages wrap inside the 78% maximum width.
- Long URLs, inline code, tables, and code blocks remain readable through their existing overflow rules.
- A short assistant response and a long Markdown document both remain visually balanced.
- Image-only and image-plus-text user messages keep correct padding and expansion behavior.
- Loader positioning follows the role that owns it and does not inherit an oversized card.
- Expanded activity, reasoning, commentary, and todo bodies stay within the available width.
- Permission buttons wrap or remain reachable in narrow and split layouts.
- Semantic states remain identifiable across light and dark themes.
- Copy actions are reachable by mouse and keyboard focus.

## Verification

### Automated

- Run `./gradlew test` on JDK 25.
- Run `./gradlew build` on JDK 25.
- Keep `ApplicationStylesheetContractTest` passing so `browser-chat.css` introduces no literal palette colors.
- Confirm `browser-chat.js`, `browser-chat.html`, Java, FXML, and language resources are unchanged by this feature.

### Manual UI QA

Check in a freshly started application:

- short and long user messages;
- short and long assistant messages with headings, lists, callouts, tables, inline code, and fenced code;
- image-only and image-plus-text user messages;
- copy action at hover and keyboard focus;
- tool activity collapsed and expanded;
- reasoning and commentary collapsed and expanded;
- permission prompt before and after resolution, including error and disabled states;
- semantic accent, info, warning, success, and danger messages;
- streaming loader, active todo, and finalized todo;
- default width, narrow width, and split mode;
- one light and one dark theme.

## Success Criteria

- The conversation and composer visibly belong to one interface family.
- User messages read as compact trailing cards instead of near-full-width panels.
- Assistant responses remain comfortable for document-style reading.
- Tool activity and reasoning consume less vertical and visual attention while collapsed.
- Colored left rails no longer dominate ordinary and semantic messages.
- Existing message behavior and interactivity remain functional.
- Component CSS remains free of literal palette colors.
