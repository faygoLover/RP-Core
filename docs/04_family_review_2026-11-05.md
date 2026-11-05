# Ревью семейства модов, 05.11.2026

Что смотрел: 22 мода (RP ×15, FL ×5, S5 ×3; без SovietEra, DeusEx, probe-проектов). Источники: `mods.toml`,
`build.gradle`, `gradle.properties`, импорты между пакетами `faygolover.*`, `ModList.isLoaded` и
`Class.forName`, цели миксинов. В игре ничего не запускал, код не менял.

## 1. Размеры

| Мод | Строк Java | Миксинов | Обязательные зависимости |
|---|---:|---:|---|
| RP Medicine 0.9.4 | 37 500 | 0 | rpcore |
| Zone Artifacts 0.1.41 | 28 900 | 0 | flrender |
| RP PDA 0.5.1 | 13 450 | 2 | rpcore |
| RP Culinary 0.8.5 | 9 140 | 3 | rpcore |
| RP Core 0.12.1 | 8 700 | 0 | **flrender** |
| RP Perks 2.8.0 | 6 670 | 0 | rpcore |
| RP Sports 0.1.9 | 5 290 | 2 | rpcore, rpstamina |
| flnettweaks 1.2.1 | 4 370 | 37 | — |
| FL Render 0.3.6 | 4 300 | 6 | — |
| RP Fixes 1.5.7 | 4 290 | **67** | — (мягко rpcore, flrender) |
| flperf 1.0.8 | 2 950 | 46 | — |
| RP Psyche 0.2.3 | 2 840 | 0 | rpcore, **rpmedicine** |
| RP Portals 0.2.6 | 2 820 | 0 | rpcore, flrender |
| RP Stamina 1.5.1 | 2 680 | 1 | rpcore |
| FL Tint 1.0.1 | 2 600 | 6 | — |
| RP Inventory 0.1.0 | 2 130 | 6 | rpcore |
| RP Needs 0.2.3 | 1 670 | 0 | rpcore |
| RP Time 1.1.0 | 1 520 | 5 | — (мягко rpcore) |
| s5adv 1.0.0 | 520 | 1 | — |
| S5 Glow Sticks 2.0.1 | 190 | 0 | flrender |
| RP Sky / RP No-Exp / s5lang | <200 | 0–1 | — |

## 2. Граф зависимостей

```
FL Render ◄── RP Core ◄── Medicine ◄── Psyche
    ▲            ▲   ▲◄── Culinary, Perks, PDA, Inventory, Needs, Stamina ◄── Sports
    │            │   ◄── Portals ──► FL Render (+ мягко Medicine)
    ├── Zone Artifacts
    └── S5 Glow Sticks
```

Основа здоровая: почти всё общение идёт через «розетки» Core (`BodyProvider`, `NutritionProvider`,
`StaminaProvider`, `TraitProvider`, события `Rp*Event`). Medicine, Culinary, Perks, Stamina друг о друге
напрямую почти не знают. Проблемы — в обходах этой схемы.

### Скрытые связи (обходят Core, в `mods.toml` не объявлены)

| Откуда → куда | Как | Где |
|---|---|---|
| Culinary → Psyche | `Class.forName("…rppsyche.server.PsycheService")` + `invoke` | `server/ExoticEffects.java:264,273` |
| Culinary → Medicine | `Class.forName("…rpmedicine.server.Medical")` | `server/ExoticEffects.java:289` |
| Culinary → Perks | `Class.forName("…rpperks.compat.CoreTraits")` | `server/Exotic.java:137` |
| Medicine → s5adv | `Class.forName("…s5adv.S5Advancements")` | `server/VanillaEffects.java:123` |
| Psyche → Zone Artifacts | compileOnly + `isLoaded` | `integration/ZaCompat.java` |
| Perks → Sports, Stamina → Perks | `isLoaded` | `SkillService.java:230` и др. |
| RP Fixes → Core (HUD), FL Render (рефлексия) | compileOnly на `libs/rpcore-0.8.6` | `fix/LsoThermometerHud.java`, `mixin/saflashlight/FlashlightShaderMixin.java:54` |

Строковая рефлексия ломается молча: переименовал класс или метод — компилятор промолчит, эффект
просто перестанет работать. Для настроения уже есть `RpMoodEvent`, для тела — `BodyProvider`, для
перков — `TraitProvider`: Culinary стоит перевести на них.

## 3. Найденные проблемы

### 3.1 Core обязательно тянет FL Render
Core использует FL Render только в 4 клиентских файлах: `client/effect/CoreTechniques`, `Interference`,
`RedEyesLayer`, `client/gm/GmOverlay`, то есть только для эффектов GM. Из-за этого **каждому** RP-моду
нужен FL Render с его миксинами на Embeddium и светом. Решение: сделать flrender мягкой зависимостью
Core (эффекты включать по `isLoaded`) или вынести GM-инструменты (п. 4.1) — тогда зависимость уйдёт
вместе с ними.

### 3.2 Psyche жёстко привязана к Medicine ради «лежачего»
Psyche обращается к Medicine только через `integration/MedicineCompat` и только в 4 местах
`PsycheService` (строки 132, 182, 204, 206): прочитать тело, `isDown`, `isClinical`. Portals тоже лезет в
Medicine ради `CarryService`/`MedCartEntity`. Состояние «лежачий/клиническая смерть/несут» нужно
нескольким модам, поэтому его место — розетка в Core (по образцу `BodyProvider`): Medicine её
наполняет, остальные читают. После этого Psyche можно делать с Medicine `mandatory=false`.

### 3.3 Моды собираются против старых версий друг друга
**Сделано 05.11:** `_family/` (versions.properties + libs + family.gradle + family.py), подключено в 15 модов;
`family.py check` — все 15 компилируются против текущих jar-ов. Подробности в `_family/README.md`.

Сейчас Core 0.12.1, FL Render 0.3.6, а `gradle.properties` указывают:

- rpcore: **0.5.2** (Sports), **0.7.0** (Inventory), 0.10.0 (PDA), 0.11.0 (×6), 0.11.1, 0.12.0;
- flrender: 0.1.0 (×3), 0.2.0; RP Needs отдельно прибит к flrender 0.3.1, medicine 0.9.3, perks 2.6.0;
- rpmedicine: 0.6.0 / 0.7.0 / 0.9.0 / 0.9.4; rpperks 2.0.0–2.5.0; rpstamina 1.4.0–1.4.2;
- RP Fixes вообще держит в `libs` копию `rpcore-0.8.6`.

Если в Core поменяется сигнатура, Sports соберётся против 0.5.2 без ошибок и упадёт в игре
`NoSuchMethodError`. Решение: один общий файл версий (например, `family.properties` рядом с репозиториями,
подключается через `apply from:`) или хотя бы скрипт «собрать всё против текущих jar», который
запускается перед «обнови моды».

### 3.4 Пересечения миксинов
| Цель | Кто | Риск |
|---|---|---|
| `ServerGamePacketListenerImpl` | RP Fixes (debug кликов), RP Inventory (`handleSetCarriedItem`), flnettweaks (`tick` ModifyConstant, disconnect), flperf (`tick` WrapOperation) | `tick` трогают двое; сейчас совместимо, но любое изменение в одном нужно проверять с другим |
| `ServerAdvancementManager.apply` | flperf (`@ModifyVariable`, фильтр), s5adv (`@Inject`, своя вкладка) | проверить, что фильтр flperf не выкидывает «Департамент» |
| MCreator | RP Fixes `mixin/mcreator` (sync переменных Mek/SCPO), flperf `mcreator/MCreatorTransformer` | фиксы одного мода в двух местах |
| `ClientLevel`, `ServerLevel` | FL Render + RP Time, RP Time + flperf | низкий |

### 3.5 RP Fixes превратился в «всё подряд»
67 миксинов против ~30 сторонних модов, плюс то, что фиксом не является: `PaintingsGmOnly`,
`LsoThermometerHud`, `TerralithIntro`, `SaWeaponPoses`, `AnomalyPostEffect`, отладка (`AlbumDebug`,
`SkinDebug`, `PlayerDebugLimits`, `mixin/debug*`). Отдельно трафик: `*Sync`-фиксы (SBW, CraftedCore,
ZeroContact, FarmCharm…) по смыслу пересекаются с flnettweaks.
Предложение по границам:
- **RP Fixes** — только чинит чужие моды;
- **flnettweaks** — транспорт и ванильная сеть; синхронизация конкретных модов остаётся в RP Fixes;
- **flperf** — производительность, сюда же все MCreator-фиксы;
- правила сервера S5 (картины только GM, вступление Terralith, термометр LSO) — в отдельный маленький
  `s5tweaks` или в s5adv, раз он уже серверный мод S5;
- отладочные миксины — за флагом в конфиге или только в dev-сборке.

### 3.6 Повторяющийся код
- `CuriosCompat` ×3 (Inventory, Medicine, PDA) — один помощник в Core;
- своя пара `Settings`/`Config` + `SyncPacket` для синхронизации настроек с клиентом в Culinary, Psyche,
  Stamina — общая база в Core («настройки с сервера на клиент»);
- 22 почти одинаковых `build.gradle` с флагами `-Pwith_*`. Переписывать не срочно, но общий
  `family.gradle` убрал бы и п. 3.3.

### 3.7 GameTest-ы лежат в `src/main`
Medicine (1 765 строк), Culinary, PDA, Inventory, Needs, Perks, Portals: тесты попадают в
готовые jar. Из-за тестов Needs и Portals вынуждены компилироваться против Medicine/Culinary.
Перенос в отдельный source set `gametest` уберёт и то и другое.

### 3.8 Время считается в трёх местах
Core `time/`, RP Time и `RpClock` + `RpTimeService` в Medicine (часы годности органов/крови).
Culinary для порчи берёт просто `getGameTime()` (`CookingService.java:372`), то есть при паузе RP-времени
еда портится, а органы — нет. `RpClock` стоит перенести в Core API и использовать в обоих модах.

## 4. Что можно вынести в отдельные моды

### 4.1 Из Core — GM-инструменты → «RP GM Tools» (сильная рекомендация)
`gm/` (15 файлов, 1 580 строк: держатели, скрытые таблички, маркеры, телепорт, якоря),
`client/gm`, эффекты red_eyes/interference. Это контент для ведущих, а не API. Core станет чистой
библиотекой (API + HUD + время + фото), перестанет требовать FL Render, а GM-мод можно не ставить
на серверах без ивентов. Portals, скорее всего, переедет в зависимость от GM Tools (проверить).

### 4.2 Из Medicine (самый большой, 37,5 тыс. строк)
- **Больница** → «RP Hospital»: `hospital/` (12 файлов, 2 000 строк: капельница, лампа, кровати,
  хранилище) + `client/geo`. Это блоки и модели, механике лечения они не нужны.
- **Лежачий/переноска** (`DownedService`, `CarryService`, `MedCartEntity`) — в розетку Core (п. 3.2),
  сама логика остаётся в Medicine.
- **Шутки и мелочи**: головокружение от вращения, `VanillaEffects` с достижением s5adv — отключаемые,
  хорошие кандидаты в аддон или хотя бы за флаги конфига.
- Интеграции (`integration/tacz`, `zc`, `lso`, `voice`) уже аккуратно изолированы, их трогать не надо.

### 4.3 Из Perks — фигура (H1, грудь на модели)
`figure/` + `figure/client` (10 файлов, ~1 000 строк) к перкам не относится. В Core уже есть
`BodyProfile`/`BodyNorms`: размер фигуры логично хранить там, а рендер оставить отдельным
косметическим модом (или внутри Perks, но в отдельном пакете с выключателем).

### 4.4 Из Zone Artifacts — не обязательно
Мод самодостаточный (зависит только от FL Render). Самый крупный кусок — тесла (`tesla/` +
`client/tesla`, ~4 100 строк). Выносить имеет смысл, только если теслу хотят ставить без остальных
аномалий. Стоит учесть, что «КПК» в ZA (`pda/`) — пульт настройки аномалий для GM, а не RP PDA. Названия
путаются.

### 4.5 Мелкие моды (Sky, No-Exp, Glow Sticks, s5lang) — оставить как есть
Они правильно отдельные: один мод — одна функция, легко выключить.

## 5. Порядок, если делать

1. Общий файл версий + сборка всех против текущих jar (п. 3.3) — дёшево, снимает главный риск.
2. Розетка «лежачий» в Core → Psyche без обязательной Medicine (п. 3.2); Culinary без `Class.forName` (п. 2).
3. GM Tools из Core (п. 4.1) → Core без FL Render (п. 3.1).
4. Разбор RP Fixes по границам (п. 3.5), MCreator — в flperf.
5. GameTest-ы в отдельный source set (п. 3.7), `RpClock` в Core (п. 3.8).
6. По желанию: RP Hospital, фигура из Perks.
