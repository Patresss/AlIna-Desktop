# Alinka — kompaktowy dymek zakończenia pracy

## Kontekst

Terminalne powiadomienia Alinki używają obecnie tej samej geometrii `468 × 184 px` co prośby o zgodę. Po ukryciu przycisków zostaje duża pusta powierzchnia, przez co komunikat „Gotowe” przypomina formularz albo pusty modal. Ciężki cień dodatkowo oddziela go od spokojnego stylu Calm Command Center. Jeżeli wątek nie ma jeszcze nazwy, w roli opisu pojawia się techniczny tytuł z datą, na przykład `2026-07-20 (16:41:47)`.

Projekt rozszerza specyfikacje `2026-07-17-alinka-mascot-notifications-design.md` oraz `2026-07-20-alinka-scoped-approval-themed-bubble-design.md`. Nie zmienia zachowania zgód zakresowych ani bezpiecznej, nieblokującej inicjalizacji Swing.

## Zatwierdzony wariant

Wybrany został wariant **A — Zwarty dymek**. Alinka pozostaje osobnym elementem po lewej stronie, a ogon dymku nadal wskazuje maskotkę. Zmieniają się proporcje terminalnego powiadomienia, gęstość treści i siła dekoracji.

### Powodzenie

Powiadomienie `COMPLETE` używa okna około `388 × 118 px`:

- Alinka ma około `82 px` i stoi przy dolnej krawędzi;
- dymek zajmuje około `292 × 92 px`;
- ma promień około `17 px`, cienki semantyczny border i pojedynczy miękki cień;
- treść zawiera małą etykietę „AGENT SKOŃCZYŁ”, tytuł „Gotowe!” oraz opcjonalny opis;
- nie rezerwuje miejsca na przyciski, progress ani licznik kolejki.

### Błąd

Powiadomienie `ERROR` używa tego samego zwartego układu. Opis błędu może zająć maksymalnie dwie linie, dlatego okno może wzrosnąć do około `388 × 132 px`. Kolor stanu i komunikatu pozostaje semantycznym kolorem `danger`, ale powierzchnia i border nadal pochodzą z motywu AlIny.

### Zgoda

Powiadomienie `APPROVAL` zachowuje szerszą geometrię potrzebną dla dwóch albo trzech przycisków. Otrzymuje jednak ten sam lżejszy border, pojedynczy cień i relację maskotki do dymku, aby wszystkie stany wyglądały jak jeden komponent.

## Motyw

Kolory nadal wynikają z motywu wybranego w ustawieniach AlIny, a nie z wyglądu aplikacji znajdującej się pod popupem ani z systemowego trybu macOS. Jasny Calm Command Center daje jasny dymek również nad ciemną aplikacją. Ciemny motyw daje ciemny dymek. Obecny resolver i jego fallback pozostają źródłem `java.awt.Color`.

## Treść terminalna

Sensowna nazwa wątku pozostaje opcjonalnym opisem pod „Gotowe!”. Automatyczna nazwa sesji jest ukrywana, gdy po normalizacji odpowiada formatowi:

```text
yyyy-MM-dd (HH:mm:ss)
```

Filtr dotyczy wyłącznie opisu terminalnego popupu. Nie zmienia nazwy wątku w historii ani w karcie rozmowy. Jeżeli po odfiltrowaniu nie ma opisu, dymek zawiera tylko etykietę stanu i tytuł. Dla `ERROR` rzeczywisty komunikat błędu ma pierwszeństwo i nie jest filtrowany tym mechanizmem.

## Implementacja

`MascotPopup` dostaje dwa profile geometrii:

- profil `APPROVAL` dla szerokiego dymku z akcjami;
- profil terminalny dla `COMPLETE` i `ERROR`.

Profil ustala rozmiar okna, maskotki, powierzchni dymku, padding i maksymalną wysokość opisu. `MascotPopup` ustawia rozmiar `JWindow` przed wywołaniem `MascotScreenPositioner`, dzięki czemu prawy dolny narożnik pozostaje poprawny po przejściu między stanami o różnych wymiarach.

Rozpoznawanie automatycznego tytułu pozostaje małą, czystą funkcją bez zależności od Swing. Nie powstaje nowa warstwa zdarzeń ani osobny system layoutu. Kolejka, priorytety, ośmiosekundowy timeout, otwieranie rozmowy, animacja wejścia, kołysanie Alinki oraz brak przejmowania fokusu pozostają bez zmian.

## Błędy i przypadki brzegowe

- Pusty albo automatyczny tytuł nie tworzy pustego odstępu pod „Gotowe!”.
- Długi sensowny tytuł jest skracany lub zawijany w granicach dwóch linii, bez zwiększania okna ponad profil.
- Długi komunikat `ERROR` nie wypycha popupu poza ekran.
- Przejście z szerokiej zgody do kompaktowego wyniku ponownie wylicza pozycję.
- Brak palety lub assetu zachowuje dotychczasowy fallback.
- Zawieszony AWT nadal nie blokuje pokazania głównego okna JavaFX.

## Testy

Walidacja obejmuje:

- rozpoznanie automatycznego tytułu z datą i pozostawienie sensownej nazwy;
- brak opisu oraz pustego miejsca po odfiltrowaniu tytułu;
- wybór profilu `APPROVAL`, `COMPLETE` i `ERROR`;
- pozycjonowanie po zmianie rozmiaru popupu;
- regresję akcji zakresowych i nieblokującego startu;
- wizualny smoke test jasnego i ciemnego motywu;
- sprawdzenie, że terminalny dymek nie ma akcji ani badge'a;
- sprawdzenie, że popup nie przejmuje fokusu.

Pełna walidacja kończy się `./gradlew test` i `./gradlew build`.

## Kryteria akceptacji

- „Gotowe” nie wygląda jak pusty formularz i nie zawiera surowej daty sesji.
- Alinka i dymek tworzą jeden zwarty, czytelny komponent.
- Terminalny popup jest wyraźnie mniejszy od prośby o zgodę.
- Jasny oraz ciemny wariant odpowiadają motywowi AlIny.
- Zgody, błędy, timeout, pozycja nad Dockiem i brak przejmowania fokusu działają jak dotychczas.
