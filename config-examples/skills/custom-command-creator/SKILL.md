---
name: custom-command-creator
description: Tworzy i aktualizuje custom commandy OpenCode/AlIna w Markdownu. Uzyj, gdy uzytkownik chce dodac, poprawic albo opisac slash/custom command, ustawic metadane, ikone, widocznosc lub skrot klawiaturowy.
---

# Custom Command Creator

## Cel

Pomagaj tworzyc custom commandy jako pliki Markdown kompatybilne z OpenCode i AlIna UI.

Domyslnie zapisuj nowa komende w lokalnym working directory:

```text
.opencode/commands/<slug>.md
```

Globalny zapis do `~/.config/opencode/commands/<slug>.md` wybieraj tylko wtedy, gdy uzytkownik wyraznie chce komende globalna albo nie da sie ustalic sensownego working directory.

## Workflow

1. Ustal nazwe, opis i intencje komendy.
2. Wybierz slug pliku w kebab-case, np. `obsidian-polish.md`.
3. Utworz katalog `.opencode/commands/`, jesli nie istnieje.
4. Zapisz plik Markdown z frontmatter i trescia promptu.
5. Jesli uzytkownik nie podal ikon, skrotow albo widocznosci, ustaw domyslne metadane.
6. Nie tworz komendy globalnej, jesli prosba dotyczy aktualnego projektu/vaulta.

## Format pliku

Minimalny format:

```markdown
---
id: my-command
name: "My command"
description: "Krotki opis widoczny w UI"
icon: bi-slash
state: ENABLED
showInChat: true
showInContextMenuPaste: true
showInContextMenuDisplay: true
---

Instrukcja dla modelu.

"""
$ARGUMENTS
"""
```

## Metadane

Wspierane pola frontmatter:

- `id`: stabilny identyfikator komendy. Preferuj slug bez rozszerzenia, np. `daily-note-review`.
- `name`: nazwa widoczna w UI.
- `description`: krotki opis komendy.
- `icon`: kod ikony Ikonli, np. `bi-journal-richtext`.
- `model`: opcjonalny model OpenCode dla tej komendy, np. `openai/gpt-5.4` albo `google/gemini-2.5-pro`.
- `state`: `ENABLED` albo `DISABLED`.
- `showInChat`: czy pokazywac w selektorze komend czatu.
- `showInContextMenuPaste`: czy pokazywac w menu kontekstowym jako akcje copy/paste.
- `showInContextMenuDisplay`: czy pokazywac w menu kontekstowym jako akcje wyswietlenia wyniku.
- `copyAndPasteShortcut`: opcjonalny skrot, ktory wykonuje komende i wkleja wynik.
- `displayShortcut`: opcjonalny skrot, ktory wykonuje komende i pokazuje wynik.

Domyslne wartosci, gdy uzytkownik ich nie poda:

- `icon: bi-slash`
- `state: ENABLED`
- `showInChat: true`
- `showInContextMenuPaste: true`
- `showInContextMenuDisplay: true`
- brak wymuszonego modelu, czyli uzyj aktualnego modelu czatu
- brak skrotow klawiaturowych

## Skroty klawiaturowe

Skrot ma strukture:

```yaml
copyAndPasteShortcut:
  modeKeys:
    - META
    - null
    - null
  executeKey: R
```

Drugi wariant:

```yaml
displayShortcut:
  modeKeys:
    - CONTROL
    - SHIFT
    - null
  executeKey: P
```

Zasady:

- `modeKeys` to maksymalnie trzy modyfikatory.
- Uzywaj `null` dla pustych slotow, gdy chcesz zachowac czytelny format.
- Na macOS `META` oznacza Command.
- Najczestsze modyfikatory: `META`, `CONTROL`, `ALT`, `SHIFT`.
- Najczestsze klawisze wykonania: `A`-`Z`, `KEY_0`-`KEY_9`, `ENTER`, `SPACE`, `TAB`, `F1`-`F12`.
- Nie ustawiaj skrotu bez pytania, jesli moze kolidowac z istniejacymi commandami.

## Ikony

AlIna uzywa Ikonli. Preferuj Bootstrap Icons z prefixem `bi-`. Dzialaja tez Material Design z prefixem `mdal-`, jesli sa juz uzyte w projekcie.

Pelna lokalna lista ikon jest w [references/icons.md](references/icons.md). Czytaj ten plik, gdy potrzebujesz znalezc konkretna ikone albo uzytkownik prosi o pelna liste.

Bezpieczna lista ikon:

- `bi-slash`
- `bi-slash-square-fill`
- `bi-plug`
- `bi-terminal`
- `bi-code-slash`
- `bi-braces`
- `bi-file-earmark-text`
- `bi-file-earmark-code`
- `bi-journal`
- `bi-journal-text`
- `bi-journal-richtext`
- `bi-card-checklist`
- `bi-check2-square`
- `bi-list-task`
- `bi-pencil-square`
- `bi-type-underline`
- `bi-translate`
- `bi-chat-left-text`
- `bi-chat-dots`
- `bi-lightning-charge`
- `bi-magic`
- `bi-stars`
- `bi-search`
- `bi-calendar-day`
- `bi-calendar-check`
- `bi-sunrise`
- `bi-signpost-split`
- `bi-diagram-3`
- `bi-kanban`
- `bi-bookmark`
- `bi-folder`
- `bi-archive`
- `bi-globe`
- `bi-link-45deg`
- `bi-clipboard`
- `bi-clipboard-check`
- `bi-gear`
- `mdal-g_translate`

Gdy nie pasuje zadna ikona, uzyj `bi-slash`.

## Przyklad

```markdown
---
id: summarize-note
name: "Summarize note"
description: "Streszcza zaznaczona notatke w kilku punktach"
icon: bi-journal-text
model: openai/gpt-5.4
state: ENABLED
showInChat: true
showInContextMenuPaste: true
showInContextMenuDisplay: true
copyAndPasteShortcut:
  modeKeys:
    - META
    - SHIFT
    - null
  executeKey: S
---

Stresc ponizszy tekst po polsku w 5-7 punktach.
Zachowaj decyzje, taski i nazwy wlasne.
Zwroc tylko gotowa tresc.

"""
$ARGUMENTS
"""
```
