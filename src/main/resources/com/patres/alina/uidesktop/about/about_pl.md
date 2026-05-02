# AlIna

AlIna to desktopowy asystent AI ze zintegrowanymi narzędziami produktywności.

---

## OpenCode

> [!info] Silnik aplikacji
> AlIna działa na bazie **OpenCode** — lokalnego runtime AI, który napędza czat, komendy i wszystkie interakcje z AI. OpenCode zarządza komunikacją z modelem, narzędziami i kontekstem.

---

## Komendy

Wpisz `/` w polu czatu, aby przeglądać i uruchamiać komendy. Każda komenda ma nazwę, ikonę i **szablon promptu systemowego** — placeholder `$message` jest zastępowany Twoim tekstem.

> [!tip] Własne prompty
> Twórz własne komendy w ustawieniach, aby automatyzować powtarzalne zadania AI. Każdej komendzie możesz przypisać unikalną ikonę i skrót klawiszowy.

---

## Menu kontekstowe

Komendy mogą być dostępne w **globalnym systemowym menu kontekstowym** — wywoływanym skrótem klawiszowym z dowolnej aplikacji w systemie.

| Tryb | Co robi |
|---|---|
| **Wklej** | Kopiuje zaznaczony tekst, uruchamia komendę, wkleja odpowiedź AI |
| **Wyświetl** | Kopiuje zaznaczony tekst, uruchamia komendę, pokazuje odpowiedź w oknie |
| **Wykonaj** | Kopiuje zaznaczony tekst, uruchamia komendę cicho w tle |

> [!note] Działa wszędzie
> Skrót menu kontekstowego jest globalny — działa nawet gdy AlIna nie jest aktywna. Skonfiguruj go w Ustawieniach UI.

---

## Motyw i język

Zmień wygląd i język w **Ustawieniach UI** (ikona zębatki w nagłówku).

> [!tip] Własne motywy
> Wrzuć własne pliki `.css` do folderu `~/.config/AlIna/themes` — pojawią się automatycznie na liście motywów.

---

## Tryb podzielony (Split Mode)

Włącz tryb podzielony przyciskiem w nagłówku lub `Shift+Left/Right`. Dashboard przesuwa się na bok i wyświetla obok czatu.

---

## Skróty klawiszowe

| Skrót | Akcja |
|---|---|
| **Skrót fokusu** | Przenosi okno aplikacji na pierwszy plan |
| **Skrót menu kontekstowego** | Otwiera globalne menu AI |
| **Skróty komend** | Skróty wklejania, wyświetlania i wykonywania per komenda |
| `Shift+Góra/Dół` | Zwiń / rozwiń dashboard |
| `Shift+Left/Right` | Przełącz tryb podzielony |

> [!info] Konfigurowalne
> Skróty fokusu i menu kontekstowego można zmienić w Ustawieniach UI. Skróty komend ustawia się w edytorze danej komendy.

---

## Harmonogram (Scheduler)

Automatyzuj komendy AI według **harmonogramu cron**. Każde zaplanowane zadanie ma:

- **Wyrażenie cron** — format Spring 6-polowy (np. `0 0 8 * * MON-FRI`)
- **Prompt** — treść wysyłana do AI
- **Tryb wykonania** — bieżąca zakładka, nowa zakładka lub w tle (cicho)
- **Model** — opcjonalne nadpisanie modelu AI

> [!success] Zadania w tle
> Zadania schedulera mogą działać cicho w tle — bez otwierania zakładek, bez przerywania pracy.

---

## Dashboard

Dashboard to **focus board** wyświetlany nad czatem (lub obok w trybie podzielonym). Wszystkie widgety można włączać i wyłączać w Ustawieniach Dashboardu.

---

### Muzyka

Kontrola aktualnie odtwarzanej muzyki. Pokazuje informacje o utworze i przyciski sterowania.

### Zadania

Wyświetla checklistę z **pliku markdown** (np. vault Obsidian). Obsługuje grupowanie zadań przez `#tagi`.

### Google Calendar

Pokazuje **dzisiejsze wydarzenia** z Google Calendar. Wspiera powiadomienia z linkiem do spotkania i prompty AI per wydarzenie.

### GitHub

Lista **pull requestów** wymagających Twojego review. Wymaga personal access token.

### Jira

Pokazuje **przypisane zadania**. Wymaga emaila Atlassian i tokenu API.

### Obsidian

Wyświetla **ostatnio edytowane notatki** z vaulta Obsidian przez Obsidian CLI.
