# Alinka — zgoda zakresowa i dymek zgodny z motywem

## Kontekst

Obecna maskotka Alinka pokazuje poza aktywnym oknem aplikacji zgody z akcjami „Zezwól raz” i „Odrzuć”, a także komunikaty zakończenia oraz błędu. Rozszerzenie dodaje bezpośrednią zgodę zakresową i zmienia prostokątną kartę w zatwierdzony dymek maskotki, który wizualnie korzysta z bieżącego motywu AlIny.

Projekt rozszerza istniejącą specyfikację `2026-07-17-alinka-mascot-notifications-design.md`. Nie zmienia reguł pojawiania się popupu, kolejki, priorytetów, nawigacji, dźwięku ani ośmiosekundowego czasu komunikatów terminalnych.

## Cel i zakres

Rozszerzenie obejmuje:

- trzecią akcję zgody, gdy request deklaruje zakres `SESSION` albo `PERSISTENT`;
- poprawną, dynamiczną etykietę zakresu;
- nowy układ dymku z Alinką stojącą obok powierzchni komunikatu;
- paletę wyprowadzoną z semantycznych kolorów aktywnego motywu;
- zgodność jasnych, ciemnych i własnych motywów;
- testy zachowania zakresu, palety i regresji fokusu.

Poza zakresem pozostają nowe grafiki maskotki, historia aktywności, trwała kolejka, formularze MCP, pytania użytkownika, zgody inne niż `APPROVAL` oraz zmiany protokołów backendów.

## Zatwierdzone doświadczenie

### Układ dymku

Popup ma postać dymku skierowanego do stojącej po jego lewej stronie Alinki. Maskotka jest osobnym elementem wizualnym, a nie kolumną wewnątrz karty. Dymek ma zaokrąglenie zgodne z kartami Calm Command Center, subtelny border, miękki cień oraz niewielki ogon wskazujący Alinkę.

W dymku znajdują się:

1. mała etykieta stanu „ALINKA PYTA” i opcjonalny licznik kolejki;
2. tytuł prośby;
3. skrócony opis;
4. jeden rząd bezpośrednich akcji.

Okno ma docelowo `468 × 184 px`, z około `352 px` szerokości przeznaczonymi na dymek, dzięki czemu trzy polskie etykiety zachowują czytelność bez łamania tekstu. Akcja jednorazowa jest głównym przyciskiem akcentowym. Zgoda zakresowa używa koloru sukcesu. Odrzucenie jest spokojnym przyciskiem z semantycznym kolorem błędu. Przy dwóch akcjach przyciski dzielą dostępną szerokość po równo; przy trzech zachowują jeden rząd. Hierarchia odpowiada istniejącej karcie zgody w czacie, ale dymek zachowuje bardziej osobisty charakter maskotki.

Komunikaty `COMPLETE` i `ERROR` korzystają z tej samej geometrii dymku. Ukrywają akcje i licznik, zachowują dotychczasowe grafiki Alinki oraz obecny timeout.

### Zakres zgody

Widoczne akcje zależą wyłącznie od `AgentInteractionRequest.approvalScope()`:

| Zakres requestu | Akcje w dymku |
|---|---|
| `NONE` | „Zezwól raz”, „Odrzuć” |
| `SESSION` | „Zezwól raz”, „Zezwól w sesji”, „Odrzuć” |
| `PERSISTENT` | „Zezwól raz”, „Zezwól zawsze”, „Odrzuć” |

Środkowy przycisk wysyła istniejące `AgentInteractionAction.APPROVE_SCOPED`. Backend zachowuje własną semantykę:

- Codex mapuje akcję na zgodę dla bieżącej sesji;
- OpenCode mapuje akcję na trwałe `always` i zapis polityki.

Nie wolno wyświetlać etykiety „Zezwól zawsze” dla requestu `SESSION`. Nie wolno też oferować akcji zakresowej dla `NONE` ani wywnioskować jej z samego typu backendu.

## Paleta aktywnego motywu

Nowa mała klasa palety mapuje semantyczne kolory `SamplerTheme` na `java.awt.Color` używane przez nieaktywujące okno Swing. Korzysta z `SamplerTheme.parseColors()` i odczytuje co najmniej:

- tło overlay i subtle;
- tekst domyślny i wyciszony;
- border domyślny albo subtle;
- accent foreground, emphasis i subtle;
- success foreground i subtle;
- danger foreground i subtle.

Wbudowane motywy Calm Command Center mają bezpośrednie wartości tych tokenów. Dla własnego CSS resolver próbuje sparsować wartości hex, `rgb(...)` i `rgba(...)`. Brakujący, pośredni albo niepoprawny token otrzymuje dokładny odpowiednik z jasnej albo ciemnej palety Calm Command Center, wybrany przez `SamplerTheme.isDarkMode()`.

Paleta jest odświeżana przed każdym pokazaniem popupu. Zmiana motywu następuje w aktywnej AlInie, gdzie popup i tak jest ukryty, dlatego nie powstaje dodatkowa subskrypcja do aktualizowania już widocznego `JWindow`.

## Architektura i przepływ danych

### Model i kolejka

`MascotNotification` zachowuje `AgentInteractionApprovalScope` pochodzący z requestu. Fabryka zgody normalizuje brak zakresu do `NONE`. Powiadomienia terminalne zawsze mają `NONE`. Reguły deduplikacji i kolejki pozostają bez zmian.

### Koordynator

`MascotNotificationCoordinator` nadal jest jedyną warstwą mapującą akcje widoku na backend. Przekazuje widokowi:

- akcję `APPROVE_ONCE`;
- opcjonalną akcję `APPROVE_SCOPED`, dostępną tylko dla `SESSION` i `PERSISTENT`;
- akcję `DENY`;
- otwarcie rozmowy.

Po kliknięciu dowolnej decyzji koordynator zachowuje obecny flow: blokuje akcje, wywołuje backend na wirtualnym wątku, publikuje `AgentInteractionResolvedEvent`, wykonuje retry, jeśli `autoContinues` jest fałszywe, i przechodzi do kolejnej zgody.

### Widok

`MascotPopupView.showApproval` otrzymuje dodatkową opcjonalną akcję zakresową. Konkretny `MascotPopup` ustala etykietę na podstawie zakresu zapisanego w powiadomieniu i nie renderuje środkowego przycisku dla `NONE`.

`MascotPopup` zachowuje przezroczyste, niefokusowalne `JWindow`, `always-on-top`, pozycjonowanie względem ekranu kursora i obecne animacje. Warstwa Swing otrzymuje paletę przed każdym renderem oraz stosuje ją do dymku, tekstów, badge'a i przycisków.

### Bezpieczny start przy zmianie monitorów

Utworzenie Swingowego `JWindow` nie może blokować wątku JavaFX. macOS potrafi zatrzymać AWT w natywnym odczycie insets ekranu podczas przepinania albo odłączania monitora. Dlatego konstruktor `MascotPopup` zleca inicjalizację przez nieblokujące `SwingUtilities.invokeLater`, zamiast czekać przez `invokeAndWait`.

Operacje pokazania, ukrycia i zamknięcia popupu trafiają na tę samą kolejkę AWT. Zachowuje to ich kolejność względem inicjalizacji, a jednocześnie gwarantuje, że główny `Stage` dochodzi do `show()` nawet wtedy, gdy AWT chwilowo utknie w kodzie systemowym. Nieudana inicjalizacja wyłącza sam widok maskotki bez zatrzymywania aplikacji.

## Stany i błędy

- Podczas rozwiązywania wszystkie aktualnie widoczne przyciski są nieaktywne, a wskaźnik postępu pozostaje widoczny.
- `RESOLVED` i `MISSING` usuwają zgodę tak jak obecnie.
- `ERROR` oraz wyjątek transportowy zachowują dymek, pokazują komunikat błędu i ponownie włączają także przycisk zakresowy, jeśli był dostępny.
- Niedostępna lub częściowa paleta nie blokuje popupu; resolver stosuje fallback jasny albo ciemny.
- Brak assetu nadal wyłącza widok bez zatrzymywania aplikacji.
- Zamknięcie koordynatora nadal zatrzymuje opóźnione rozwiązywanie i zamyka `JWindow`.

## Lokalizacja i dokumentacja

Nowe teksty maskotki mają osobne klucze polskie i angielskie:

- „Zezwól w sesji” / “Allow for session”;
- „Zezwól zawsze” / “Always allow”.

Podręcznik opisuje, że zakres środkowej akcji zależy od runtime'u i konkretnego requestu. Istniejące teksty kart czatu nie są zmieniane.

## Testy

Testy jednostkowe i kontraktowe obejmują:

- przeniesienie `SESSION`, `PERSISTENT` i domyślnego `NONE` do `MascotNotification`;
- brak akcji zakresowej dla `NONE`;
- wysłanie `APPROVE_SCOPED` dla `SESSION` i `PERSISTENT`;
- zachowanie `APPROVE_ONCE` i `DENY`;
- ponowne włączenie wszystkich właściwych akcji po błędzie;
- parsowanie wymaganych formatów kolorów;
- fallback palety dla niepełnego i niepoprawnego motywu;
- zachowanie dotychczasowych testów kolejki, priorytetów, ustawienia i zasobów.
- nieblokujące zlecenie inicjalizacji popupu z wątku JavaFX.

Końcowy smoke test na macOS obejmuje:

- dymek zgody w jasnym i ciemnym Calm Command Center;
- oba warianty etykiety zakresowej i request bez zakresu;
- kliknięcie każdej akcji;
- inline error oraz ponowne odblokowanie przycisków;
- `COMPLETE` i `ERROR` bez akcji;
- pozycję nad Dockiem oraz brak zmiany aktywnej aplikacji podczas samego pokazania popupu.

Pełna walidacja kończy się poleceniami `./gradlew test` i `./gradlew build`.

## Kryteria akceptacji

- Użytkownik może udzielić na popupie jednorazowej, sesyjnej albo trwałej zgody dokładnie wtedy, gdy request na to pozwala.
- Etykieta nigdy nie obiecuje trwalszego zakresu niż obsługiwany przez request.
- Dymek jest rozpoznawalnie związany z Alinką i jednocześnie wizualnie odpowiada aktywnemu motywowi aplikacji.
- Wszystkie trzy decyzje pozostają czytelne po polsku i angielsku.
- Błąd rozwiązania nie usuwa zgody ani nie pozostawia przycisków zablokowanych.
- Popup nadal nie przejmuje fokusu i nie tworzy pozycji w Docku ani pasku zadań.
- Odłączenie lub przepięcie monitora nie może zatrzymać startu głównego okna AlIny na inicjalizacji Swing.
