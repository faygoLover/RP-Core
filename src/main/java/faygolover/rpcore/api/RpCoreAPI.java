package faygolover.rpcore.api;

import faygolover.rpcore.api.event.ExertionEvent;
import net.minecraft.resources.ResourceLocation;
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
 *   <li>Тело ({@link BodyProvider}, RP Medicine) и питание ({@link NutritionProvider}, RP Culinary) — по одному моду.</li>
 *   <li>Нагрузка: {@link #exertion} — RP Stamina сообщает потраченную выносливость, питание считает калории.</li>
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

    /** Мод тела (RP Medicine). Один. */
    public static void registerBody(BodyProvider provider) {
        body = provider;
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

    // ------------------------------------------------------------------ питание

    private static volatile NutritionProvider nutrition;

    /** Мод питания (RP Culinary). Один. */
    public static void registerNutrition(NutritionProvider provider) {
        nutrition = provider;
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
