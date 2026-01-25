# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/`: Java sources
  - `com.patres.alina.uidesktop`: JavaFX UI and input
  - `com.patres.alina.server`: Spring Boot services, storage, APIs
  - `com.patres.alina.common`: Shared models, events, utils
- `src/main/resources/`: FXML layouts and assets (e.g., `.../fxml/*.fxml`, `assets/styles/*.css`)
- `config/`: Local configuration (e.g., `application.local.yml`, JSON settings)
- `data/`: Local storage (e.g., `data/conversations`, `data/css` for user CSS themes)
- `logs/`: Application logs
- Build config: `build.gradle.kts`, `settings.gradle.kts`, Gradle wrapper

## Build, Test, and Development Commands
- Build: `./gradlew clean build` — compiles all modules and runs tests.
- Run (Desktop UI): `./gradlew run` — launches `com.patres.alina.AppLauncher`.
- Tests: `./gradlew test` — executes JUnit 5 tests.
- Package runtime: `./gradlew jlink` — creates a custom runtime image.
- Zip artifact: `./gradlew packageExecutableZip` — zips the runtime under `release/<version>/`.

## Coding Style & Naming Conventions
- Language: Java 24; use 4‑space indentation, no tabs.
- Classes: `PascalCase`; methods/fields: `camelCase`; constants: `UPPER_SNAKE_CASE`.
- Packages remain under `com.patres.alina.*`.
- FXML files use kebab‑case (e.g., `chat-window.fxml`).
- Prefer small, focused classes; keep UI in `uidesktop`, server logic in `server`, shared types in `common`.

## Testing Guidelines
- Framework: JUnit 5; Spring tests via `spring-boot-starter-test` when needed.
- Location: `src/test/java/...` mirroring package structure.
- Naming: `ClassNameTest.java` for unit tests; `...IT.java` for integrations.
- Run locally with `./gradlew test`; keep tests deterministic and fast.

## Commit & Pull Request Guidelines
- Commits: imperative, concise subjects (e.g., "Add message streaming"); group related changes.
- Reference issues (e.g., `#123`) and include rationale in the body when non‑trivial.
- PRs: clear description, steps to run (`./gradlew run`), config notes, and screenshots for UI changes.
- Keep diffs minimal; update docs/config samples when behavior changes.

## Security & Configuration Tips
- Do not commit secrets. Prefer env vars (e.g., `SPRING_AI_OPENAI_API_KEY`) or untracked local overrides.
- Validate config reads/writes stay within `data/` and avoid accidental path traversal.
- Local storage: conversations under `data/conversations/`; commands under `data/commands/`; user themes under `data/css/`.
