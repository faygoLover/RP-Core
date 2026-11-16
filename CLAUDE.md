# RP Core

Общая библиотека семейства RP-модов (`rpcore`): сама механик не ведёт, даёт общие механизмы и розетки, через
которые моды работают друг с другом. Forge 1.20.1, Java 17. Отвечать по-русски.

## Прежде чем что-то добавлять
1. `../_family/FAMILY_MAP.md`, раздел 3 «Куда это положить» — нет ли уже механизма или розетки.
2. `docs/MAP.md`, раздел 4 — рецепт для твоего случая (новая розетка, событие, механизм).
3. `docs/INDEX.md` — поиск по слову, нет ли уже такого класса.

В Core идёт только то, что нужно **двум и более модам**. Механика одного мода — в этот мод.
Новый класс, пакет, пакет сети, команда, ключ конфига или метод `RpCoreAPI` — обнови `docs/MAP.md` и
`docs/CHANGELOG.md` в том же коммите, каталог в `../_family/FAMILY_MAP.md` §4 — если добавил розетку или событие.

## Нельзя
- Ломать то, что уже вызывают моды: у метода `RpCoreAPI`, розетки и события нельзя менять подпись и смысл. Новое
  в розетке — только методом с ответом «не умею» по умолчанию (`default`), новое в `RpCoreAPI` — новым методом.
- Давать новому методу розетки имя, которое уже занято статическим методом в моде-поставщике (так было с
  `TraitProvider.grantKnowledge` и RP Perks): сначала поиск по семейству.
- Импортировать моды семейства (кроме FL Render — и то только в `client/effect`, `client/gm`).
- Менять id (`rpcore:hidden_plate`, `wall_holder`, `vacuum_holder`, `owner_tag`, `number_tag`, эффекты), имена
  данных мира (`rpcore_anchors`, `rpcore_marks`, `rpcore_character_wipes`), разделы игрока (`rpcore_profile`,
  `rpcore_taste`), ключи `rpcore-common.toml`, состав пакетов канала `rpcore:main`.
- Добавлять новое в `gm/`, `client/gm/`, `effect/`: это уедет в отдельный мод (план, T5.1).
- Push, выпуск, живой сервер — только по слову Алекса.

## Команды
```
gradlew.bat build                                        # jar в build/libs
python ../_family/family.py verify --tests "RP Core"    # компиляция, связи, игровые тесты (17)
python ../_family/family.py sync                         # разложить jar в _family/libs после сборки
python ../_family/tools/mapgen.py "RP Core"             # пересоздать docs/INDEX.md
```
JDK 17: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.14.7-hotspot"`. После пересборки с той же версией
удалить `~/.gradle/caches/forge_gradle/deobf_dependencies/blank/rpcore/<версия>*`, иначе соседи возьмут старый jar.

## Где что
- `docs/MAP.md` — устройство, потоки, рецепты. `docs/INDEX.md` — указатель кода (скрипт). `docs/CHANGELOG.md` — версии.
- `docs/00_plan.md` … `06_…` — история решений; что из них действует — `docs/MAP.md` §11.
- Общие правила работы: `../_handover/06_how_to_work.md`.
