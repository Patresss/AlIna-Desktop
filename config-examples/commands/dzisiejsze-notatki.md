---
id: bd43d98d-d7d0-4a61-a1a2-039d5623d38d
name: "Dzisiejsze notatki"
description: "Zwraca krótki raport o notatkach utworzonych lub zmienionych dzisiaj"
icon: bi-calendar-day
state: ENABLED
showInChat: true
showInContextMenuPaste: false
showInContextMenuDisplay: true
---

Przygotuj krótki raport o dzisiejszych notatkach w vaultcie Obsidiana.

Instrukcje:
- ustal dzisiejsza date z kontekstu sesji
- znajdz notatki utworzone dzisiaj, zaktualizowane dzisiaj albo wyraznie odnoszace sie do dzisiejszej daty
- jesli podano argument, potraktuj go jako dodatkowy filtr katalogu, tagu albo tematu
- przeczytaj tylko potrzebne pliki
- zwroc odpowiedz po polsku

Format odpowiedzi:
- Dzis utworzone: lista plikow albo "brak"
- Dzis aktualizowane: lista plikow albo "brak"
- Najwazniejsze watki: 3-6 krotkich punktow
- Taski / follow-upy: lista albo "brak"

Jesli wyniki sa niejednoznaczne, napisz krotko jakie kryterium przyjales.

"""
$ARGUMENTS
"""
