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
     * Ниже какой доли шкалы прокрутка времени (и время вне сети) не опускает еду и воду ({@code [time] skip_floor},
     * по умолчанию 0.2). Сервер. @since 0.11.0
     */
    public static double skipFloor() {
        return CoreConfig.SPEC.isLoaded() ? CoreConfig.SKIP_FLOOR.get() : 0.2;
    }

    // ------------------------------------------------------------------ позы и сон сидя/лёжа

    /** Поза игрока: сидит на земле ({@code /sit}), лежит на спине ({@code /lay}), сидит на сиденье мода или стоит. @since 0.11.0 */
    public enum Posture { NONE, SIT, LAY, SEAT }

    /** Поза игрока сейчас (сервер). {@link Posture#SEAT} — едет на сиденье мода (стул, скамья). @since 0.11.0 */
    public static Posture posture(Player player) {
        return faygolover.rpcore.pose.Postures.posture(player);
    }

    /** Спит ли игрок сидя или лёжа ({@code /sleep}); кровати — ванильный {@code isSleeping()}. Сервер. @since 0.11.0 */
    public static boolean postureSleeping(Player player) {
        return faygolover.rpcore.pose.Postures.sleeping(player);
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

    /** Выставить уровень навыка; false — никто не ведёт этот навык. */
    public static boolean setSkill(Player player, ResourceLocation skill, int level) {
        for (TraitProvider p : PROVIDERS) {
            if (p.managesSkill(skill) && p.setSkill(player, skill, level)) return true;
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
