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
 *   <li>Время RP ({@code /rptime}): {@link #timeRunning}, события {@link faygolover.rpcore.api.event.RpTimeSkipEvent}.</li>
 *   <li>Фото игроков (портрет по скину) — {@link Photos}.</li>
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

    /** Второй мод на то же место: оставляем первого, в лог — оба имени. */
    private static boolean accept(String what, Object current, Object provider) {
        if (provider == null || current == provider) return false;
        if (current == null) return true;
        RpCore.LOGGER.error("RP Core: мод {} уже есть ({}), второй ({}) не подключён.", what,
                current.getClass().getName(), provider.getClass().getName());
        return false;
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

    // ------------------------------------------------------------------ нагрузка

    /** Физическая нагрузка (сервер): потраченная выносливость RP Stamina. Слушать — {@link ExertionEvent}. */
    public static void exertion(Player player, double stamina) {
        if (stamina <= 0 || player.level().isClientSide) return;
        MinecraftForge.EVENT_BUS.post(new ExertionEvent(player, stamina));
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
}
