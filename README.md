# AlIna Desktop — AI Chat Application

AlIna is a native desktop AI chat application. It combines a modern JavaFX UI with Spring Boot services running in the same process. 
Conversations and settings are stored locally, while integrations (Spring AI/OpenAI, MCP) enable powerful tool usage.

Key features:
- Chat UI with streaming responses.
- Local storage of conversations and settings in `data/`.
- Theming and styling (Atlantafx, custom CSS in `data/css/`).
- Integrations with Spring AI (OpenAI) and MCP client.
- Embedded Spring Boot backend (no separate server required).

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
- `data/` — app data (conversations, commands, CSS themes).
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

- Conversations: `data/conversations/`
- Commands: `data/commands/`
- User themes (CSS): `data/css/`
- Local config: `config/application.local.yml` (untracked, no secrets).

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
