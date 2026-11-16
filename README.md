

### Generatory, kity i sety
Dodatkowe funkcje są w folderze `extras/` jako osobny, współpracujący plugin Paper:
- `/kit 11` do `/kit 20` — miecze z Sharpness XI–XX i jedzenie.
- `/sety` — menu sześciu barwionych zestawów zbroi.
- `/generator set iron|gold|diamond [sekundy]` — generator dropiący surowiec; `/generator remove` i `/generator list`.

Zbuduj dodatek osobno w folderze `extras` poleceniem `mvn clean package`, a oba JAR-y włóż do `plugins/`. Po instalacji zrestartuj Paper. Enchanty XI–XX są celowo niestandardowo wysokie i wpływają mocno na balans.
