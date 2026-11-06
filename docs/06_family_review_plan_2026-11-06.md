# План по оставшимся пунктам ревью семейства, 06.11.2026

Продолжение [04_family_review_2026-11-05.md](04_family_review_2026-11-05.md) §5. Пункт 1 (общие версии `_family`) сделан,
здесь пункты 2–6. Каждое утверждение ревью сверено с текущим кодом (локальные `main`/`master` на 06.11) и с
`_family/FACTS_2026-11-06.md`. Код не менял, в игре ничего не запускал.

## Коротко

| П. | Что | Ревью верно? | Предлагаю | Риск для живого | Объём | Нужен выбор Алекса |
|---|---|---|---|---|---|---|
| 2 | Розетка «лежачий» в Core, Psyche без Medicine, Culinary без `Class.forName` | Да, но одной розетки «лежачий» мало: Psyche берёт из Medicine ещё опьянение, ломку и антипсихотик. Portals трогать не нужно | Расширить розетки Core (тело, психика, знания), перевести на них Psyche, Culinary, Medicine, Perks | Сохранения и сеть не меняются. Пять модов выкладываются только вместе | ~1 день + проверка | Только `mandatory=false` или убрать зависимость |
| 3 | GM Tools из Core, Core без FL Render | Да. Но FL Render у Core уже только клиентский (`side="CLIENT"`), а на S5 он стоит у всех из-за ZA | **A:** FL Render необязателен для Core (5 файлов). **B:** отдельный мод — только по слову Алекса | A — нет. B — средний: блоки `rpcore:*` в мире, точки, сеть | A ~полдня, B 2–3 дня | Да: делать ли B |
| 4 | Разбор RP Fixes по границам | Да. Отладки уже нет (убрана 06.11), один фикс мёртвый (Sanity) | **A:** разложить по пакетам внутри RP Fixes, ключи конфига не трогать. **B:** новый мод `s5tweaks` | A — нет. B — низкий-средний: значения конфига | A 2–3 ч, B ~1 день | Да: A или B |
| 5 | GameTest в отдельный source set | Тесты в `src/main` — да. Что Portals из-за тестов зависит от Medicine — **неверно** | Закрыть: `exclude` уже решает главное. Source set не нужен | Нет | 0 | Согласиться закрыть |
| 6 | Фигура из Perks, Hospital | Да | Не трогать: H1 отложен Алексом, Hospital отложен ревью | — | — | Только вместе с решением по H1 |

## Общие условия

- **Не смешивать с текущим релиз-кандидатом** (`_handover/07_my_plan.md` §2). Эти правки — следующая партия после выкладки.
  Иначе в один релиз попадут и сетевые изменения Medicine/Portals, и новый API Core, и проверять придётся всё сразу.
- Один владелец на репозиторий. Сейчас параллельные сессии пишут документы по Psyche, Perks, Culinary, Medicine, RP Needs
  и план слоёв персонажа (`05_layers_and_time_plan_2026-11-06.md`). Код п. 2 начинать, когда они закончат. План слоёв
  совместим с п. 2: там «ломка в Medicine → Psyche читает её», это и есть новая розетка.
- После каждой партии: `python _family/family.py sync`, затем `check`, один прогон GameTest в конце, короткая проверка в
  игре только по плану, согласованному с Алексом.

---

## П. 2. Розетки Core: Psyche без обязательной Medicine, Culinary без отражения

### Сверка с кодом
- **Psyche → Medicine.** Подтверждено: всё идёт через `rppsyche/integration/MedicineCompat`, вызовы в
  `PsycheService.java:132, 182, 204, 206`. Но `MedicineCompat.read()` (строка 132) отдаёт не только «лежит»: боль,
  уровень `down` (обморок, нокдаун, клиническая смерть), опьянение, седацию, ломку (`Substances.anyWithdrawal`) и
  антипсихотик. Из этого боль и седация в Core уже есть (`BodyProvider.pain/sedation`, с 0.11.0, Medicine их отдаёт в
  `integration/CoreBody`). Не хватает: уровня `down`, опьянения, ломки, антипсихотика.
- `isDown`/`isClinical` принимают `Entity`, а не `Player`: ими проверяется и `BodyStubEntity` (тело вышедшего игрока).
  Методы `BodyProvider` принимают `Player`, поэтому новый метод должен принимать `Entity`.
- В `mods.toml` Psyche: `rpmedicine mandatory=true, [0.9.0,)`. В `build.gradle`: `implementation rpmedicine`.
- **Portals → Medicine.** Ревью говорит, что Portals «тоже лезет в Medicine ради лежачего». На деле Portals уже
  правильно мягкий: `mandatory=false`, все классы Medicine только во вложенном `MedicineCompat.Impl`, который грузится
  при `isLoaded`. Нужно ему не «лежачий», а каталка и волок (`grab`, `release`, `pushedCart`, `pickUp`) — это
  специфика Medicine, в Core ей не место. **Portals не трогать.**
- **Culinary.** Подтверждено три обхода через отражение, сейчас все работают (цели существуют, сигнатуры совпадают):
  - `ExoticEffects.Psyche.stress` → `PsycheService.impulse(ServerPlayer, double)`;
  - `ExoticEffects.Psyche.sanityHit` → `PsycheService.affected(Player)` + публичное поле `PsycheData.sanityHit`;
  - `ExoticEffects.Medicine.infect` → `Medical.state(Entity)` + публичные поля `MedicalState.sepsis`, `spoiledBloodSeconds`;
  - `Exotic.learn` → `rpperks.compat.CoreTraits.grantKnowledge(ServerPlayer, ResourceLocation)`, возвращает `int`.
  - Отражение в Mekanism (`ExoticEffects.Mekanism`) — чужой мод, его оставить.
- `RpMoodEvent` для Culinary не подходит: по его смыслу числа решает Psyche (`strength` — доля), а Culinary передаёт
  готовые величины из своей таблицы экзотики.
- Попутно: Medicine → s5adv (`VanillaEffects.spinAchievement`) — тоже отражение, но за `isLoaded("s5adv")` и с
  записью в лог. В п. 2 не входит, предлагаю оставить.

### Что менять
**RP Core 0.12.1 → 0.13.0** (только добавления, старое не ломается):
- `api/BodyProvider` — новые методы с ответом «не умею» по умолчанию:
  - `DownState down(Entity e)` — новый `api/DownState { NONE, FAINT, KNOCKDOWN, CLINICAL }`, по умолчанию `NONE`;
  - `double intoxication(Player)` 0–100, по умолчанию 0;
  - `boolean withdrawal(Player)`, по умолчанию false;
  - `double antipsychotic(Player)`, по умолчанию 0;
  - `boolean infect(Player, double sepsis, double hours)` — заражение от еды, по умолчанию false (вызывающий обходится сам).
- `RpCoreAPI.down(Entity)` и `RpCoreAPI.isDown(Entity)` — удобные обёртки.
- Новая розетка `api/MindProvider` (мод психики): `stress(ServerPlayer, double)`, `sanityHit(ServerPlayer, double)`.
  `RpCoreAPI.registerMind(...)`, `RpCoreAPI.stress(...)`, `RpCoreAPI.sanityHit(...)` — без Psyche ничего не делают.
  Существующий `registerSanity` оставить как есть.
- `TraitProvider`: `default int grantKnowledge(ServerPlayer, ResourceLocation)` → −1; `RpCoreAPI.grantKnowledge(...)`
  опрашивает зарегистрированных.

**RP Medicine 0.9.5 → 0.9.6:** `integration/CoreBody` реализует `down` (через `Medical.state` и `BodyStubEntity`),
`intoxication`, `withdrawal`, `antipsychotic`, `infect` (логика из отражения Culinary переезжает сюда как есть).
`rpcore` в `mods.toml` — `[0.13.0,)`.

**RP Psyche 0.2.3 → 0.3.0:** `MedicineCompat` → `BodyCompat` поверх `RpCoreAPI.body()`, ни одного импорта
`faygolover.rpmedicine`. `PsycheService` регистрирует `MindProvider` (`impulse` и прибавка к `sanityHit`).
`mods.toml`: `rpmedicine` — `mandatory=false` (или убрать строку, см. вопросы). `build.gradle`: убрать
`implementation rpmedicine`. `rpcore` — `[0.13.0,)`.

**RP Culinary 0.8.6 → 0.8.7:** `ExoticEffects.Psyche/Medicine` и `Exotic.learn` вызывают `RpCoreAPI`. Запасное
поведение то же: нет мода — ванильные эффекты (`WEAKNESS`, `HUNGER`), знание не выдаётся. `rpcore` — `[0.13.0,)`.

**RP Perks 2.8.0 → 2.8.1** (или 2.7.1, если в релиз пойдёт ветка без H1): поставщик черт реализует `grantKnowledge`
через существующий `CoreTraits.grantKnowledge`.

Порядок внутри: Core → `family.py sync` → Medicine, Perks → Psyche, Culinary → `family.py check`.

### Риск для живого сервера и сохранений
- Сохранения: новых полей и id нет. Сеть: пакеты не меняются.
- Пять модов связаны диапазонами `[0.13.0,)`: старый Core с новыми модами не загрузится, Forge скажет об этом явно.
  Выкладывать все пять разом.
- На S5 Medicine стоит, поэтому Psyche должна вести себя так же, как сейчас. Риск — тихая ошибка в чтении тела
  (например, лежачие рядом перестанут давить). Проверка: чтение кода и короткий тест в игре (нокдаун рядом поднимает
  стресс; паразиты из экзотики дают сепсис; изучение группы экзотики выдаёт знание). У Culinary есть GameTest-ы,
  на `infect` можно добавить один.
- Psyche без Medicine (не S5): нет боли, лежачих, ломки — меньше источников стресса, но мод работает.

### Объём
Около дня: Core ~120 строк, Medicine ~60, Psyche ~80, Culinary ~40, Perks ~10. Плюс `check`, GameTest, проверка.

### Можно без решений Алекса
Весь дизайн API и правки выше. Решение нужно только по строке зависимости и по сроку (см. вопросы).

---

## П. 3. GM-инструменты из Core, Core без FL Render

### Сверка с кодом
- Подтверждено: FL Render импортируют ровно 4 файла Core — `client/effect/CoreTechniques`, `Interference`
  (`PostFx`), `RedEyesLayer` (`FlRender.shadowPass/shaderPack`), `client/gm/GmOverlay.java:93` (`CameraPose.of`,
  это 3 строки: поворот камеры по X и Y).
- **Уточнение к ревью:** в `mods.toml` Core зависимость `flrender` уже `side="CLIENT"`, серверу FL Render не нужен.
  На S5 FL Render стоит у всех клиентов из-за ZA, Portals и Glow Sticks. Для S5 выгоды от развязки почти нет: это
  чистота Core для других сборок.
- Что лежит в мире и пострадает при переносе в другой мод:
  - блоки `rpcore:hidden_plate`, `rpcore:wall_holder`, `rpcore:vacuum_holder`; предметы `owner_tag`, `number_tag`;
    `BlockEntityType` `rpcore:hidden_plate`, `rpcore:holder`;
  - эффекты `rpcore:red_eyes`, `rpcore:interference`, 5 зелий, 2 звука;
  - данные мира `data/rpcore_anchors.dat` (именованные точки), `data/rpcore_marks.dat` (счётчик меток);
  - тег `rpcore:gm_markers`, схема настроек `GmSchema.register` (её использует Portals), пакеты `network/GmPackets`,
    команда `command/PointCommand`.
- Вкладка креатива `rpcore:misc` (`RpCoreAPI.MISC_TAB`) общая: туда кладут свои предметы FL Tint и Portals. Она
  остаётся в Core при любом варианте.
- Portals использует `rpcore.gm` в 6 файлах (подтверждено), больше никто.

### Вариант A — FL Render необязателен для Core (предлагаю по умолчанию)
- `mods.toml`: `flrender` → `mandatory=false`. `build.gradle`: FL Render остаётся для компиляции.
- Новый `client/effect/FlBridge` — единственный класс Core с импортами FL Render, вызывается только при
  `isLoaded("flrender")`.
- `ClientEffects.init`: `CoreTechniques.init()` и пост-эффект помех — только при FL Render.
- `RedEyesLayer`: `shadowPass()` и `shaderPack()` через мост; без FL Render — обычное свечение.
- `Interference`: без FL Render — только звук, без экранного шума.
- `GmOverlay`: свои 3 строки вместо `CameraPose.of`.
- Версия: войдёт в Core 0.13.0 вместе с п. 2 (тот же репозиторий).
- Риск: сохранения, сеть — нет. Визуально: красные глаза и помехи с FL Render должны выглядеть как сейчас — показать
  Алексу.
- Объём: около полудня, 5 файлов.

### Вариант B — отдельный мод «RP GM Tools» (только по слову Алекса)
- Переносится: `gm/`, `client/gm/`, `effect/`, `client/effect/`, `GmPackets`, `PointCommand`, модели, состояния
  блоков, строки перевода, звуки.
- Чтобы мир не потерял блоки, два пути:
  1. Новый мод регистрирует всё под старым пространством имён `rpcore`. Мир не замечает переезда. В логе Forge будет
     предупреждение о чужом префиксе, на работу оно не влияет.
  2. Новое пространство имён (`rpgm`) + `MissingMappingsEvent` с переназначением для блоков, предметов,
     `BlockEntityType`, эффектов, зелий и звуков. Чище, но если что-то пропустить, держатели и пластины на живом
     станут воздухом, а предметы пропадут.

  Предлагаю путь 1. Файлы данных `rpcore_anchors` и `rpcore_marks` — сохранить имена.
- Portals: 6 файлов на новый пакет и обязательная зависимость от GM Tools.
- Новый сетевой канал: сервер и клиенты обновляются вместе (сборка так и делает).
- Перед выкладкой: прогон на копии живого мира, проверить, что пластины, держатели и точки на месте.
- Объём 2–3 дня с проверкой. Риск средний.

### Нужен выбор Алекса
Делать ли B. По умолчанию — только A.

---

## П. 4. RP Fixes по границам

### Сверка с кодом
- Миксинов сейчас 63 в `rpfixes.mixins.json` (40 общих, 23 клиентских), классов в `mixin/` — 64. В ревью было 67:
  06.11 убраны отладочные `debug_skin` и `debug_album` (3 миксина).
- Отладки больше нет. `mixin/debuglimits` — это `PlayerDebugLimits`, правило сервера (игрокам без op не показывать
  хитбоксы и прочие подсказки), а не отладка.
- У всех правок, кроме одной, есть ключ в `FixesConfig`. Без ключа — яркость ХИС `glowsticks/*` (жёстко 8).
- **Мёртвый фикс:** `sanity_player_leak` (`LeakFixes`, `croissantnova.sanitydim`). Sanity: Descent Into Madness в
  сборке больше нет (заменён RP Psyche), но ключ стоит в `S5 Packs/layers/server/files/config/rpfixes-common.toml`.
  Заплатка ищет класс по имени и тихо выключается, вреда нет.
- Связи с семейством: `LsoThermometerHud` → HUD Core (`compileOnly` через `_family`, запасной `libs/rpcore-0.8.6`,
  проверка `isLoaded("rpcore")`) — нормально. `saflashlight/FlashlightShaderMixin.java:54` → FL Render через
  `Class.forName` — тот же тихий обход, что в Culinary.
- s5adv как дом для правил S5 **не подходит**: он только серверный (`displayTest="IGNORE_SERVER_VERSION"`), а
  термометр LSO, клавиши Effortless, ограничения отладки, Tips и ХИС работают на клиенте.

### Раскладка (по смыслу)
- **Фиксы чужих модов — остаются:** `taa_shooter_leak`, `emf_emote_mode`, `worldedit_lazy_states`, `safe_bends`,
  `lazy_bends`, `cuffed_emote_lock`, `hmi_auto_exclude`, `securitycraft_frame_fog`, `cnpc_keep_player_skin`,
  `cnpc_scheduler_shutdown`, `figura_inventory_scissor`, `anomaly_own_post_effect`, `breaching_keep_lights`,
  `agp_tags_after_join`, `sa_flashlight_occlusion`, `emote_resync`, `sa_crossbow_pose`, `sa_bow_pose_sync`,
  `ihs_chair_facing`, `carryon_lost_passenger`, `emote_config_retry`, `emote_no_client_repeat`.
- **Трафик и тики чужих модов — остаются в RP Fixes** (с ревью согласен, во flnettweaks не переносить):
  `scpo_tick_patches`, `craftedcore_sync_on_change`, `walkers_cooldown_on_change`, `remorphed_sync_on_change`,
  `mcreator_sync_on_change`, `mekanismexplosives_timer_local`, `csaugmentations_on_change`,
  `zerocontact_stamina_on_change`, `team_rejoin_noop`, `sbw_iff_needed_only`, `butchery_grinder_on_change`,
  `customnpcs_no_player_sounds`, `clanginghowl_quiet`, `farm_and_charm_saturation_on_change`.
- **Правила и вкус S5 — не фиксы:** `paintings_gm_only`, `terralith_keep_world_type`, `terralith_no_intro`,
  `effortless_quiet` (+ `EffortlessKeys`), `player_debug_limits`, `tips_pause_only`, `lso_thermometer_hud`,
  `sa_flashlight_soft`, яркость ХИС.
- **Убрать:** `sanity_player_leak` и строку в конфиге слоя `server` S5 Packs.

### Вариант A — внутри RP Fixes (предлагаю по умолчанию)
- Классы правил S5 → пакет `faygolover.rpfixes.s5` (и `mixin/s5/…`), поправить `rpfixes.mixins.json`.
- **Ключи и разделы конфига не трогать:** на живом может лежать свой `rpfixes-common.toml` с изменёнными значениями,
  а новый путь ключа сбросит их на значения по умолчанию.
- README: три раздела — фиксы, трафик, правила S5.
- Убрать мёртвый фикс Sanity.
- `FlashlightShaderMixin`: вместо `Class.forName` — `compileOnly` FL Render и вложенный класс, который грузится только
  при `isLoaded("flrender")` (как `MedicineCompat.Impl` в Portals).
- Версия RP Fixes 1.5.8 (если 1.5.7 уйдёт в текущий релиз) или 1.6.0.
- Риск: ошибка в `mixins.json` роняет запуск — ловится одной загрузкой. Сохранения и сеть не затронуты.
- Объём: 2–3 часа.

### Вариант B — новый мод `s5tweaks`
- Те же 9 правил в отдельном моде для обеих сторон, свой `s5tweaks-common.toml`.
- Перед переносом прочитать живой `config/rpfixes-common.toml` и перенести значения в слой сборки.
- В сборку добавляется мод, все клиенты обновляются.
- Объём около дня, риск низкий-средний (значения конфига).

### Нужен выбор Алекса
A или B. По умолчанию A. Удаление фикса Sanity — по умолчанию да.

---

## П. 5. GameTest-ы в отдельный source set

### Сверка с кодом
- GameTest в `src/main` у 7 модов — подтверждено: Medicine 5 файлов, PDA 2, Inventory 2, Culinary, Needs, Perks,
  Portals по одному. У других модов тестов нет.
- Во всех 7 `build.gradle` в `jar` стоит `exclude '<пакет>/gametest/**', 'data/<мод>/structures/**'` (Needs — с 06.11).
  06.11 проверено `unzip -Z1`: в jar-ах их нет.
- Основной код ни в одном моде не ссылается на классы тестов: они находятся только по `@GameTestHolder`. Поэтому
  исключение из jar безопасно, `NoClassDefFoundError` не будет.
- Утверждение ревью «из-за тестов Needs и Portals компилируются против Medicine/Culinary»:
  - Needs — верно, но это `compileOnly` (в jar не попадает, в `mods.toml` не обязательно);
  - Portals — **неверно**: `MedicineCompat.Impl` сам импортирует `CarryService` и `MedCartEntity`, Medicine нужна Portals
    для компиляции и без тестов.

### Что даст source set сверх `exclude`
- Needs соберётся без jar-ов Medicine и Culinary.
- Компилятор не даст основному коду сослаться на тестовый класс (сейчас такая ошибка всплыла бы только в игре).
- Цена: в 7 модах новый `sourceSet gametest`, `runs.gameTestServer` с `mods { source sourceSets.gametest }`, перенос
  `structures/` в ресурсы тестов, проверка, что `gameTestServer` и `family.py check` работают. Около часа на мод, риск
  сломать запуск тестов.

### Предложение
Закрыть пункт: главное (тесты не в релизных jar) уже сделано через `exclude`. Проверка jar-ов — в шагах релиза
(`_handover/07_my_plan.md` §2, шаг 3). Source set — только если появится реальная ошибка или Needs понадобится
собирать без соседей.

### Нужен выбор Алекса
Согласиться закрыть. По умолчанию — закрыт.

---

## П. 6. Фигура из Perks, RP Hospital

### Сверка с кодом
- Фигура: `rpperks/figure` + `figure/client`, 10 файлов. Хранится в данных мира `data/rpperks_figures.dat`
  (размер, выбран ли, пол), а не в `BodyProfile` Core (там пол, возраст, рост). Связи: анкета `PerkScreen` импортирует
  4 класса фигуры, `FigureEvents` знает id `rpmedicine:body_stub` (рисует фигуру на теле вышедшего).
- H1 Алекс отложил («не трогать без его слова»). Открыт вопрос релиза: Perks 2.7.0 без H1 или выключатель H1 в 2.8.x.
- Hospital: подтверждено, `rpmedicine.hospital` используют 28 файлов вне пакета. Выносить без слоя абстракции нельзя.

### Предложение
Ничего не делать. Если H1 пойдёт дальше: сначала серверный выключатель (он и так в плане релиза), потом при желании
отдельный косметический мод с прежним именем данных `rpperks_figures` (чтобы не переносить выбор игроков).
Hospital — отложен.

---

## Порядок

1. Дождаться выкладки текущего релиз-кандидата.
2. **П. 2 + п. 3 A** — один заход по Core 0.13.0 (общий репозиторий), затем Medicine, Perks, Psyche, Culinary.
3. **П. 4 A** — независимо, можно параллельно с п. 2: другой репозиторий (RP Fixes + строка в S5 Packs).
4. `family.py sync` + `check`, один прогон GameTest, план короткой проверки в игре — Алексу.
5. П. 3 B — только если Алекс скажет. П. 5 закрыт. П. 6 ждёт решения по H1.

## Вопросы Алексу (вариант по умолчанию — первый)

1. Делать эти пункты отдельной партией после текущего релиза? — **да** / вместе с релизом.
2. Psyche и Medicine: `mandatory=false` в `mods.toml` Psyche — **да** / убрать строку совсем.
3. GM-инструменты отдельным модом (п. 3 B)? — **нет, только FL Render необязателен (A)** / да, отдельный мод / ничего.
4. RP Fixes: **разложить внутри RP Fixes (A)** / новый мод `s5tweaks` (B).
5. Убрать мёртвый фикс Sanity и строку `sanity_player_leak` из конфига сборки? — **да** / оставить.
6. GameTest source set: **закрыть пункт** / всё же перенести.
7. Medicine → s5adv (достижение за кружение через отражение): **оставить** / перевести на ванильную выдачу по id.
