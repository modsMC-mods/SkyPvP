# Dodatki SkyPvP: generatory, kity i sety

To jest osobny, niewielki plugin Paper uzupełniający główny plugin SkyPvP. Nie podmieniaj nim głównego JAR-a — zainstaluj oba pliki JAR w folderze `plugins`.

## Budowanie dodatku
Otwórz folder `extras` jako projekt Maven w IntelliJ albo uruchom terminal w folderze `extras`:

```bash
mvn clean package
```

Gotowy plik: `extras/target/skypvp-extras-1.0.0.jar`. Skopiuj go obok głównego SkyPvP JAR-a do `plugins/`, potem uruchom ponownie Paper.

## Komendy
- `/kit 11` ... `/kit 20` — miecz z Sharpness XI–XX oraz 16 pieczonych steków.
- `/sety` — menu z sześcioma zestawami barwionej skórzanej zbroi.
- `/generator set iron [sekundy]` — ustaw generator na bloku pod graczem.
- `/generator set gold [sekundy]`
- `/generator set diamond [sekundy]`
- `/generator remove` — usuń pobliski generator.
- `/generator list` — pokaż skonfigurowane generatory.

Domyślny czas generatora wynosi 10 sekund. Maksymalny czas można ustawić na 300 sekund. Dane generatorów zapisują się w configu dodatku `plugins/SkyPvPExtras/config.yml`.

Miecze 11–20 oznaczają w tej wersji poziom zaklęcia Sharpness od XI do XX. To niestandardowo wysokie enchanty i mogą bardzo mocno zmienić balans PvP. Używaj ich tylko na swoim serwerze i dostosuj poziomy do rozgrywki.
