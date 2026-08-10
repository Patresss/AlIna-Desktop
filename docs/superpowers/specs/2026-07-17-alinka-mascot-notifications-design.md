# Alinka — maskotka zgód i zakończenia pracy

## Cel

Alinka ma informować o zdarzeniach agenta wtedy, gdy użytkownik pracuje poza głównym oknem AlIny. Zastępuje rozbudowane centrum aktywności małym, kontekstowym popupem ponad innymi aplikacjami.

Rozwiązanie obejmuje:

- oczekujące zgody typu `APPROVAL`;
- bezpośrednie akcje „Zezwól raz”, opcjonalna zgoda zakresowa i „Odrzuć”;
- informację o zakończeniu albo błędzie pracy agenta;
- przejście do właściwej rozmowy;
- kolejkę wielu równoczesnych zgód;
- możliwość całkowitego wyłączenia maskotki w ustawieniach UI.

Rozwiązanie nie zastępuje kart interakcji w czacie ani nie przechowuje historii aktywności. Nie upraszcza formularzy MCP, pytań agenta ani interakcji URL do zestawu przycisków zgody.

## Zatwierdzone doświadczenie

Maskotką jest Alinka: własny, miękki, fioletowy stworek z dwiema antenkami. Finalna grafika jest stylizowanym, trójwymiarowym pluszakiem o zwartej sylwetce i czytelnych krawędziach.

Popup ma dwa główne układy:

1. **Zgoda** — pozostaje widoczna do rozwiązania prośby, pokazuje tytuł i opis, akcje „Zezwól raz” oraz „Odrzuć”, opcjonalną zgodę zakresową, a także licznik następnych próśb. Kliknięcie treści otwiera pełny kontekst w rozmowie.
2. **Wynik** — pokazuje „Gotowe” albo „Nie udało się”, jest w całości klikalny i znika automatycznie po ośmiu sekundach. Kliknięcie otwiera powiązaną rozmowę.

Popup pojawia się w prawym dolnym rogu ekranu, na którym znajduje się kursor. Pozycja korzysta z granic ekranu pomniejszonych o systemowe insets, aby nie zasłaniać Docka ani paska zadań. Wejście używa krótkiego przesunięcia z wygaszeniem, a widoczna maskotka delikatnie kołysze się bez stałego timera wysokiej częstotliwości.

## Reguły fokusu

Maskotka działa tylko wtedy, gdy główne okno AlIny nie jest aktywne albo jest zminimalizowane.

- Zgoda odebrana przy aktywnej AlInie trafia do kolejki, ale popup pozostaje ukryty, ponieważ karta jest już widoczna w czacie.
- Jeżeli użytkownik opuści AlInę bez rozwiązania zgody, popup pojawia się po utracie fokusu.
- Powrót do głównego okna ukrywa popup zgody, ale nie usuwa zgody z kolejki.
- Zakończenie lub błąd odebrane przy aktywnej AlInie nie są odkładane do późniejszego pokazania.
- Samo pokazanie popupu nie wywołuje `requestFocus()` ani `toFront()`. Fokus może zmienić się dopiero po świadomym kliknięciu popupu.

Popup jest niezależnym, przezroczystym oknem `always-on-top`, bez ownera głównego Stage, dzięki czemu może pozostać widoczny również przy zminimalizowanej AlInie. Implementacja nie może pokazywać dodatkowej pozycji w Docku lub pasku zadań. Zachowanie nieprzejmowania fokusu jest obowiązkowym elementem smoke testu na macOS. Test wykazał, że przezroczysty JavaFX `Stage` aktywuje inną aplikację przy samym `show()`, dlatego finalny widok używa nieaktywującego, niefokusowalnego `JWindow`; nie stosujemy cichego fallbacku do zwykłego powiadomienia systemowego.

## Architektura

### `MascotNotificationCoordinator`

Jeden koordynator jest tworzony przez `AssistantAppLauncher` i otrzymuje główny `Stage` oraz `ApplicationWindow`. Subskrybuje:

- `ChatMessageStreamEvent` dla `AGENT_INTERACTION`, `COMPLETE`, `ERROR` i `CANCELLED`;
- `AgentInteractionResolvedEvent` dla zgód rozwiązanych z poziomu czatu lub backendu;
- zmianę fokusu i stanu zminimalizowania głównego Stage.

Koordynator odpowiada za deduplikację, priorytety, kolejkę, czas życia komunikatów i wywołanie backendu. Nie buduje kontrolek widoku.

### `MascotPopup`

Widok posiada przezroczyste, nieaktywujące `JWindow`, grafikę Alinki, etykiety, licznik kolejki i maksymalnie trzy przyciski Swing. Udostępnia koordynatorowi operacje pokazania zgody, pokazania wyniku, pokazania błędu rozwiązywania oraz ukrycia. Nie zna event busa ani `BackendApi`.

### `MascotScreenPositioner`

Mała, czysta klasa wylicza pozycję popupu z geometrii ekranów, kursora, rozmiaru popupu i marginesu. Logika nie zależy od aktywnego Stage i może być testowana bez uruchamiania warstwy okienkowej.

### Nawigacja

`ApplicationWindow` dostaje jedną publiczną operację aktywowania wątku po `threadId`. Dla otwartej zakładki przełącza ją bez tworzenia duplikatu. Następnie launcher przywraca główny Stage i prosi system o fokus. W pierwszym przyroście maskotka obsługuje tylko sesje obecne w aplikacji, ponieważ zgody i eventy zakończenia pochodzą z aktywnych streamów tych sesji.

## Model kolejki

Oczekujące zgody są przechowywane w kolejności nadejścia i deduplikowane po `requestId`. W danej chwili widoczna jest najwyżej jedna zgoda. Licznik pokazuje liczbę pozostałych próśb, bez bieżącej.

Priorytet elementów:

1. oczekująca zgoda;
2. błąd zakończenia pracy;
3. pomyślne zakończenie pracy.

Powiadomienia terminalne są krótkotrwałe. Jeśli zgoda jest widoczna, terminalny komunikat czeka na jej rozwiązanie. Koordynator zachowuje najwyżej pięć najnowszych komunikatów terminalnych, deduplikuje je po `threadId` i odrzuca elementy starsze niż 60 sekund, aby po dłuższej nieobecności nie odtwarzać nieaktualnego stosu. Kolejka istnieje wyłącznie w pamięci i jest czyszczona przy zamknięciu aplikacji.

`CANCELLED` usuwa związane oczekiwania streamu, ale nie tworzy popupu wyniku.

## Rozwiązywanie zgody

Podstawowe przyciski mapują się na:

- `AgentInteractionAction.APPROVE_ONCE`;
- opcjonalne `AgentInteractionAction.APPROVE_SCOPED`, gdy request deklaruje `SESSION` albo `PERSISTENT`;
- `AgentInteractionAction.DENY`.

Po kliknięciu koordynator blokuje widoczne akcje i pokazuje stan przetwarzania. Na wirtualnym wątku wywołuje `BackendApi.resolveAgentInteraction(requestId, response)`.

- `RESOLVED`: usuwa prośbę, publikuje `AgentInteractionResolvedEvent`, aby karta w czacie została oznaczona jako rozwiązana, i pokazuje kolejną zgodę. Jeżeli `autoContinues` jest fałszywe, koordynator wywołuje `BackendApi.retryLastUserMessage(threadId)`, odtwarzając zachowanie kontrolera czatu.
- `MISSING`: usuwa nieaktualną prośbę i przechodzi do kolejnej.
- `ERROR`: zachowuje prośbę, pokazuje komunikat i ponownie odblokowuje akcje.
- wyjątek transportowy: jest traktowany jak `ERROR`; nie usuwa prośby.

Publikacja `AgentInteractionResolvedEvent` po decyzji z maskotki jest lokalną synchronizacją UI. Koordynator usuwa wpis idempotentnie, więc późniejszy event backendu nie powoduje błędu ani przejścia o dwa miejsca w kolejce.

Szczegóły zakresowej akcji, etykiet oraz dymku zgodnego z motywem opisuje rozszerzenie `2026-07-20-alinka-scoped-approval-themed-bubble-design.md`. Kliknięcie treści lub nazwy prośby nadal otwiera rozmowę, gdzie użytkownik może przeczytać pełny kontekst.

## Eventy terminalne

`COMPLETE` przy nieaktywnej AlInie tworzy komunikat „Gotowe”. `ERROR` tworzy czerwony komunikat „Nie udało się” z bezpiecznie skróconym opisem błędu. Oba są powiązane z `threadId`, znikają po ośmiu sekundach i po kliknięciu otwierają rozmowę.

Istniejące odtwarzanie dźwięku przy `COMPLETE` pozostaje w kontrolerze streamu, dlatego maskotka nie odtwarza drugiego dźwięku. Nowa zgoda odtwarza pojedynczy skonfigurowany dźwięk powiadomienia tylko wtedy, gdy popup faktycznie pojawia się poza aktywną AlIną.

## Ustawienia i lokalizacja

`UiSettings` otrzymuje pole `mascotNotificationsEnabled` z domyślną wartością `true`, aby starsze pliki ustawień zachowały kompatybilność. W ustawieniach UI pojawia się pojedynczy przełącznik „Maskotka powiadomień”. Wyłączenie:

- natychmiast ukrywa popup;
- czyści krótkotrwałe komunikaty terminalne;
- nie wpływa na karty zgód w czacie ani działanie agentów;
- nie rozwiązuje oczekujących zgód.

Wszystkie teksty popupu, tooltipy, ustawienie i komunikaty błędów mają wersje polską i angielską.

## Asset Alinki

Finalne assety powstały po zatwierdzeniu specyfikacji jako stylizowany, trójwymiarowy pluszowy stworek na jednolitym tle chroma-key. Najpierw powstała czujna Alinka dla zgody, a wariant „Gotowe” został przygotowany jako edycja tego samego obrazu ze zmienioną miną i pozą. Tło obu obrazów usunięto lokalnym narzędziem `imagegen`, a wyniki zweryfikowano pod kątem kanału alfa, czystych krawędzi, spójnej sylwetki i braku zielonej obwódki.

Pliki `alinka-permission.png` i `alinka-complete.png` trafiają do `src/main/resources/com/patres/alina/uidesktop/assets/mascot/`. Stan błędu używa czujnej pozy wraz z czerwonym kolorem komunikatu. Kod nie odwołuje się do plików pozostawionych poza repozytorium.

## Obsługa błędów i lifecycle

- Puste `threadId`, brak requestu lub interakcja inna niż `APPROVAL` nie tworzą bezpośredniej karty decyzyjnej.
- Wielokrotny event tej samej zgody nie tworzy duplikatu.
- Zamknięcie aplikacji zatrzymuje animacje i timery oraz zamyka popup.
- Zmiana motywu nie wymaga regenerowania grafiki; widok korzysta z semantycznych kolorów aplikacji.
- Brak assetu nie blokuje uruchomienia aplikacji: widok loguje błąd i nie pokazuje popupu zamiast wyświetlać uszkodzoną kontrolkę.
- Błąd pozycjonowania używa prawego dolnego rogu ekranu głównego.
- Jeżeli popup jest ukryty przez powrót fokusu do AlIny, nierozwiązana zgoda pozostaje w kolejce.

## Testy

Testy jednostkowe koordynatora obejmują:

- brak popupu przy aktywnej AlInie;
- pokazanie nierozwiązanej zgody po utracie fokusu;
- ukrycie bez usunięcia zgody po odzyskaniu fokusu;
- deduplikację requestów i kolejność FIFO;
- licznik pozostałych zgód;
- priorytet zgód nad eventami terminalnymi;
- mapowanie `APPROVE_ONCE` i `DENY`;
- obsługę `RESOLVED`, `MISSING`, `ERROR` oraz wyjątku;
- publikację `AgentInteractionResolvedEvent` i retry dla `autoContinues=false`;
- limit, 60-sekundowe wygasanie oraz deduplikację terminalnych komunikatów;
- timeout ośmiu sekund i nawigację do właściwego wątku;
- wyłączenie funkcji ustawieniem.

Testy `MascotScreenPositioner` obejmują ekran główny, drugi monitor, ujemne współrzędne i ograniczenie do dostępnych granic ekranu. Testy kontraktowe sprawdzają obecność i przezroczystość assetów oraz kompletność lokalizacji.

Weryfikacja końcowa:

- `./gradlew test`;
- `./gradlew build`;
- smoke test popupu zgody, powodzenia i błędu;
- ręczne potwierdzenie na macOS, że samo pojawienie się popupu nie aktywuje AlIny, popup pozostaje ponad innymi aplikacjami, akcje działają jednym kliknięciem, a pozycja respektuje Dock.

## Kryteria akceptacji

- Nierozwiązana zgoda nie pozostaje niewidoczna po przejściu do innej aplikacji.
- Użytkownik może bezpiecznie zezwolić jednorazowo albo odrzucić bez otwierania AlIny.
- Zatwierdzenie z maskotki aktualizuje kartę w rozmowie i pozwala agentowi kontynuować.
- Wiele zgód nie tworzy wielu okien i żadna nie znika z kolejki.
- Pomyślne zakończenie oraz błąd są widoczne poza AlIną i prowadzą do właściwej rozmowy.
- Samo pojawienie się maskotki nie kradnie fokusu.
- Funkcję można wyłączyć bez wpływu na podstawowy flow czatu.
