---
name: task-capture
description: Add personal tasks to a Markdown task list file, especially when the user asks to add a todo, reminder, checkbox item, shopping item, errand, or action to their task list in Obsidian or a local .md file. Use this whenever the user wants a task saved persistently in a file instead of only tracked in session state. If a `Tasks file` path is missing, ask for it before writing.
---

# Task Capture Skill

Capture tasks into the user's Markdown task file as durable checklist items.

This skill exists to avoid session-only task tracking. Write to the file, not to `todowrite`, when the user's intent is to save a real task.

Read [references/TASK_STANDARD.md](references/TASK_STANDARD.md) before writing tasks when you need the rationale behind the format.

## When To Use

Use this skill when the user asks to:

- add a task
- save a reminder
- put something on a todo list
- add a checkbox item to an Obsidian note or Markdown file
- capture errands, calls, purchases, follow-ups, or one-off next actions

## Source Of Truth

Use the `Tasks file` parameter if the invocation provides it.

If `Tasks file` is missing, ask one short question asking for the path.

If the provided path does not exist, ask whether to create it or use a different path.

## Task Standard

Write every new task as one Markdown checkbox appended to the end of the file in this exact shape:

```markdown
- [ ] Kup 3 chleby ➕ 2026-04-06
```

Rules:

- Start with `- [ ] `
- Use a concrete next action, not a vague topic
- Use Polish imperative second-person singular where natural: `Kup`, `Zadzwoń`, `Wyślij`, `Umów`, `Sprawdź`
- Do not use infinitive forms like `kupić chleb`, `zadzwonić do banku`
- Keep it as one action per line
- Append ` ➕ YYYY-MM-DD` using the current local date
- Append at the very end of the file
- Preserve existing file formatting and line endings

## Writing Rules

Convert the user's intent into an actionable task that is easy to do without further interpretation.

Prefer the smallest concrete next step:

- `Kup chleb` instead of `Zakupy`
- `Zadzwoń do dentysty i umów wizytę` instead of `Dentysta`
- `Wyślij fakturę do Ani` instead of `Faktura`

If the user gives extra detail that makes the action better, keep it:

- `Kup 3 chleby`
- `Zadzwoń do mamy w sprawie obiadu na niedzielę`

If the request is too ambiguous to produce a clear next action, ask one targeted question.

## File Editing Workflow

1. Resolve the target path from `Tasks file`.
2. Read the file.
3. Check whether the same open task already exists in a materially identical form.
4. If it already exists, tell the user instead of adding a duplicate unless they explicitly want duplicates.
5. Append the new task as the last line.
6. Confirm the exact task text and file path.

## Duplicate Heuristic

Treat these as duplicates when still unchecked:

- `- [ ] Kup chleb ➕ 2026-04-06`
- `- [ ] Kup chleb ➕ 2026-04-07`

The date does not make it a distinct task by itself.

Do not treat these as duplicates:

- `Kup chleb` vs `Kup 3 chleby`
- `Zadzwoń do banku` vs `Zadzwoń do banku w sprawie karty`

## Response Style

After writing, answer briefly:

```text
Dodane do /sciezka/do/tasks.md:
- [ ] Kup 3 chleby ➕ 2026-04-06
```

If you had to ask for the path, ask only that and wait.

## Examples

User: `Dodaj do listy zadan kupic chleb`

Output line:

```markdown
- [ ] Kup chleb ➕ 2026-04-06
```

User: `dodaj zebym zadzwonil do banku jutro`

Output line:

```markdown
- [ ] Zadzwoń do banku ➕ 2026-04-06
```

Note: unless the user's system has a richer task syntax already in active use, keep the format limited to the checkbox plus capture date. Do not invent priorities, tags, due dates, or metadata.
