# AlIna

AlIna to desktopowy asystent AI ze zintegrowanymi narzędziami produktywności.

---

## OpenCode

AlIna działa na bazie **OpenCode** — lokalnego runtime AI, który napędza czat, komendy i wszystkie interakcje z AI. OpenCode zarządza komunikacją z modelem, narzędziami i kontekstem.

## Komendy

Komendy to konfigurowalne prompty AI uruchamiane z pola czatu po wpisaniu `/`. Każda komenda ma nazwę, opis, ikonę i szablon promptu systemowego. Placeholder `$message` jest zastępowany Twoim tekstem.

## Menu kontekstowe

Komendy mogą być dostępne w **globalnym systemowym menu kontekstowym** wywoływanym skrótem klawiszowym. Trzy tryby akcji:

- **Wklej** — kopiuje zaznaczony tekst, uruchamia komendę i wkleja odpowiedź AI
- **Wyświetl** — kopiuje zaznaczony tekst, uruchamia komendę i pokazuje odpowiedź w oknie
- **Wykonaj** — kopiuje zaznaczony tekst i uruchamia komendę cicho

## Motyw i język

Zmień motyw i język w **Ustawieniach UI** (ikona zębatki → Ustawienia UI). Własne motywy CSS można umieszczać w folderze `data/css` i wybierać z listy motywów.

## Tryb podzielony (Split Mode)

Włącz tryb podzielony przyciskiem w nagłówku lub `Shift+Left/Right`. Dashboard pojawia się obok czatu.

## Skróty klawiszowe

| Skrót | Akcja |
|---|---|
| Skrót fokusu | Przenosi okno na pierwszy plan (konfigurowalny w Ustawieniach UI) |
| Skrót menu kontekstowego | Otwiera globalne menu AI (konfigurowalny w Ustawieniach UI) |
| Skróty komend | Każda komenda może mieć własne skróty wklejania, wyświetlania i wykonywania |
| `Shift+Góra/Dół` | Zwiń / rozwiń dashboard |
| `Shift+Left/Right` | Przełącz tryb podzielony |

## Harmonogram (Scheduler)

Scheduler uruchamia komendy AI automatycznie według harmonogramu cron. Każde zadanie ma:

- **Wyrażenie cron** — format Spring 6-polowy (sekunda minuta godzina dzieńMiesiąca miesiąc dzieńTygodnia)
- **Prompt** — treść wysyłana do AI
- **Tryb wykonania** — bieżąca zakładka, nowa zakładka lub w tle (cicho)
- **Model** — opcjonalne nadpisanie modelu AI

## Dashboard

Dashboard to focus board wyświetlany nad (lub obok w trybie podzielonym) czatem. Zawiera konfigurowalne widgety:

### Muzyka

Kontrola aktualnie odtwarzanej muzyki. Pokazuje informacje o utworze i przyciski sterowania.

### Zadania

Wyświetla checklistę z pliku markdown (np. vault Obsidian). Obsługuje grupy zadań przez `#tagi`.

### Google Calendar

Pokazuje dzisiejsze wydarzenia z Google Calendar. Wspiera powiadomienia o spotkaniach i prompty AI per wydarzenie.

### GitHub

Lista pull requestów wymagających Twojego review. Wymaga personal access token.

### Jira

Pokazuje przypisane zadania Jira. Wymaga emaila i tokenu API.

### Obsidian

Wyświetla ostatnio edytowane notatki z vaulta Obsidian przez Obsidian CLI.
