# Alinka — powrót do formy notyfikacji

## Decyzja

Obecny kompaktowy, jasny dymek zostaje wycofany. Użytkownik wskazał jako wzorzec pierwszą wersję popupu widoczną na zachowanym smoke teście `alinka-popup-smoke.png`: ciemną kartę typu toast z Alinką po lewej stronie. Ta decyzja zastępuje wizualne wymagania z późniejszej specyfikacji kompaktowego dymku.

## Wygląd

- Wszystkie trzy stany (`APPROVAL`, `COMPLETE`, `ERROR`) używają jednej geometrii `468 × 184 px`.
- Alinka ma rozmiar `116 px` i pozostaje częściowo poza powierzchnią karty, tak jak w pierwszym wariancie.
- Powierzchnia jest zawsze ciemnogranatowa, z jasnym tekstem, subtelnym obramowaniem i wyraźnym, miękkim cieniem. Nie zależy od motywu AlIny ani aplikacji znajdującej się pod popupem.
- Karta zachowuje niewielkie wskazanie w stronę maskotki, ale jest odbierana jako systemowa notyfikacja, nie jako duży biały dymek dialogowy.
- `COMPLETE` i `ERROR` używają pełnowymiarowych grafik maskotki. Automatyczny tytuł wątku w formacie daty nadal jest ukrywany; znaczący tytuł albo komunikat błędu pozostaje widoczny.

## Zachowanie bez zmian

Pozostają: akcje „Zezwól raz”, „Zezwól w sesji” lub „Zezwól zawsze” oraz „Odrzuć”, kolejka i badge, ośmiosekundowy timeout komunikatu terminalnego, kliknięcie otwierające rozmowę, brak przejmowania fokusu, pozycjonowanie na ekranie kursora i nieblokujący start Swing po zmianie monitorów.

## Implementacja

`MascotPopupLayout` ponownie udostępnia wspólny profil dla wszystkich typów. Domyślny `MascotPopup` korzysta z `MascotPalette.calmDark()`. Wstrzykiwany dostawca palety pozostaje dostępny dla izolowanych testów widoku, natomiast runtime nie rozwiązuje już palety z aktywnego motywu. Znika zestaw osobnych, kompaktowych ikon.

## Weryfikacja

- test profilu wspólnej geometrii;
- dotychczasowe testy treści terminalnej, zakresów zgód, kolejki i inicjalizacji;
- smoke test stanów `COMPLETE`, `ERROR` i `APPROVAL` w ciemnej karcie;
- potwierdzenie, że pokazanie popupu nie zmienia aktywnej aplikacji;
- `./gradlew test` i `./gradlew build`.

