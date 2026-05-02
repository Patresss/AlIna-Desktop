# AlIna

AlIna is a desktop AI assistant with integrated productivity tools.

---

## OpenCode

> [!info] Core Engine
> AlIna runs on **OpenCode** — a local AI runtime that powers the chat, commands, and all AI interactions. OpenCode handles model communication, tool use, and context management under the hood.

---

## Commands

Type `/` in the chat input to browse and run commands. Each command has a name, icon, and a **system prompt template** — the `$ARGUMENTS` placeholder gets replaced with your input.

> [!tip] Custom prompts
> Create your own commands in the settings to automate repetitive AI tasks. You can assign each command a unique icon and keyboard shortcut.

---

## Context Menu

Commands can be exposed in a **global system-wide context menu** — accessible via a keyboard shortcut from any app on your system.

| Mode | What it does |
|---|---|
| **Paste** | Copies selected text, runs the command, pastes the AI response |
| **Display** | Copies selected text, runs the command, shows the response in a popup |
| **Execute** | Copies selected text, runs the command silently in the background |

> [!note] Works everywhere
> The context menu shortcut is global — it works even when AlIna is not focused. Configure it in UI Settings.

---

## Theme & Language

Change the look and language in **UI Settings** (gear icon in the header).

> [!tip] Custom themes
> Drop your own `.css` files into the `~/.config/AlIna/themes` folder — they will appear in the theme picker automatically.

---

## Split Mode

Toggle split mode with the header button or `Shift+Left/Right`. The dashboard slides to the side and displays next to the chat.

---

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| **Focus shortcut** | Brings the app window to focus |
| **Context menu shortcut** | Opens the global AI context menu |
| **Command shortcuts** | Per-command paste, display, and execute shortcuts |
| `Shift+Up/Down` | Collapse / expand the dashboard |
| `Shift+Left/Right` | Toggle split mode |

> [!info] Configurable
> Focus and context menu shortcuts can be changed in UI Settings. Command shortcuts are set per-command in the command editor.

---

## Scheduler

Automate AI commands on a **cron schedule**. Each scheduled task has:

- **Cron expression** — Spring 6-field format (e.g. `0 0 8 * * MON-FRI`)
- **Prompt** — the message sent to the AI
- **Execution mode** — current tab, new tab, or background (silent)
- **Model** — optional override for the AI model

> [!success] Background tasks
> Scheduler tasks can run silently in the background — no tabs opened, no interruptions.

---

## Dashboard

The dashboard is a **focus board** displayed above the chat (or beside it in split mode). All widgets are toggleable in Dashboard Settings.

---

### Music

Controls the currently playing media. Shows track info and playback controls.

### Tasks

Displays a checklist loaded from a **markdown file** (e.g. an Obsidian vault). Supports grouping tasks with `#tags`.

### Google Calendar

Shows **today's events** from Google Calendar. Supports meeting join-link notifications and per-event AI prompts.

### GitHub

Lists **pull requests** awaiting your review. Requires a personal access token.

### Jira

Shows your **assigned issues**. Requires an Atlassian email and API token.

### Obsidian

Displays **recently edited notes** from an Obsidian vault via the Obsidian CLI.
