# AlIna — Manual

AlIna to natywna desktopowa aplikacja AI zbudowana na JavaFX i Spring Boot. Działa jako klient dla OpenCode — lokalnego środowiska uruchomieniowego AI obsługującego modele, narzędzia, MCP i dostęp do terminala. Wszystkie dane (rozmowy, ustawienia, profil) przechowywane są lokalnie na dysku.

## Szybki start

### Wymagania

- JDK 21+ (zalecany 24)
- Klucz API dostawcy AI (np. OpenAI)

### Uruchomienie

```bash
export SPRING_AI_OPENAI_API_KEY=<twój_klucz>
./gradlew run
```

Klucz API można też ustawić w pliku `config/application.local.yml` (nie jest śledzony przez git).

## Główne funkcjonalności

### Chat z AI

Główny obszar aplikacji to okno czatu ze streamowanymi odpowiedziami AI. Wiadomości renderowane są w formacie Markdown. Obsługiwane są załączniki graficzne — można przeciągnąć lub wkleić obraz do pola wiadomości.

**Model AI** — domyślnie `gpt-4o`. Model można zmienić w pasku statusu na dole okna czatu. Format: `provider/model` (np. `openai/gpt-4o`, `anthropic/claude-3`).

### Zakładki (Tabs)

Można prowadzić wiele równoległych sesji czatu w zakładkach. Dwuklik na nazwie zakładki pozwala ją zmienić. Każda zakładka to oddzielna konwersacja z osobnym kontekstem.

### Quick Actions (`/`)

Wpisanie `/` w polu wiadomości otwiera menu szybkich akcji:

- **Clear Chat** — wyczyść bieżącą rozmowę
- **New Chat** — nowa sesja
- **Models** — zmiana modelu AI
- **History** — wyszukiwanie w historii sesji
- **Commands** — lista własnych poleceń
- **UI Settings / Dashboard Settings / OpenCode Settings** — otwieranie ustawień
- **Theme** — zmiana motywu
- **Language** — zmiana języka (en/pl)
- **Add Task** — dodanie zadania do Focus Board

### Custom Commands (Polecenia)

Szablony promptów wielokrotnego użytku. Każde polecenie ma:

- nazwę, opis, ikonę
- szablon system prompt z placeholderem `$ARGUMENTS`
- opcjonalne nadpisanie modelu AI
- skróty klawiaturowe dla trybów paste/display/execute
- ustawienia widoczności (picker czatu, menu kontekstowe, ekran powitalny)

Polecenia można tworzyć i edytować w **Settings → Commands**. Pliki poleceń przechowywane są w `<katalog_roboczy>/.opencode/commands/` lub `~/.config/opencode/commands/`.

Przykładowe polecenia: `morning-plan`, `standup-sync`, `translate-to-english-or-polish`, `popraw-bledy`.

### Systemowe menu kontekstowe

Globalne menu dostępne z dowolnej aplikacji w systemie za pomocą skrótu klawiaturowego. Trzy tryby:

- **Paste** — kopiuje zaznaczony tekst, wysyła do AI, wkleja odpowiedź z powrotem
- **Display** — kopiuje tekst, wysyła do AI, pokazuje wynik w osobnym okienku
- **Execute** — kopiuje tekst, wysyła do AI w tle (bez wyświetlania)

Skrót klawiaturowy konfigurowalny w **Settings → UI Settings**.

### Dashboard (Focus Board)

Panel widgetów wyświetlany nad lub obok czatu (tryb split). Zawiera:

- **Tasks** — lista zadań z pliku Markdown (np. Obsidian `focus.md`), grupowanie po `#tagach`, możliwość wysłania zadania jako prompt do AI
- **Google Calendar** — dzisiejsze wydarzenia, powiadomienia o spotkaniach z konfigurowalnym wyprzedzeniem
- **GitHub** — pull requesty oczekujące na review (wymaga Personal Access Token)
- **Jira** — przypisane zadania (wymaga email + API token Atlassian)
- **Obsidian** — ostatnio edytowane notatki (wymaga `obsidian-cli`)
- **Music** — sterowanie odtwarzaniem muzyki, wyświetlanie aktualnego utworu

Dashboard przełącza się klawiszami **Shift+Up/Down**. Tryb split (obok czatu): **Shift+Left/Right** lub przycisk w nagłówku.

Konfiguracja widgetów: **Settings → Dashboard**.

### Scheduler

Automatyczne zadania AI uruchamiane według harmonogramu cron. Każde zadanie ma:

- wyrażenie cron (6-polowe, format Spring)
- prompt do wykonania
- tryb: bieżąca zakładka, nowa zakładka lub w tle
- opcjonalne nadpisanie modelu

Konfiguracja: **Settings → Scheduler**.

### Skills

Rozszerzenia AI w postaci plików Markdown. Aktywowane w czacie składnią `#skill:<nazwa>`. Przechowywane w `~/.config/opencode/skills/<nazwa>/SKILL.md`.

### System uprawnień

Gdy OpenCode wymaga wykonania narzędzia, komendy bash lub akcji MCP, w polu wiadomości pojawiają się przyciski:

- **Allow** — zezwól jednorazowo
- **Always Allow** — zezwól na stałe
- **Deny** — odmów

## Pasek nagłówka

Przyciski od lewej: **New Session** | **History** | **About** | **Open OpenCode Session** | **Settings** (menu rozwijane) | **Split Mode** | **Pin** (zawsze na wierzchu).

## Ustawienia

### UI Settings

- Motyw (7 wbudowanych + własne pliki CSS w `~/.config/AlIna/themes/`)
- Język (angielski / polski)
- Skrót klawiaturowy Focus Board
- Skrót systemowego menu kontekstowego
- Dźwięk powiadomienia po odpowiedzi AI (chime, bubble, ding, soft bell, droplet, sparkle, warm pop, gentle rise)
- Przycisk Expand (rozszerzanie okna + auto-split)

### OpenCode Settings

- Hostname (domyślnie `127.0.0.1`)
- Port (domyślnie `4096`)
- Katalog roboczy
- Model czatu

### Dashboard Settings

- Włączanie/wyłączanie widgetów
- Interwały odświeżania i limity elementów
- Dane dostępowe: GitHub PAT, Jira email/token
- Ustawienia kalendarza (ukrywanie całodniowych, tylko przyszłe, powiadomienia, wyprzedzenie)
- Ścieżka do obsidian-cli, wzorce wykluczeń
- Szablony promptów AI per widget

## Lokalizacja danych

| Co | Ścieżka (macOS) |
|----|-----------------|
| Dane aplikacji | `~/.config/AlIna/` |
| Rozmowy | `~/.config/AlIna/conversations/` |
| Konfiguracja | `~/.config/AlIna/config/` |
| Motywy | `~/.config/AlIna/themes/` |
| Polecenia | `~/.config/opencode/commands/` |
| Skills | `~/.config/opencode/skills/` |
| MCP | `~/.config/opencode/opencode.json` |

Windows: `%APPDATA%\AlIna`, Linux: `~/.local/share/AlIna`.

## Budowanie i pakowanie

```bash
./gradlew clean build          # kompilacja + testy
./gradlew run                  # uruchomienie
./gradlew test                 # same testy
./gradlew packageExecutableZip # archiwum ZIP

# Instalatory
./gradlew -x test jpackageFat -PinstallerType=dmg   # macOS DMG
./gradlew -x test jpackageFat -PinstallerType=pkg   # macOS PKG
./gradlew -x test jpackageFat -PinstallerType=exe   # Windows
./gradlew -x test jpackageFat -PinstallerType=deb   # Linux DEB
```

## Integracja z Google Calendar

Wymaga dwóch narzędzi CLI:

```bash
brew install --cask google-cloud-sdk
brew install googleworkspace-cli
```

Uwaga: Homebrew ma też pakiet `gws` niezwiązany z Google Workspace — upewnij się, że zainstalowałeś `googleworkspace-cli`. Sprawdź komendą `gws --help` — powinna opisywać funkcje Google Workspace (Calendar, Drive, Gmail), nie zarządzanie repozytoriami Git.

### Autoryzacja

```bash
gcloud auth application-default login
```

Jeśli Google blokuje domyślny client ID, użyj własnego pliku OAuth (Desktop app) z Google Cloud Console:

```bash
gcloud auth application-default login \
  --client-id-file="/path/to/client_secret.json" \
  --scopes="https://www.googleapis.com/auth/cloud-platform,https://www.googleapis.com/auth/calendar.readonly"
```

Przygotowanie pliku OAuth:
1. Utwórz projekt w Google Cloud Console
2. Włącz Google Calendar API
3. Skonfiguruj OAuth consent screen
4. Utwórz OAuth client ID typu Desktop app
5. Pobierz plik JSON — przechowuj poza repozytorium

### Rozwiązywanie problemów

- **`gcloud` nie znaleziony** — zainstaluj Google Cloud SDK, upewnij się że jest w `PATH`
- **`gws` nie znaleziony** — zainstaluj `googleworkspace-cli`
- **`gws` to nie Google Workspace CLI** — odinstaluj zły pakiet (`brew uninstall gws`), zainstaluj `googleworkspace-cli`
- **Google blokuje logowanie** — użyj własnego pliku OAuth z `--client-id-file`

Po zmianie narzędzi CLI uruchom ponownie AlIna.
