# Mała mapa SkyPvP — instalacja

Dodałem datapack generatora kompaktowej, pływającej areny: centralna platforma PvP, cztery małe wysepki, mostki, lampy i złote znaczniki przykładowych spawnów.

## Wymagania
Minecraft Java / Paper 1.21.4. To datapack do świata, nie plugin `.jar`.

## Instalacja na własnym serwerze
1. Pobierz repozytorium ZIP i rozpakuj je.
2. Skopiuj folder `map-datapack` do folderu świata serwera i zmień jego nazwę na `skypvp-map`.
   Przykład: `world/datapacks/skypvp-map/pack.mcmeta`.
3. Zatrzymaj i uruchom serwer albo wpisz `/reload` (restart jest bezpieczniejszy).
4. W grze wejdź w miejsce z dużą ilością wolnej przestrzeni, najlepiej wysoko nad ziemią. Stań dokładnie w miejscu, które ma być środkiem areny.
5. Wpisz `/function skypvp:small_map`.

Mapa buduje się względem miejsca, w którym stoisz. Generator nie usuwa istniejącego terenu, więc wybierz wolne miejsce. Zrób kopię świata przed testem. Wymiary są małe: mniej więcej 39 × 39 bloków.

Złote bloki są tylko znacznikami — ustaw spawny eventu osobno komendą `/event setspawn <nazwa>`, stojąc na wybranym miejscu. Plugin i datapack są oddzielnymi elementami.
