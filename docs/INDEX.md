# RP Core — указатель кода

Создано `python _family/tools/mapgen.py`, версия мода 0.14.1-1.20.1-forge.
**Руками не править**: пересоздаётся скриптом. Смысл, потоки работы и рецепты — в `docs/MAP.md`.
Разбор по шаблонам, без компиляции: необычно оформленная регистрация может отсутствовать в списках.

Всего: 107 файлов, 10703 строк Java.

## 1. Пакеты

| Пакет | Файлов | Строк |
|---|---:|---:|
| `faygolover.rpcore` | 2 | 192 |
| `faygolover.rpcore.access` | 2 | 151 |
| `faygolover.rpcore.api` | 16 | 1898 |
| `faygolover.rpcore.api.config` | 1 | 352 |
| `faygolover.rpcore.api.data` | 2 | 387 |
| `faygolover.rpcore.api.event` | 17 | 622 |
| `faygolover.rpcore.client` | 5 | 340 |
| `faygolover.rpcore.client.access` | 2 | 216 |
| `faygolover.rpcore.client.effect` | 6 | 448 |
| `faygolover.rpcore.client.gm` | 5 | 551 |
| `faygolover.rpcore.client.hud` | 6 | 493 |
| `faygolover.rpcore.client.photo` | 3 | 289 |
| `faygolover.rpcore.command` | 3 | 570 |
| `faygolover.rpcore.data` | 1 | 202 |
| `faygolover.rpcore.effect` | 2 | 187 |
| `faygolover.rpcore.gametest` | 3 | 609 |
| `faygolover.rpcore.gm` | 15 | 1621 |
| `faygolover.rpcore.network` | 8 | 553 |
| `faygolover.rpcore.photo` | 2 | 386 |
| `faygolover.rpcore.pose` | 3 | 360 |
| `faygolover.rpcore.profile` | 2 | 188 |
| `faygolover.rpcore.time` | 1 | 88 |

## 2. Классы

### `faygolover.rpcore`
- `CoreConfig`, 65 — Общий конфиг RP Core: config/rpcore-common.toml.
- `RpCore`, 127 — RP Core — общая библиотека семейства RP-модов (RP Perks, RP Stamina, RP Medicine, RP Culinary, RP PDA).

### `faygolover.rpcore.access`
- `AccessServer`, 51 — Выдача доступа к скрытым предметам с сервера: ведущим (уровень прав из [access] конфига Core) на выделенном сервере.
- `RpAccess`, 100 — Замок семейства RP (Core 0.6.0, только локальная сборка).

### `faygolover.rpcore.api`
- `BodyNorms`, 99 — Общие формулы тела, чтобы все моды семейства считали одинаково (дизайн — RP Stamina Sports, docs/06 п.
- `BodyProfile` (record), 75 — Анкета тела: пол, возраст, рост.
- `BodyProvider` (interface), 150 — Мод, который ведёт тело игрока (сейчас — RP Medicine).
- `Consumables`, 163 — Общая база того, что пьют, едят и курят (датапак rpcore/consumables): вода, кофеин, алкоголь, никотин.
- `FoodTaste` (record), 68 — Вкусы персонажа (часть анкеты): любимое и нелюбимое — вид еды («из чего») или способ («как приготовлено»).
- `InventoryModifier` (interface), 42 — Источник поправок к инвентарю: особенности (RP Perks), тело (RP Medicine) и т.п.
- `InventoryProvider` (interface), 82 — Мод ограничения инвентаря (RP Inventory).
- `NutritionProvider` (interface), 84 — Мод питания (RP Culinary): сытость в калориях, вода, нутриенты, вес и жир.
- `Photos`, 39 — Фото игроков — сервер (медкарта RP Medicine, аватарки RP PDA).
- `Pictures`, 48 — Картинки по ключу — сервер (свои аватарки RP PDA, в том числе у NPC).
- `RpCoreAPI`, 603 — Точка встречи модов семейства RP.
- `RpIds`, 230 — Общие названия навыков и черт.
- `SleepProvider` (interface), 17 — Мод, который ведёт сон (RP Needs).
- `StaminaProvider` (interface), 66 — Мод выносливости (RP Stamina).
- `StatusLines`, 68 — Строки состояния от модов семейства для мода тела (RP Medicine): что чувствует сам игрок, что видно при осмотре другим и что видит ГМ сканером и в панели.
- `TraitProvider` (interface), 64 — Источник навыков и черт игрока.

### `faygolover.rpcore.api.config`
- `SettingsBinder`, 352 — Привязка полей класса настроек к конфигу Forge: одно поле — один ключ в toml.

### `faygolover.rpcore.api.data`
- `PersistentTag`, 102 — Простые данные игрока в виде тега: раздел в getPersistentData() игрока (ForgeData в его файле).
- `PlayerStore`, 285 — Данные игрока с общим жизненным циклом: хранение вместе с игроком, копия при смерти и возврате из Края, отправка клиенту при входе, возрождении и смене измерения, уборка при выходе.

### `faygolover.rpcore.api.event`
- `BodyProfileChangedEvent`, 31 — Анкета тела игрока изменилась (игрок заполнил её или ГМ поправил).
- `ExertionEvent`, 23 — Физическая нагрузка (сервер): RP Stamina сообщает потраченную выносливость, мод питания считает по ней расход калорий.
- `FoodEatenEvent`, 56 — Игрок съел или выпил (сервер).
- `InventoryCapacityChangedEvent`, 18 — Ёмкость инвентаря игрока изменилась (открылись, закрылись, заморозились или оттаяли ячейки).
- `PhotoChangedEvent`, 17 — Сервер принял новое фото игрока (faygolover.rpcore.api.Photos).
- `PictureChangedEvent`, 33 — Сервер принял новую картинку по ключу или картинку удалили (faygolover.rpcore.api.Pictures).
- `RpAlarmEvent`, 36 — Будильник (или другой громкий сигнал) в точке: моды сна будят спящих в радиусе.
- `RpCharacterWipeEvent`, 16 — ГМ обнулил персонажа (сервер): каждый мод стирает своё, как у нового игрока.
- `RpConsumeEvent`, 38 — Игрок съел или выпил долю предмета (сервер): укус большого блюда, глоток напитка, последний кусок.
- `RpHarmEvent`, 31 — Игрок своей рукой довёл другого игрока до нокдауна или до клинической смерти и смерти (сервер).
- `RpMoodEvent`, 37 — С игроком случилось что-то, что влияет на настроение (сервер).
- `RpPostureSleepEvent`, 41 — Игрок пытается уснуть сидя или лёжа (/sleep).
- `RpPostureWakeEvent`, 32 — Игрок проснулся после сна сидя или лёжа (/sleep): сам, от удара, встал или вышел из игры.
- `RpSleepCountEvent`, 39 — Как считать игрока, когда решается, ускорять ли ночь (RP Time: ночь идёт быстрее, только когда спят все).
- `RpTimeSkipEvent`, 80 — Прокрутка времени RP для игрока (/rptime add|set|sleep|player, ускоренная ночь RP Time; сервер).
- `RpTimeSkipOfflineEvent`, 68 — Прокрутка времени RP для тех, кого нет в сети (/rptime sleep|skip …
- `StaminaRestoredEvent`, 26 — Выносливость восстановилась (сервер): RP Stamina сообщает, сколько вернулось за шаг — отдыхом, предметами, стимуляторами.

### `faygolover.rpcore.client`
- `CoreClient`, 62 — Клиент RP Core: общий слой HUD, команда /rphud — редактор HUD всех модов семейства, фото игроков, «ГМские штуки», пустая рука.
- `CoreClientConfig`, 39 — Клиентский конфиг RP Core: config/rpcore-client.toml — ключ ведущего, режим пустой руки, вид эффектов.
- `EmptyHand`, 66 — Пустая рука от первого лица.
- `PostureOverlay`, 41 — Сон сидя или лёжа (/sleep): экран плавно темнеет, внизу подсказка, как проснуться.
- `ShowLogins`, 132 — Логины рядом с РП-именами (Fakename) — только для админов (уровень прав 2+).

### `faygolover.rpcore.client.access`
- `ClientAccess`, 84 — Клиент замка семейства: ключ из config/rpcore-client.toml, фильтр вкладок креатива (вкладка без предметов сама не показывается, в поиске их тоже нет) и пересборка креатива и JEI, когда доступ меняется.
- `CoreJeiPlugin`, 132 — JEI для замка семейства: без доступа прячет предметы RP-модов и рецепты с ними, с доступом возвращает.

### `faygolover.rpcore.client.effect`
- `ClientEffects`, 56 — Клиент: эффекты RP Core на сущностях вокруг (их присылает сервер — EffectSync), слой глаз, помехи.
- `CoreTechniques`, 19 — Техники RP Core во вкладке FL Render (подписи — в lang: flrender.technique.rpcore.
- `EyeRenderTypes`, 58 — Типы отрисовки «Красных глаз»: как у глаз паука и светящихся сущностей, но чуть «подтянутые» к камере (вид сжимается к камере на 2 %: на экране всё на месте, а по глубине — ближе на 0,06 блока в 3 блоках).
- `Interference`, 76 — «Помехи»: у всех, кто рядом с носителем, экран как у камеры в зоне радиации — частицы засвечивают случайные точки и короткие чёрточки, лёгкое зерно; плюс шипение и щелчки счётчика Гейгера (InterferenceSound).
- `InterferenceSound`, 75 — Звук «Помех»: шипение помех (громче ближе к носителю) и щелчки счётчика Гейгера (чаще ближе: от пары в секунду до треска).
- `RedEyesLayer`, 164 — «Красные глаза».

### `faygolover.rpcore.client.gm`
- `GmClient`, 58 — Клиент «ГМских штук» RP Core: отрисовка держателей, подсветка ведущему, окна настроек пластины и держателей.
- `GmOverlay`, 114 — Подсветка ведущему: с меткой в руке (GmTools#MARKERS) видны контуры всех «ГМских штук» в радиусе 48 и подписи (имя пластины или портала, предмет держателя).
- `GmSchema`, 106 — Описание окна настроек «ГМской штуки»: поля (выбор, флажок, текст, число) и кнопки-помощники.
- `GmSettingsScreen`, 172 — Окно настроек «ГМской штуки» по её схеме (GmSchema): поля в два столбца (подпись — значение), колесо мыши прокручивает длинный список, внизу — кнопки-помощники схемы, «Готово» и «Отмена».
- `HolderRenderer`, 101 — Предмет в держателе.

### `faygolover.rpcore.client.hud`
- `Anchor` (enum), 28 — Якорь элемента HUD: угол или середина экрана, от которой отсчитывается смещение.
- `HudEditorScreen`, 161 — Один редактор HUD для всех модов семейства (раньше у RP Stamina и RP Medicine были свои копии): перетаскивание мышью, колесо — масштаб (с Shift — на 1 пиксель экрана), ПКМ — скрыть или показать, Shift+ПКМ — скрыть или показать тол…
- `HudElement` (interface), 55 — Элемент общего HUD.
- `HudLayout`, 125 — Раскладка HUD игрока: config/rpcore-hud.json.
- `HudRegistry`, 46 — Все элементы HUD семейства RP.
- `VanillaArmor`, 78 — Ванильные единицы брони (02.11, Алекс): у нас броня по зонам, поэтому полоска скрыта по умолчанию.

### `faygolover.rpcore.client.photo`
- `ClientPhotos`, 110 — Фото игроков на клиенте (медкарта, аватарки КПК).
- `ClientPictures`, 111 — Картинки по ключу на клиенте (faygolover.rpcore.api.Pictures).
- `SkinCapture`, 68 — Снимок своего скина по запросу сервера (как фото медкарты RP Medicine): ждём, пока скин догрузится (с Ely.by он приходит не сразу), снимаем текстуру с видеокарты в PNG и отправляем.

### `faygolover.rpcore.command`
- `CoreCommand`, 251 — /rpcore — что из семейства установлено и кто ведёт навыки; /rpcore commands [мод] — общий список команд семейства; /rpcore skill — уровень и прогресс навыка, ГМ ставит уровень или прибавляет add 1 | add 0.1 поверх наработанного (е…
- `PointCommand`, 84 — /rpcore point — именованные точки прибытия без блока: ведущий встаёт туда, куда переносить, смотрит, куда должны смотреть прибывшие, и пишет /rpcore point set .
- `TimeCommand`, 235 — /rptime — время RP (решения 30.10).

### `faygolover.rpcore.data`
- `PlayerStores`, 202 — Общий жизненный цикл данных игрока: все PlayerStore и PersistentTag семейства.

### `faygolover.rpcore.effect`
- `EffectSync`, 99 — Сервер: кто под «Красными глазами» и «Помехами» — рассылается всем, кто видит сущность (ванилла сообщает эффекты только самому игроку).
- `RpEffects`, 88 — Эффекты RP Core (0.8.1) и зелья к ним.

### `faygolover.rpcore.gametest`
- `CoreTestData`, 85 — Тестовые данные игрока для игровых тестов RP Core.
- `PlayerStoreTests`, 220 — Данные игрока с общим жизненным циклом (PlayerStore, PersistentTag): имя ветки в сохранении, копия при смерти и возврате из Края, повторная отправка клиенту, уборка при выходе, версия формата.
- `SettingsBinderTests`, 304 — Привязка настроек к конфигу (SettingsBinder).

### `faygolover.rpcore.gm`
- `GmBlock` (interface), 13 — Блок «ГМской штуки» (пластина, держатели, порталы RP Portals): игрокам без метки в руке не рисуется рамка выделения, ведущему с меткой — рисуется контур (подсветка).
- `GmConfigurable` (interface), 33 — Блок-сущность «ГМской штуки» с настройками: окно ведущего получает их с сервера (#writeSettings), возвращает изменённые (#applyFrom).
- `GmMarkerSet`, 35 — Клиент: загруженные блок-сущности «ГМских штук» (пластины, держатели, порталы RP Portals).
- `GmRegistry`, 91 — Блоки и предметы RP Core: «ГМские штуки» (невидимая пластина, настенный и сферический держатели) и общая вкладка креатива «RP: разное» (#MISC_TAB_KEY) — в неё кладут свои немногие предметы и другие моды семейства.
- `GmSettings`, 32 — Окно настроек «ГМских штук» — серверная сторона: открыть ведущему (ПКМ меткой по блоку) и применить присланное.
- `GmTeleport`, 116 — Перенос сущности для пластин и порталов.
- `GmTools`, 54 — Общее для «ГМских штук» (невидимая пластина, держатели, порталы RP Portals): кто ведущий и что значит «держит предмет-метку».
- `HiddenPlateBlock`, 144 — Невидимая нажимная пластина: без модели, коллизии, звука шагов и рамки выделения.
- `HiddenPlateBlockEntity`, 370 — Невидимая пластина.
- `HolderBlock`, 138 — Держатель предмета: сам невидим (предмет рисует HolderRenderer), не ломается ничем, кроме ведущего, не сдвигается поршнем.
- `HolderBlockEntity`, 201 — Держатель предмета (настенный и «сферический в вакууме»): сам предмет со всеми данными и то, как его показывать.
- `ItemMarks`, 177 — Метки на предметах (решения Alex 26.10): номер («№ 000123», счётчик на мир) и владелец (РП-имя на момент выдачи).
- `RpAnchors`, 97 — Именованные точки мира: пластина или портал с именем записывает сюда, куда к нему прибывать.
- `VacuumHolderBlock`, 26 — Сферический держатель в вакууме: предмет висит в центре блока, опора не нужна.
- `WallHolderBlock`, 94 — Настенный держатель — как рамка: предмет прижат к грани блока-опоры (#FACING — куда смотрит предмет, опора сзади).

### `faygolover.rpcore.network`
- `AccessPackets`, 31 — Пакеты замка семейства: сервер сообщает, выдан ли игроку доступ ведущего.
- `CoreNetwork`, 81 — Свой сетевой канал RP Core (rpcore:main).
- `EffectPackets`, 31 — Эффекты RP Core на сущности (маска EffectSync) — всем, кто её видит.
- `GmPackets`, 55 — Окно настроек «ГМской штуки»: сервер присылает текущие настройки, клиент возвращает изменённые.
- `PhotoPackets`, 104 — Пакеты службы фото: сервер просит снимок, клиент присылает; клиенты спрашивают чужие фото.
- `PicturePackets`, 90 — Пакеты картинок по ключу: клиент присылает (по разрешению сервера) и спрашивает; сервер раздаёт.
- `PosturePackets`, 53 — Сон сидя или лёжа: самому игроку — затемнить экран или снять затемнение (0.11.0).
- `ProfilePackets`, 108 — Пакеты анкеты тела: сервер присылает игроку его анкету; игрок один раз присылает заполненную.

### `faygolover.rpcore.photo`
- `PhotoServer`, 191 — Служба фото на сервере (ревью 11.10, п.
- `PictureServer`, 195 — Картинки по ключу (свои аватарки КПК, в том числе у NPC): в отличие от фото, не привязаны к игроку.

### `faygolover.rpcore.pose`
- `EmoteImpl`, 41 — Вызовы Emotecraft.
- `PostureView`, 27 — Клиент: какая поза у игроков вокруг (для хитбокса).
- `Postures`, 292 — Позы и сон сидя/лёжа (сервер): /sit — сесть на землю, /lay — лечь на спину, /sleep — уснуть, когда сидишь (и на сиденье мода) или лежишь.

### `faygolover.rpcore.profile`
- `CharacterWipe`, 81 — Вайп персонажа по слову ГМа: анкета, вкусы и данные всех модов (через RpCharacterWipeEvent).
- `ProfileServer`, 107 — Анкета тела: хранится в постоянных данных игрока (PlayerPersisted — переживает смерть), своему клиенту уходит копия.

### `faygolover.rpcore.time`
- `RpTime`, 88 — Время RP (/rptime, итоговый дизайн Medicine §6.2).

Классов без описания: 0 из 107.

## 3. Подписки на события

| Событие | Кто слушает |
|---|---|
| `AttachCapabilitiesEvent` | `PlayerStores.onAttach` |
| `BuildCreativeModeTabContentsEvent` | `ClientAccess.onBuildTab` |
| `ClientChatReceivedEvent` | `ShowLogins.onChat` |
| `EntityJoinLevelEvent` | `EffectSync.onJoin` |
| `EntityRenderersEvent.AddLayers` | `ClientEffects.onLayers` |
| `EntityRenderersEvent.RegisterRenderers` | `GmClient.onRenderers` |
| `FMLClientSetupEvent` | `GmClient.onSetup` |
| `FMLCommonSetupEvent` | `RpCore.commonSetup` |
| `ItemTooltipEvent` | `ItemMarks.onTooltip` |
| `LivingAttackEvent` | `GmTeleport.onAttack` |
| `LivingHurtEvent` | `Postures.onHurt` |
| `MobEffectEvent.Added` | `EffectSync.onAdded` |
| `MobEffectEvent.Expired` | `EffectSync.onExpired` |
| `MobEffectEvent.Remove` | `EffectSync.onRemove` |
| `ModConfigEvent.Loading` | `ClientAccess.onConfig` |
| `ModConfigEvent.Reloading` | `ClientAccess.onConfigReload` |
| `PlayerEvent.Clone` | `PlayerStores.onClone` |
| `PlayerEvent.PlayerChangedDimensionEvent` | `PlayerStores.onDimension`, `Postures.onDimension`, `ProfileServer.onDimension` |
| `PlayerEvent.PlayerLoggedInEvent` | `AccessServer.onLogin`, `CharacterWipe.onLogin`, `PhotoServer.onLogin`, `PlayerStores.onLogin`, `ProfileServer.onLogin` |
| `PlayerEvent.PlayerLoggedOutEvent` | `AccessServer.onLogout`, `PhotoServer.onLogout`, `PictureServer.onLogout`, `PlayerStores.onLogout`, `Postures.onLogout` |
| `PlayerEvent.PlayerRespawnEvent` | `PlayerStores.onRespawn`, `Postures.onRespawn`, `ProfileServer.onRespawn` |
| `PlayerEvent.StartTracking` | `EffectSync.onStartTracking`, `Postures.onStartTracking` |
| `RegisterCapabilitiesEvent` | `PlayerStores.onRegisterCaps` |
| `RegisterClientCommandsEvent` | `CoreClient.onClientCommands` |
| `RegisterCommandsEvent` | `CoreCommand.register`, `ItemMarks.onCommands`, `PointCommand.register`, `Postures.register`, `TimeCommand.register` |
| `RegisterGuiOverlaysEvent` | `CoreClient.onOverlays` |
| `RegisterKeyMappingsEvent` | `EmptyHand.onKeys`, `ShowLogins.onKeys` |
| `RenderGuiOverlayEvent.Pre` | `VanillaArmor.onOverlay` |
| `RenderHandEvent` | `EmptyHand.onRenderHand` |
| `RenderHighlightEvent.Block` | `GmOverlay.onHighlight` |
| `RenderLevelStageEvent` | `GmOverlay.onRenderLabels`, `GmOverlay.onRenderLevel`, `Interference.onRenderLevel` |
| `RenderNameTagEvent` | `ShowLogins.onNameTag` |
| `RpSleepCountEvent` | `Postures.onSleepCount` |
| `ServerStartingEvent` | `RpTime.onServerStarting` |
| `ServerStoppedEvent` | `AccessServer.onServerStopped`, `PhotoServer.onServerStopped`, `PictureServer.onServerStopped` |
| `TickEvent.ClientTickEvent` | `EmptyHand.onClientTick`, `Interference.onClientTick`, `ShowLogins.onClientTick`, `SkinCapture.onClientTick` |
| `TickEvent.ServerTickEvent` | `AccessServer.onServerTick`, `EffectSync.onServerTick`, `Postures.onServerTick` |
| `net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut` | `CoreClient.onLogout` |
| `net.minecraftforge.event.entity.EntityEvent.Size` | `Postures.onSize` |

## 4. Сеть

Канал: `faygolover/rpcore/network/CoreNetwork.java`, имя `main`, версия протокола `не объявлена константой`.
- `PhotoPackets.Request` (направление не указано в регистрации)
- `PhotoPackets.Upload` (направление не указано в регистрации)
- `PhotoPackets.Query` (направление не указано в регистрации)
- `PhotoPackets.Data` (направление не указано в регистрации)
- `PhotoPackets.Changed` (направление не указано в регистрации)
- `PicturePackets.Upload` (направление не указано в регистрации)
- `PicturePackets.Query` (направление не указано в регистрации)
- `PicturePackets.Data` (направление не указано в регистрации)
- `PicturePackets.Changed` (направление не указано в регистрации)
- `ProfilePackets.Sync` (направление не указано в регистрации)
- `ProfilePackets.Submit` (направление не указано в регистрации)
- `ProfilePackets.TasteSync` (направление не указано в регистрации)
- `ProfilePackets.TasteSubmit` (направление не указано в регистрации)
- `AccessPackets.Grant` (направление не указано в регистрации)
- `GmPackets.Open` (направление не указано в регистрации)
- `GmPackets.Apply` (направление не указано в регистрации)
- `EffectPackets.State` (направление не указано в регистрации)
- `PosturePackets.Sleep` (направление не указано в регистрации)
- `PosturePackets.Pose` (направление не указано в регистрации)

Классы с обработчиками пакетов (7): `AccessPackets`, `EffectPackets`, `GmPackets`, `PhotoPackets`, `PicturePackets`, `PosturePackets`, `ProfilePackets`.

## 5. Команды

- `faygolover/rpcore/client/CoreClient.java`: `rphud`
- `faygolover/rpcore/command/CoreCommand.java`: `rpcore` `commands` `skill` `add` `profile` `sex` `male` `female` `none` `age` `height` `reset` `wipe` `confirm` `taste`
- `faygolover/rpcore/command/PointCommand.java`: `rpcore` `point` `set` `remove` `tp` `list`
- `faygolover/rpcore/command/TimeCommand.java`: `rptime` `pause` `resume` `add` `set` `sleep` `player`
- `faygolover/rpcore/gm/ItemMarks.java`: `mark` `id` `clear` `owner`
- `faygolover/rpcore/pose/Postures.java`: `sit` `lay` `sleep`

## 6. Данные

- getPersistentData: `PersistentTag` ×5
- capability: `PlayerStore` (ключ задан в другом классе)
- свои файлы на диске: `HudLayout`
- capability: `CoreTestData`, ключ `rpcore:test_store`
- getPersistentData: `GmTeleport` ×4, теги: `rpcore_tp_until`, `rpcore_tp_safe_until`
- SavedData: `ItemMarks`, файл "rpcore_marks"
- SavedData: `RpAnchors`, файл "rpcore_anchors"
- свои файлы на диске: `PhotoServer`
- свои файлы на диске: `PictureServer`
- SavedData: `CharacterWipe`, файл "rpcore_character_wipes"
- getPersistentData: `ProfileServer` ×4, теги: `rpcore_profile`, `rpcore_taste`

## 7. Конфиги

Нет.

## 8. Что берёт из RP Core и что ему даёт

Методы `RpCoreAPI` (сколько вызовов): `CommandInfo` 2, `Posture` 15, `addSkill` 1, `body` 1, `bodyFatPercent` 1, `bodyState` 1, `commands` 1, `dayLengthSeconds` 1, `debugCommands` 1, `isSkillManaged` 1, `modules` 2, `nutrition` 1, `nutritionHandles` 1, `profile` 2, `registerCommand` 13, `registerModule` 1, `setProfile` 1, `setSkill` 1, `setTaste` 2, `skill` 1, `skillProgress` 1, `taste` 1, `weightKg` 1, `wipeCharacter` 1.

События Core: `BodyProfileChangedEvent`, `ExertionEvent`, `PhotoChangedEvent`, `PictureChangedEvent`, `RpAlarmEvent`, `RpCharacterWipeEvent`, `RpConsumeEvent`, `RpPostureSleepEvent`, `RpPostureWakeEvent`, `RpSleepCountEvent`, `RpTimeSkipEvent`, `RpTimeSkipOfflineEvent`, `StaminaRestoredEvent`.

## 9. Связи с другими модами

Проверки `isLoaded`: `emotecraft` (Postures).

Отражение (`Class.forName`):
- `RpCore` → `faygolover.rpcore.gametest.CoreTestData`

Пакеты других модов семейства в коде: `flrender` — 4 файлов.

## 10. Зарегистрированные id

- `faygolover/rpcore/effect/RpEffects.java` (5): `red_eyes` `interference` `long_red_eyes` `long_interference` `strong_interference`
- `faygolover/rpcore/gm/GmRegistry.java` (7): `hidden_plate` `wall_holder` `vacuum_holder` `owner_tag` `number_tag` `holder` `misc`
