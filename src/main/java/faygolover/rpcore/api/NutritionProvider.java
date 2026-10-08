package faygolover.rpcore.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Мод питания (RP Culinary): сытость в калориях, вода, нутриенты, вес и жир. Когда он стоит, другие моды
 * семейства берут голод и жажду у него: RP Medicine — для заживления, иммунитета, анализов и веса; RP Stamina —
 * «нет сил» вместо ванильной сытости. Регистрирует — {@link RpCoreAPI#registerNutrition}. Методы — на сервере.
 */
public interface NutritionProvider {
    /** Нутриенты: запасы 0–120 (ниже 20 — нехватка, выше 100 — избыток). */
    String PROTEIN = "protein", FAT = "fat", CARBS = "carbs", VITAMINS = "vitamins";

    /** Ведёт ли мод эту часть ({@link RpIds#FOOD_HUNGER}, {@link RpIds#FOOD_THIRST}, {@link RpIds#FOOD_WEIGHT}…). */
    boolean handles(ResourceLocation feature);

    /** Сытость: 0 — пусто, 1 — сыт, выше 1 — переел. */
    double satiety(Player player);

    /** Вода 0–1 (с LSO — его вода). */
    double hydration(Player player);

    /** Запас нутриента 0–120 или NaN, если не ведётся. */
    double nutrient(Player player, String id);

    /** Голод: 0 — сыт, 1 — голоден, 2 — очень голоден («нет сил»), 3 — голодает (худеет). */
    int hungerStage(Player player);

    /** Плохой аппетит (приелось, после капельницы, болезнь). */
    boolean poorAppetite(Player player);

    double weightKg(Player player);

    double bodyFatPercent(Player player);

    /** ГМ выставил вес и/или жир (NaN — не менять). */
    void setBody(Player player, double weightKg, double fatPercent);

    /** Вода: плюс — попил (капельница), минус — потерял (рвота, жар); проценты 0–100. */
    void addWater(Player player, double percent);

    /** Рвота: желудок пустеет — съеденное, но не усвоенное, пропадает. */
    void vomit(Player player);

    /** Внутривенное питание: состав растекается по телу за {@code seconds} секунд игры, потом плохой аппетит. */
    void feedIntravenous(Player player, double kcal, double protein, double fat, double carbs, double vitamins, int seconds);

    /** Прокрутка времени (команда ГМа, сон): голод и жажда за {@code hours} часов игры. */
    void advance(Player player, double hours);
}
