package faygolover.rpcore.api;

import faygolover.rpcore.CoreConfig;
import faygolover.rpcore.RpCore;
import faygolover.rpcore.api.event.ExertionEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Точка встречи модов семейства RP. Каждый мод требует только RP Core; друг о друге моды узнают отсюда.
 * <ul>
 *   <li>Навыки и черты: источник регистрирует {@link TraitProvider}, остальные спрашивают {@link #skill}, {@link #hasTrait}.</li>
 *   <li>Модули: каждый мод семейства отмечается в {@link #registerModule}, чтобы {@code /rpcore} показал, что установлено.</li>
 *   <li>Тело ({@link BodyProvider}, RP Medicine), питание ({@link NutritionProvider}, RP Culinary) и выносливость
 *   ({@link StaminaProvider}, RP Stamina) — по одному моду. Второй такой мод первого не заменяет (в лог — оба).</li>
 *   <li>Нагрузка: {@link #exertion} — RP Stamina сообщает потраченную выносливость, питание считает калории.</li>
 *   <li>Команды: каждый мод сообщает свои в {@link #registerCommand}, {@code /rpcore commands} показывает все.</li>
 *   <li>Время RP ({@code /rptime}): {@link #timeRunning}, {@link #setTimeRunning}, события
 *   {@link faygolover.rpcore.api.event.RpTimeSkipEvent}; кто спит для ускорения ночи RP Time —
 *   {@link faygolover.rpcore.api.event.RpSleepCountEvent}.</li>
 *   <li>Фото игроков (портрет по скину) — {@link Photos}; картинки по ключу (свои аватарки, у NPC тоже) — {@link Pictures}.</li>
 *   <li>Анкета тела (пол, возраст, рост) — {@link #profile}; формулы тела — {@link BodyNorms}.</li>
 *   <li>Инвентарь: мод ограничения — {@link InventoryProvider} ({@link #slotOpen}, {@link #slotUsable}), поправки к
 *   нему — {@link InventoryModifier} ({@link #registerInventoryModifier}).</li>
 *   <li>«ГМские штуки» (0.8.0): общая вкладка креатива {@link #MISC_TAB}, метки ведущего — тег
 *   {@code rpcore:gm_markers} ({@link faygolover.rpcore.gm.GmTools}), блоки с подсветкой — {@link faygolover.rpcore.gm.GmBlock}
 *   и {@link faygolover.rpcore.gm.GmMarkerSet}, окно настроек — {@link faygolover.rpcore.gm.GmConfigurable},
 *   именованные точки прибытия — {@link faygolover.rpcore.gm.RpAnchors}, перенос — {@link faygolover.rpcore.gm.GmTeleport}.</li>
 * </ul>
 * Регистрировать — в FMLCommonSetupEvent (через enqueueWork).
 */
public final class RpCoreAPI {
    private RpCoreAPI() {}

    private static final List<TraitProvider> PROVIDERS = new CopyOnWriteArrayList<>();
    private static final Map<String, String> MODULES = Collections.synchronizedMap(new LinkedHashMap<>());

    // ------------------------------------------------------------------ модули

    /** Отметить мод семейства: id и что он даёт (для /rpcore). */
    public static void registerModule(String modId, String description) {
        MODULES.put(modId, description);
    }

    public static Map<String, String> modules() {
        synchronized (MODULES) {
            return Map.copyOf(MODULES);
        }
    }

    // ------------------------------------------------------------------ тело

    private static volatile BodyProvider body;

    /** Мод тела (RP Medicine). Один: второй не заменяет первого. */
    public static synchronized void registerBody(BodyProvider provider) {
        if (accept("тела", body, provider)) body = provider;
    }

    /** Ведёт ли мод тела эту часть физиологии (тогда простая версия в других модах уступает ему). */
    public static boolean bodyHandles(ResourceLocation feature) {
        BodyProvider b = body;
        return b != null && b.handles(feature);
    }

    /** Мод тела или null. */
    public static BodyProvider body() {
        return body;
    }

    /** Без сознания ли игрок (по моду тела; без него — всегда в сознании). */
    public static boolean unconscious(net.minecraft.world.entity.player.Player player) {
        BodyProvider b = body;
        return b != null && b.unconscious(player);
    }

    /** Руки работают (по моду тела; без него — всегда). КПК, еда, готовка, тренировки. @since 0.14.0 */
    public static boolean handsFree(Player player) {
        BodyProvider b = body;
        return b == null || b.handsFree(player);
    }

    // ------------------------------------------------------------------ питание

    private static volatile NutritionProvider nutrition;

    /** Мод питания (RP Culinary). Один: второй не заменяет первого. */
    public static synchronized void registerNutrition(NutritionProvider provider) {
        if (accept("питания", nutrition, provider)) nutrition = provider;
    }

    /** Мод питания или null. */
    public static NutritionProvider nutrition() {
        return nutrition;
    }

    /** Ведёт ли мод питания эту часть ({@link RpIds#FOOD_HUNGER}…): тогда своя простая версия уступает ему. */
    public static boolean nutritionHandles(ResourceLocation feature) {
        NutritionProvider n = nutrition;
        return n != null && n.handles(feature);
    }

    // ------------------------------------------------------------------ выносливость

    private static volatile StaminaProvider stamina;

    /** Мод выносливости (RP Stamina). Один: второй не заменяет первого. @since 0.4.0 */
    public static synchronized void registerStamina(StaminaProvider provider) {
        if (accept("выносливости", stamina, provider)) stamina = provider;
    }

    /** Мод выносливости или null — тогда выносливости нет. @since 0.4.0 */
    public static StaminaProvider stamina() {
        return stamina;
    }

    /** Атрибут характеристики выносливости для модификаторов или null (нет мода выносливости). @since 0.4.0 */
    public static Attribute staminaAttribute(StaminaProvider.Stat stat) {
        StaminaProvider s = stamina;
        return s == null ? null : s.attribute(stat);
    }

    // ------------------------------------------------------------------ инвентарь

    private static volatile InventoryProvider inventory;
    private static final Map<ResourceLocation, InventoryModifier> INVENTORY_MODIFIERS =
            Collections.synchronizedMap(new LinkedHashMap<>());

    /** Мод ограничения инвентаря (RP Inventory). Один: второй не заменяет первого. @since 0.7.0 */
    public static synchronized void registerInventory(InventoryProvider provider) {
        if (accept("инвентаря", inventory, provider)) inventory = provider;
    }

    /** Мод ограничения инвентаря или null — тогда открыто всё. @since 0.7.0 */
    public static InventoryProvider inventory() {
        return inventory;
    }

    /**
     * Есть ли у игрока ячейка {@code index} ванильного инвентаря (0–8 хотбар, 9–35 хранилище, 36–39 броня, 40 вторая
     * рука). Без мода ограничения — есть всегда. Замороженная ячейка — тоже есть. @since 0.7.0
     */
    public static boolean slotOpen(Player player, int index) {
        InventoryProvider p = inventory;
        return p == null || p.isSlotOpen(player, index);
    }

    /** Можно ли сейчас класть в ячейку и брать из неё: открыта и не заморожена. @since 0.7.0 */
    public static boolean slotUsable(Player player, int index) {
        InventoryProvider p = inventory;
        return p == null || (p.isSlotOpen(player, index) && !p.isSlotFrozen(player, index));
    }

    /**
     * Источник поправок к инвентарю (особенности, тело…). Тот же id второй раз — заменяет прежний. Регистрировать в
     * FMLCommonSetupEvent. @since 0.7.0
     */
    public static void registerInventoryModifier(ResourceLocation id, InventoryModifier modifier) {
        INVENTORY_MODIFIERS.put(id, modifier);
    }

    /** Все источники поправок (для мода ограничения). @since 0.7.0 */
    public static Map<ResourceLocation, InventoryModifier> inventoryModifiers() {
        synchronized (INVENTORY_MODIFIERS) {
            return Map.copyOf(INVENTORY_MODIFIERS);
        }
    }

    /** У источника поправок что-то поменялось для игрока — пересчитать в ближайший тик (сервер). @since 0.7.0 */
    public static void inventoryChanged(Player player) {
        InventoryProvider p = inventory;
        if (p != null && !player.level().isClientSide) p.refresh(player);
    }

    /** Второй мод на то же место: оставляем первого, в лог — оба имени. */
    private static boolean accept(String what, Object current, Object provider) {
        if (provider == null || current == provider) return false;
        if (current == null) return true;
        RpCore.LOGGER.error("RP Core: мод {} уже есть ({}), второй ({}) не подключён.", what,
                current.getClass().getName(), provider.getClass().getName());
        return false;
    }

    // ------------------------------------------------------------------ речь

    /** Как сказано: вслух рядом или сообщением (КПК, личка). @since 0.7.1 */
    public enum SpeechChannel { VOICE_NEARBY, MESSAGE }

    /**
     * Фильтр речи игрока: меняет то, что он говорит или пишет (заикание от стресса, бред). Возвращает новый текст или
     * тот же. Вызывается на сервере.
     * @since 0.7.1
     */
    @FunctionalInterface
    public interface SpeechFilter {
        String filter(net.minecraft.server.level.ServerPlayer player, String text, SpeechChannel channel);
    }

    private static final Map<ResourceLocation, SpeechFilter> SPEECH_FILTERS = Collections.synchronizedMap(new LinkedHashMap<>());

    /** Подключить фильтр речи (тот же id второй раз — заменяет прежний). Регистрировать в FMLCommonSetupEvent. @since 0.7.1 */
    public static void registerSpeechFilter(ResourceLocation id, SpeechFilter filter) {
        SPEECH_FILTERS.put(id, filter);
    }

    /**
     * Пропустить текст игрока через все фильтры речи по порядку регистрации. Зовёт мод, который доставляет речь (RP
     * PDA): перед тем как показать её другим. Только речь самого игрока — не системные сообщения и не ГМа. Сервер.
     * @since 0.7.1
     */
    public static String filterSpeech(net.minecraft.server.level.ServerPlayer player, String text, SpeechChannel channel) {
        if (text == null || text.isEmpty()) return text;
        List<SpeechFilter> filters;
        synchronized (SPEECH_FILTERS) {
            filters = List.copyOf(SPEECH_FILTERS.values());
        }
        String out = text;
        for (SpeechFilter f : filters) {
            try {
                String next = f.filter(player, out, channel);
                if (next != null && !next.isEmpty()) out = next;
            } catch (RuntimeException e) {
                RpCore.LOGGER.error("RP Core: фильтр речи упал", e);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ команды

    /**
     * Команда мода семейства для общего списка {@code /rpcore commands}.
     * @param syntax         как набирать, например {@code /rpstamina set <игроки> <число>}
     * @param descriptionKey ключ перевода с описанием
     * @param permission     уровень прав (0 — все, 2 — ГМ)
     * @param debug          отладочная: видна и работает, только если в конфиге Core включены отладочные команды
     */
    public record CommandInfo(String modId, String syntax, String descriptionKey, int permission, boolean debug) {}

    private static final List<CommandInfo> COMMANDS = new CopyOnWriteArrayList<>();

    /** Сообщить свою команду в общий список (регистрировать один раз, в FMLCommonSetupEvent). @since 0.4.0 */
    public static void registerCommand(String modId, String syntax, String descriptionKey, int permission, boolean debug) {
        COMMANDS.add(new CommandInfo(modId, syntax, descriptionKey, permission, debug));
    }

    public static List<CommandInfo> commands() {
        return List.copyOf(COMMANDS);
    }

    /**
     * Включены ли отладочные команды семейства ({@code config/rpcore-common.toml}, {@code commands.debug_commands}).
     * Проверять в {@code .requires(...)} отладочной ветки. @since 0.4.0
     */
    public static boolean debugCommands() {
        return CoreConfig.SPEC.isLoaded() && CoreConfig.DEBUG_COMMANDS.get();
    }

    // ------------------------------------------------------------------ время RP

    /**
     * Идёт ли время RP ({@code /rptime}). Пока не идёт — голод не тратится, раны не заживают, таймеры тела стоят.
     * Механика выключена в конфиге — время идёт всегда (как без неё). Сервер. @since 0.4.0
     */
    public static boolean timeRunning() {
        return faygolover.rpcore.time.RpTime.running();
    }

    /** Включена ли механика {@code /rptime start|stop} в конфиге Core. Сервер. @since 0.7.2 */
    public static boolean timeControlEnabled() {
        return faygolover.rpcore.time.RpTime.enabled();
    }

    /**
     * Пустить или остановить время RP, как {@code /rptime start|stop} (RP Time: {@code /time pause|resume}).
     * Механика выключена в конфиге — ничего не делает и возвращает {@code false}. Сервер. @since 0.7.2
     */
    public static boolean setTimeRunning(boolean running) {
        if (!faygolover.rpcore.time.RpTime.enabled()) return false;
        faygolover.rpcore.time.RpTime.setStarted(running);
        return true;
    }

    /**
     * Длина RP-суток в реальных секундах: у RP Time — его день + ночь (без временного {@code /time speed}), без него —
     * {@code [time] day_length_minutes} Core. От неё считают медленные механики (голод, жажда, бодрость). Сервер. @since 0.11.0
     */
    public static double dayLengthSeconds() {
        return faygolover.rpcore.time.RpTime.dayLengthSeconds();
    }

    /**
     * RP Time сообщает себя источником времени: пауза и длина суток дальше берутся у него, {@link #timeRunning} и
     * {@link #setTimeRunning} работают через него (своя механика Core {@code [rptime]} не нужна). @since 0.11.0
     */
    public static void registerTimeSource(java.util.function.BooleanSupplier running, java.util.function.Consumer<Boolean> setRunning,
                                          java.util.function.DoubleSupplier dayLengthSeconds) {
        faygolover.rpcore.time.RpTime.setSource(running, setRunning, dayLengthSeconds);
    }

    /**
     * То же, что {@link #registerTimeSource(java.util.function.BooleanSupplier, java.util.function.Consumer, java.util.function.DoubleSupplier)},
     * и скорость хода ({@code /time speed}, без паузы): от неё зависит {@link #bodyScale}. @since 0.13.0
     */
    public static void registerTimeSource(java.util.function.BooleanSupplier running, java.util.function.Consumer<Boolean> setRunning,
                                          java.util.function.DoubleSupplier dayLengthSeconds, java.util.function.DoubleSupplier speed) {
        faygolover.rpcore.time.RpTime.setSource(running, setRunning, dayLengthSeconds, speed);
    }

    /**
     * Время тела: сколько RP-секунд проходит за реальную секунду сейчас. Сутки ({@link #dayLengthSeconds}) с учётом
     * {@code /time speed} RP Time, на паузе — 0. При 3-часовых сутках — 8. Долгие процессы (вещества, заживление,
     * рассудок, мышцы, голод) идут по нему, быстрые (кровь, боль, действие лекарства) — по реальному времени. Вне сети
     * время тела не идёт. Сервер. @since 0.13.0
     */
    public static double bodyScale() {
        return faygolover.rpcore.time.RpTime.bodyScale();
    }

    /**
     * Эталонный ход времени тела: сутки RP за 3 реальных часа ({@link #bodyScale} = 8). Под него подобраны числа долгих
     * механик семейства (рассудок, восстановление мышц, рост навыков, заживление). Одно число на все моды — своё «8»
     * в коде мода не писать. @since 0.15.0
     */
    public static final double BODY_REFERENCE = 8.0;

    /**
     * Во сколько раз долгие процессы тела идут сейчас быстрее эталона ({@link #BODY_REFERENCE}): 1 — при 3-часовых
     * сутках, 0 — на паузе RP-времени. Умножай на это «секунду в сети» в долгой механике. Сервер. @since 0.15.0
     */
    public static double bodyRate() {
        return bodyScale() / BODY_REFERENCE;
    }

    /** {@link #bodyScale}, но без паузы: для пересчёта настроек и подсказок («через сколько реальных минут»). Сервер. @since 0.13.0 */
    public static double rpPerReal() {
        return faygolover.rpcore.time.RpTime.rpPerReal();
    }

    /** Реальные секунды в RP-часы по {@link #bodyScale} (на паузе — 0). Сервер. @since 0.13.0 */
    public static double rpHours(double realSeconds) {
        return realSeconds * bodyScale() / 3600.0;
    }

    /** RP-часы в реальные секунды при идущем времени ({@link #rpPerReal}). Сервер. @since 0.13.0 */
    public static double realSeconds(double rpHours) {
        double r = rpPerReal();
        return r > 0 ? rpHours * 3600.0 / r : Double.POSITIVE_INFINITY;
    }

    /**
     * Ниже какой доли шкалы прокрутка времени (и время вне сети) не опускает еду и воду ({@code [time] skip_floor},
     * по умолчанию 0.2). Сервер. @since 0.11.0
     */
    public static double skipFloor() {
        return CoreConfig.SPEC.isLoaded() ? CoreConfig.SKIP_FLOOR.get() : 0.2;
    }

    // ------------------------------------------------------------------ позы и сон сидя/лёжа

    /** Поза игрока: сидит на земле ({@code /sit}), лежит на спине ({@code /lay}), сидит на сиденье мода или стоит. @since 0.11.0 */
    public enum Posture { NONE, SIT, LAY, SEAT, HOSPITAL_BED }

    /**
     * Поза игрока сейчас (сервер). {@link Posture#SEAT} — едет на сиденье мода (стул, скамья),
     * {@link Posture#HOSPITAL_BED} — лежит на больничной койке (RP Medicine). @since 0.11.0
     */
    public static Posture posture(Player player) {
        return faygolover.rpcore.pose.Postures.posture(player);
    }

    /** Спит ли игрок сидя или лёжа ({@code /sleep}); кровати — ванильный {@code isSleeping()}. Сервер. @since 0.11.0 */
    public static boolean postureSleeping(Player player) {
        return faygolover.rpcore.pose.Postures.sleeping(player);
    }

    /** Будильник или другой громкий сигнал: моды сна будят спящих в радиусе ({@link faygolover.rpcore.api.event.RpAlarmEvent}). @since 0.11.0 */
    public static void alarm(net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 pos, double radius) {
        MinecraftForge.EVENT_BUS.post(new faygolover.rpcore.api.event.RpAlarmEvent(level, pos, radius));
    }

    private static volatile java.util.function.ToDoubleFunction<Player> sanity;

    /** RP Psyche сообщает, как читать рассудок (0–100). @since 0.11.0 */
    public static void registerSanity(java.util.function.ToDoubleFunction<Player> source) {
        sanity = source;
    }

    /** Рассудок 0–100; без мода психики — 100. Сервер. @since 0.11.0 */
    public static double sanity(Player player) {
        var s = sanity;
        return s == null ? 100 : s.applyAsDouble(player);
    }

    private static volatile java.util.function.ToDoubleFunction<Player> stress;

    /** RP Psyche сообщает, как читать стресс (0–100). @since 0.14.0 */
    public static void registerStress(java.util.function.ToDoubleFunction<Player> source) {
        stress = source;
    }

    /** Стресс 0–100; без мода психики — 0. Сервер. @since 0.14.0 */
    public static double stress(Player player) {
        var s = stress;
        return s == null ? 0 : s.applyAsDouble(player);
    }

    private static volatile MindProvider mind;

    /** Мод психики (RP Psyche): через него давят на стресс и рассудок. @since 0.15.0 */
    public static void registerMind(MindProvider provider) {
        mind = provider;
    }

    /** Разовый толчок стресса (шкала 0–100). false — мода психики нет, ничего не сделано. Сервер. @since 0.15.0 */
    public static boolean stressImpulse(net.minecraft.server.level.ServerPlayer player, double amount) {
        MindProvider m = mind;
        if (m == null) return false;
        if (amount > 0) m.stressImpulse(player, amount);
        return true;
    }

    /** Разовый удар по рассудку: минус {@code amount} (шкала 0–100). false — мода психики нет. Сервер. @since 0.15.0 */
    public static boolean sanityHit(net.minecraft.server.level.ServerPlayer player, double amount) {
        MindProvider m = mind;
        if (m == null) return false;
        if (amount > 0) m.sanityHit(player, amount);
        return true;
    }

    private static final Map<ResourceLocation, LightProvider> LIGHTS = new LinkedHashMap<>();
    private static volatile LightProvider[] lights = new LightProvider[0];

    /**
     * Мод сообщает, как узнать, что сущность несёт включённый свет. Тот же id второй раз — заменяет прежнего.
     * Регистрировать на обеих сторонах (конструктор мода или FMLCommonSetupEvent). @since 0.15.0
     */
    public static void registerLight(ResourceLocation id, LightProvider provider) {
        synchronized (LIGHTS) {
            LIGHTS.put(id, provider);
            lights = LIGHTS.values().toArray(new LightProvider[0]);
        }
    }

    /**
     * Несёт ли сущность включённый свет — так сказал хотя бы один поставщик ({@link LightProvider}). Без поставщиков
     * — false. Обе стороны. @since 0.15.0
     */
    public static boolean carriesLight(net.minecraft.world.entity.LivingEntity entity) {
        for (LightProvider p : lights) if (p.carriesLight(entity)) return true;
        return false;
    }

    private static volatile SleepProvider sleep;

    /** Мод сна (RP Needs). @since 0.14.0 */
    public static void registerSleep(SleepProvider provider) {
        sleep = provider;
    }

    /** Спит ли по-настоящему; без мода сна — в кровати или в позе {@code /sleep}. Сервер. @since 0.14.0 */
    public static boolean asleep(Player player) {
        var s = sleep;
        return s != null ? s.asleep(player) : player.isSleeping() || postureSleeping(player);
    }

    /** Стадия недосыпа ({@link SleepProvider#stage}); без мода сна — 0. Сервер. @since 0.14.0 */
    public static int sleepStage(Player player) {
        var s = sleep;
        return s == null ? 0 : s.stage(player);
    }

    /** Вайп персонажа: сейчас или при входе. true — уже сделано. Сервер. @since 0.14.0 */
    public static boolean wipeCharacter(net.minecraft.server.MinecraftServer server, java.util.UUID id, String name, String by) {
        return faygolover.rpcore.profile.CharacterWipe.wipe(server, id, name, by);
    }

    /** Разбудить спящего сидя или лёжа (RP Needs: выспался). Сервер. @since 0.11.0 */
    public static void wakeFromPosture(net.minecraft.server.level.ServerPlayer player) {
        faygolover.rpcore.pose.Postures.wakeUp(player);
    }

    /** Игрок валится с ног и засыпает где стоит: ложится на спину и спит (RP Needs, недосып). Сервер. @since 0.11.0 */
    public static boolean collapseAsleep(net.minecraft.server.level.ServerPlayer player) {
        return faygolover.rpcore.pose.Postures.collapse(player);
    }

    // ------------------------------------------------------------------ нагрузка

    /** Физическая нагрузка (сервер): потраченная выносливость RP Stamina. Слушать — {@link ExertionEvent}. */
    public static void exertion(Player player, double stamina) {
        if (stamina <= 0 || player.level().isClientSide) return;
        MinecraftForge.EVENT_BUS.post(new ExertionEvent(player, stamina));
    }

    /**
     * Выносливость восстановилась (сервер): RP Stamina сообщает, сколько вернулось. Слушать —
     * {@link faygolover.rpcore.api.event.StaminaRestoredEvent}. @since 0.5.0
     */
    public static void staminaRestored(Player player, double amount) {
        if (amount <= 0 || player.level().isClientSide) return;
        MinecraftForge.EVENT_BUS.post(new faygolover.rpcore.api.event.StaminaRestoredEvent(player, amount));
    }

    // ------------------------------------------------------------------ анкета тела

    /**
     * Анкета тела игрока (пол, возраст, рост). Сервер — для любого игрока; клиент — только своя (у других —
     * {@link BodyProfile#DEFAULT}). @since 0.5.0
     */
    public static BodyProfile profile(Player player) {
        return faygolover.rpcore.profile.ProfileServer.get(player);
    }

    /** Записать анкету (сервер; команда ГМа, переход со старых данных мода). @since 0.5.0 */
    public static void setProfile(net.minecraft.server.level.ServerPlayer player, BodyProfile profile) {
        faygolover.rpcore.profile.ProfileServer.set(player, profile);
    }

    /**
     * Клиент: отправить заполненную анкету (окно создания персонажа). Сервер примет её, только пока анкета не
     * заполнена. @since 0.5.0
     */
    public static void submitProfile(BodyProfile.Sex sex, int age, double heightCm) {
        faygolover.rpcore.network.CoreNetwork.sendToServer(new faygolover.rpcore.network.ProfilePackets.Submit(sex, age, heightCm));
    }

    /** Вкусы персонажа (любимое и нелюбимое). Сервер — любого игрока, клиент — свои. @since 0.10.0 */
    public static FoodTaste taste(Player player) {
        return faygolover.rpcore.profile.ProfileServer.taste(player);
    }

    /** Записать вкусы (сервер, ГМ). @since 0.10.0 */
    public static void setTaste(net.minecraft.server.level.ServerPlayer player, FoodTaste taste) {
        faygolover.rpcore.profile.ProfileServer.setTaste(player, taste);
    }

    /** Клиент: отправить выбранные вкусы (сервер примет один раз). @since 0.10.0 */
    public static void submitTaste(String favourite, String disliked) {
        faygolover.rpcore.network.CoreNetwork.sendToServer(new faygolover.rpcore.network.ProfilePackets.TasteSubmit(favourite, disliked));
    }

    /** Вес тела, кг: у мода питания, иначе у мода тела; NaN — никто не ведёт. @since 0.5.0 */
    public static double weightKg(Player player) {
        NutritionProvider n = nutrition;
        if (n != null) return n.weightKg(player);
        BodyProvider b = body;
        return b == null ? Double.NaN : b.weightKg(player);
    }

    /** Доля жира, %: у мода питания, иначе у мода тела; NaN — никто не ведёт. @since 0.5.0 */
    public static double bodyFatPercent(Player player) {
        NutritionProvider n = nutrition;
        if (n != null) return n.bodyFatPercent(player);
        BodyProvider b = body;
        return b == null ? Double.NaN : b.bodyFatPercent(player);
    }

    /** Тело относительно нормы своего пола и возраста (сервер). Вес никто не ведёт — норма. @since 0.5.0 */
    public static BodyNorms.BodyState bodyState(Player player) {
        return BodyNorms.state(profile(player), weightKg(player), bodyFatPercent(player));
    }

    /** Насколько хорошо человек сейчас учится, 0–1 (боль — у мода тела; без него — 1). @since 0.5.0 */
    public static double learningFactor(Player player) {
        BodyProvider b = body;
        if (b == null) return 1.0;
        double f = b.learningFactor(player);
        return Double.isFinite(f) ? Math.max(0, Math.min(1, f)) : 1.0;
    }

    // ------------------------------------------------------------------ навыки и черты

    public static void registerProvider(TraitProvider provider) {
        PROVIDERS.add(provider);
    }

    /** Есть ли у навыка уровни (кто-то его ведёт). Без такого мода навык «на максимуме» — решает читающий мод. */
    public static boolean isSkillManaged(ResourceLocation skill) {
        for (TraitProvider p : PROVIDERS) if (p.managesSkill(skill)) return true;
        return false;
    }

    /** Уровень навыка 0–10 или пусто, если навык никто не ведёт. */
    public static OptionalInt skill(Player player, ResourceLocation skill) {
        for (TraitProvider p : PROVIDERS) {
            if (p.managesSkill(skill)) return OptionalInt.of(Math.max(0, Math.min(10, p.skill(player, skill))));
        }
        return OptionalInt.empty();
    }

    /** Выставить уровень навыка (наработанный, дальше растёт сам); false — никто не ведёт этот навык. */
    public static boolean setSkill(Player player, ResourceLocation skill, int level) {
        for (TraitProvider p : PROVIDERS) {
            if (p.managesSkill(skill) && p.setSkill(player, skill, level)) return true;
        }
        return false;
    }

    /** Точный прогресс навыка: уровень + доля до следующего (3,4567); NaN — никто не ведёт или не умеет. @since 0.12.0 */
    public static double skillProgress(Player player, ResourceLocation skill) {
        for (TraitProvider p : PROVIDERS) {
            if (p.managesSkill(skill)) return p.skillProgress(player, skill);
        }
        return Double.NaN;
    }

    /** ГМ: прибавить или отнять уровни (дробно: 0,1 — 10 % уровня); false — никто не ведёт этот навык. @since 0.12.0 */
    public static boolean addSkill(Player player, ResourceLocation skill, double levels) {
        for (TraitProvider p : PROVIDERS) {
            if (p.managesSkill(skill) && p.addSkill(player, skill, levels)) return true;
        }
        return false;
    }

    /** Сообщить о практике навыка (сервер): тот, кто ведёт навык, начислит опыт. */
    public static void practice(Player player, ResourceLocation skill, double amount) {
        if (amount <= 0 || player.level().isClientSide) return;
        for (TraitProvider p : PROVIDERS) {
            if (p.managesSkill(skill)) {
                p.practice(player, skill, amount);
                return;
            }
        }
    }

    /** Прямой опыт навыка (сервер): ровно {@code xp} опыта, без пересчёта практики. @since 0.5.0 */
    public static void grantXp(Player player, ResourceLocation skill, double xp) {
        if (xp <= 0 || player.level().isClientSide) return;
        for (TraitProvider p : PROVIDERS) {
            if (p.managesSkill(skill)) {
                p.grantXp(player, skill, xp);
                return;
            }
        }
    }

    /**
     * Выдать знание — служебную черту, которую игрок получает в игре (прочитал записи). 1 — выдано, 0 — уже было,
     * −1 — некому выдать (нет мода черт или нет такого знания). Сервер. @since 0.15.0
     */
    public static int grantKnowledge(net.minecraft.server.level.ServerPlayer player, ResourceLocation trait) {
        for (TraitProvider p : PROVIDERS) {
            int r = p.grantKnowledge(player, trait);
            if (r >= 0) return r;
        }
        return -1;
    }

    private static final java.util.Set<ResourceLocation> UNKNOWN_ADVANCEMENTS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Выдать игроку достижение по id — ванильным способом, все его критерии. Так моды семейства выдают достижения
     * любого мода или датапака, не зная о нём. true — достижение есть (выдано сейчас или уже было); false — такого
     * достижения на сервере нет (одна строка в лог на id). Сервер. @since 0.15.0
     */
    public static boolean grantAdvancement(net.minecraft.server.level.ServerPlayer player, ResourceLocation id) {
        net.minecraft.advancements.Advancement adv = player.server.getAdvancements().getAdvancement(id);
        if (adv == null) {
            if (UNKNOWN_ADVANCEMENTS.add(id)) RpCore.LOGGER.warn("RP Core: достижения {} на сервере нет — не выдано", id);
            return false;
        }
        net.minecraft.advancements.AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
        if (progress.isDone()) return true;
        List<String> remaining = new ArrayList<>();
        for (String criterion : progress.getRemainingCriteria()) remaining.add(criterion);
        for (String criterion : remaining) player.getAdvancements().award(adv, criterion);
        return true;
    }

    public static boolean hasTrait(Player player, ResourceLocation trait) {
        for (TraitProvider p : PROVIDERS) if (p.hasTrait(player, trait)) return true;
        return false;
    }

    /** Список источников (для /rpcore). */
    public static List<String> providerNames() {
        List<String> out = new ArrayList<>();
        for (TraitProvider p : PROVIDERS) out.add(p.getClass().getName());
        return out;
    }

    /** Вкладка креатива «RP: разное» — для модов семейства, у которых слишком мало предметов на свою вкладку. */
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.CreativeModeTab> MISC_TAB =
            faygolover.rpcore.gm.GmRegistry.MISC_TAB_KEY;
}
