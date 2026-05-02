# AlIna

AlIna is a desktop AI assistant with integrated productivity tools.

---

## OpenCode

AlIna runs on **OpenCode** — a local AI runtime that powers the chat, commands, and all AI interactions. OpenCode handles model communication, tool use, and context management under the hood.

## Commands

Commands are customizable AI prompts triggered from the chat input by typing `/`. Each command has a name, description, icon, and a system prompt template. The `$message` placeholder is replaced with your input.

## Context Menu

Commands can be exposed in a **global system-wide context menu** accessible via a configurable keyboard shortcut. Three action modes are available:

- **Paste** — copies selected text, runs the command, and pastes the AI response
- **Display** — copies selected text, runs the command, and shows the response in a popup
- **Execute** — copies selected text and runs the command silently

## Theme & Language

Change the application theme and language in **UI Settings** (gear icon → UI Settings). Custom CSS themes can be placed in the `data/css` folder and selected from the theme picker.

## Split Mode

Toggle split mode with the split button in the header or `Shift+Left/Right`. The dashboard appears side-by-side with the chat.

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| Focus shortcut | Brings the app window to focus (configurable in UI Settings) |
| Context menu shortcut | Opens the global AI context menu (configurable in UI Settings) |
| Command shortcuts | Each command can have its own paste, display, and execute shortcuts |
| `Shift+Up/Down` | Collapse / expand the dashboard |
| `Shift+Left/Right` | Toggle split mode |

## Scheduler

The scheduler runs AI commands automatically on a cron schedule. Each task has:

- **Cron expression** — Spring 6-field format (second minute hour dayOfMonth month dayOfWeek)
- **Prompt** — the message sent to the AI
- **Execution mode** — current tab, new tab, or background (silent)
- **Model** — optional override for the AI model

## Dashboard

The dashboard is a focus board displayed above (or beside in split mode) the chat. It contains configurable widgets:

### Music

Controls the currently playing media. Shows track info and playback controls.

### Tasks

Displays a checklist from a markdown file (e.g., Obsidian vault). Supports task groups via `#tags`.

### Google Calendar

Shows today's events from Google Calendar. Supports meeting notifications and AI prompts per event.

### GitHub

Lists pull requests that need your review. Requires a personal access token.

### Jira

Shows your assigned Jira issues. Requires email and API token.

### Obsidian

Displays recently edited notes from an Obsidian vault via the Obsidian CLI.
