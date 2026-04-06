# AlIna Desktop — OpenCode Client

AlIna is a native desktop AI client for OpenCode. It combines a modern JavaFX UI with Spring Boot services running in the same process.
Conversations and settings are stored locally, while the main chat runtime, tools, skills, terminal access, and MCP execution flow through OpenCode.
https://github.com/Patresss/AlIna-Desktop/blob/main/examples/commands.png?raw=true

### Dark mode
![](https://github.com/Patresss/AlIna-Desktop/blob/main/examples/main_dark.png?raw=true)

### Light Mode
![](https://github.com/Patresss/AlIna-Desktop/blob/main/examples/light.png?raw=true)


### Comands
![](https://github.com/Patresss/AlIna-Desktop/blob/main/examples/commands.png?raw=true)


Key features:
- Chat UI with streaming responses.
- Local storage of conversations, settings and assistant state in `~/.config/AlIna/`.
- Theming and styling (Atlantafx, custom CSS in `~/.config/AlIna/themes/`).
- OpenCode-backed chat runtime with approvals, skills, local tools and MCP.
- Embedded Spring Boot backend (no separate server required).
- Workspace dashboard above the chat with always-visible checklist tasks.
- File-based local skills, profile memory and prompt inbox under `~/.config/AlIna/`.
- Persistent always-on-top setting and workspace-focused configuration panel.
- Starter custom commands for morning planning, standups, decisions and Obsidian cleanup.

## Requirements

- JDK 21+ (JDK 21/24 recommended) with `jpackage` available.
- OS: macOS, Windows, or Linux (x64/aarch64 with a matching JDK).

## Build & Run

- Build: `./gradlew clean build`
- Run UI: `./gradlew run`
- Tests: `./gradlew test`

Before first use, set your API key (e.g., OpenAI) as an environment variable or in a local config file:
- `SPRING_AI_OPENAI_API_KEY=<your_key>`
- Keep local overrides in `config/` (e.g., `application.local.yml`). Do not commit secrets.

## Project Structure (Summary)

- `src/main/java/`
  - `com.patres.alina.uidesktop` — JavaFX UI and input.
  - `com.patres.alina.server` — Spring Boot services, APIs, storage.
  - `com.patres.alina.common` — shared models, events, utilities.
- `src/main/resources/` — FXML layouts and assets.
- `config/` — local configuration (YAML/JSON).
- Runtime storage: `~/.config/AlIna/`
- `logs/` — application logs.

## Creating Installers

Recommended path: fat‑JAR packaging (most reliable across platforms) — task `jpackageFat`.

- macOS DMG: `./gradlew -x test jpackageFat -PinstallerType=dmg`
- macOS PKG: `./gradlew -x test jpackageFat -PinstallerType=pkg`
- Windows EXE: `./gradlew -x test jpackageFat -PinstallerType=exe`
- Windows MSI: `./gradlew -x test jpackageFat -PinstallerType=msi`
- Linux DEB: `./gradlew -x test jpackageFat -PinstallerType=deb`
- Linux RPM: `./gradlew -x test jpackageFat -PinstallerType=rpm`
- Platform default: `./gradlew createInstallerFat`

Advanced (runtime image + jlink): `./gradlew jpackage`
- May require additional JPMS tuning for Spring/Reactive. If you hit errors, prefer the fat‑JAR route above.
- You can also zip the app image: `./gradlew packageExecutableZip`.

Outputs are written to `release/<version>/`, for example:
- macOS: `release/1.0-SNAPSHOT/AlIna-1.0.0.dmg` or `.pkg`
- Windows: `release/1.0-SNAPSHOT/AlIna-1.0.0.exe` or `.msi`
- Linux: `release/1.0-SNAPSHOT/AlIna-1.0.0.deb` or `.rpm`

Options & metadata:
- Force installer type: `-PinstallerType=<dmg|pkg|exe|msi|deb|rpm|app-image>`
- App name: `AlIna`
- macOS bundle id: `com.patres.alina`
- Version: taken from Gradle `version`; automatically sanitized to a numeric format (e.g., `1.0.0`) when required by `jpackage`.
- Icons (optional):
  - macOS: `src/main/resources/icon/desktop/main_icon.icns`
  - Windows/Linux: `src/main/resources/icon/desktop/main_icon.ico`

## Data & Configuration

- Conversations: `~/.config/AlIna/conversations/`
- Commands: local OpenCode commands from `<working-directory>/.opencode/commands/` first, then global `~/.config/opencode/commands/`.
- User themes (CSS): `~/.config/AlIna/themes/`
- Skills: `~/.config/opencode/skills/` via native OpenCode config.
- Assistant profile: `~/.config/AlIna/profile/default/`
- MCP config: `~/.config/opencode/opencode.json`.
- App config: `~/.config/AlIna/config/`
- Local config: `config/application.local.yml` (untracked, no secrets).

## Workspace Features

- `Focus Board`: a persistent dashboard above the chat showing checklist tasks from `profile/default/focus.md` or an Obsidian file.
- `Context Settings`: configure always-on-top behavior, Obsidian vault paths, profile files and skill directories.
- `Memory`: persistent reusable context lives in `profile/default/memory.md`.
- `Learned Notes`: short conversation journal appended to `profile/default/learned-notes.md`.
- `Prompt Inbox`: lightweight backlog for ideas and prompts in `profile/default/prompt-inbox.md`.
- `Skills`: drop folder-based skills into `skills/<name>/SKILL.md` and activate them in chat with `#skill:<name>`.
- `OpenCode`: AlIna starts and configures a local OpenCode backend, then uses it for chat execution, tools, skills, bash and MCP.

Architecture notes:
- [`docs/assistant-architecture.md`](docs/assistant-architecture.md)

Example skill usage:
- `#skill:code-review check this diff`
- `#skill:obsidian clean up this note`

## Signing & Distribution (Optional)

- macOS: use `jpackage` signing flags (`--mac-sign`, `--mac-signing-key-user-name`, `--mac-signing-keychain`) and notarization if required.
- Windows: sign the installer with `signtool` after build or integrate a signing step in Gradle.

## Troubleshooting

- `jpackage` not found: use a full JDK that includes `jpackage` and ensure it’s on PATH (`java --version`, `jpackage --version`).
- macOS Gatekeeper: unsigned apps may require right‑click → Open, or codesigning/notarization.
- jlink/JPMS errors: use `jpackageFat` (recommended) instead of `jpackage`.
- Missing icons: tasks skip icon configuration if the files are absent.

## For Developers

- Build: `./gradlew clean build`
- Run UI: `./gradlew run`
- Tests: `./gradlew test` (JUnit 5)
- Quick installer: `./gradlew createInstallerFat`
