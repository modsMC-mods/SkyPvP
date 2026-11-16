# SkyPvP Paper Plugin

Prosty, edytowalny plugin dla **własnego serwera Paper**: lobby, teleport do PvP, menu kolorowych kostiumów oraz eventy 1v1 i FFA.

> To autorski projekt inspirowany popularnym trybem SkyPvP, a nie kopia prywatnych plików, map, tekstur ani dokładnej implementacji MineStar.gg. Plugin nie tworzy map — zbuduj lobby/areny w świecie i ustaw na nich lokalizacje komendami.

## Wymagania
- Java 21
- Paper 1.21.4
- Maven (albo IntelliJ IDEA z obsługą Maven)

## Budowanie
W katalogu z `pom.xml` uruchom:

```bash
mvn clean package
```

Gotowy plugin: `target/skypvp-1.0.0.jar`.
Skopiuj go do folderu `plugins` serwera Paper i uruchom serwer. Konfiguracja powstanie w `plugins/SkyPvP/config.yml`.

## Pierwsze ustawienie
W grze, jako operator:

1. Stań w lobby i wpisz `/setspawn`.
2. Stań na punkcie wejścia na arenę PvP i wpisz `/setpvp`.
3. Zbuduj eventową arenę; ustaw spawny graczy, powtarzając `/event setspawn <nazwa>` w każdym miejscu.
4. Utwórz event: `/event create duel 1v1` albo `/event create ffa freeforall`.
5. Gracze zapisują się przez `/event join duel`; administrator uruchamia `/event start duel`.

## Komendy
- `/spawn` — teleport do lobby.
- `/setspawn` — ustaw lobby (admin).
- `/pvp` — teleport na PvP.
- `/setpvp` — ustaw punkt PvP (admin).
- `/kostiumy` — otwórz menu kostiumów w lobby; kostiumy są kosmetyczne i nie są przenoszone do eventu przy domyślnej konfiguracji.
- `/event create <nazwa> <1v1|ffa>` — utwórz event (admin).
- `/event setspawn <nazwa>` — dodaj spawn eventu (admin; powtórz, by dodać więcej).
- `/event join <nazwa>` — zapisz się.
- `/event leave` — wypisz się przed startem.
- `/event start <nazwa> [sekundy]` — odliczanie i start (admin).
- `/event stop` — zatrzymaj aktywny event (admin).
- `/event list` — pokaż eventy.

## Uwagi
- Lobby, punkt PvP i areny zapisują się w configu.
- 1v1 wymaga dokładnie dwóch zapisanych graczy; FFA wymaga minimum dwóch.
- Eventy korzystają z normalnych zasad walki i śmierci serwera. Skonfiguruj zestawy/ochronę świata osobno, jeśli ich potrzebujesz.
- Kostiumy to barwione skórzane zbroje. Plugin zapamiętuje i przywraca zbroję gracza przy zmianie/zdjęciu kostiumu i przed eventem. Do zaawansowanych modeli 3D potrzebny jest resource pack i osobna integracja.
- Testuj najpierw na prywatnym Paperze i rób kopię zapasową świata/configu przed instalacją.
