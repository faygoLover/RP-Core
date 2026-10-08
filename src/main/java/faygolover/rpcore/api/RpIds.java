package faygolover.rpcore.api;

import net.minecraft.resources.ResourceLocation;

/**
 * Общие названия навыков и черт. Моды договариваются через них, а не через классы друг друга:
 * RP Medicine спрашивает «есть ли черта diabetic», RP Perks отвечает по своим перкам.
 */
public final class RpIds {
    private RpIds() {}

    private static ResourceLocation id(String path) {
        return new ResourceLocation("rpcore", path);
    }

    // ------------------------------------------------------------------ навыки (0–10)
    /** Медицина (RP Medicine). */
    public static final ResourceLocation MEDICINE = id("medicine");
    /** Кулинария (будущий мод питания). */
    public static final ResourceLocation COOKING = id("cooking");
    /** Короткоствольное оружие: пистолеты, пистолеты-пулемёты. */
    public static final ResourceLocation SHORT_ARMS = id("short_arms");
    /** Длинноствольное оружие: винтовки, дробовики. */
    public static final ResourceLocation LONG_ARMS = id("long_arms");
    /** Тяжёлое оружие: пулемёты, снайперские винтовки, гранатомёты. */
    public static final ResourceLocation HEAVY_ARMS = id("heavy_arms");
    public static final ResourceLocation EXPLOSIVES = id("explosives");
    /** Ближний бой: любое оружие ближнего боя и голые руки. */
    public static final ResourceLocation MELEE = id("melee");
    /** Сила (растёт от нагрузки: удары, добыча). */
    public static final ResourceLocation STRENGTH = id("strength");
    /** Форма (растёт от нагрузки: бег, плавание, лестницы). */
    public static final ResourceLocation FITNESS = id("fitness");

    // ------------------------------------------------------------------ черты тела и характера
    /** Крепкий: тяжелее ранить, дольше держится. */
    public static final ResourceLocation TOUGH = id("tough");
    /** Хрупкий: легче ломается и теряет сознание. */
    public static final ResourceLocation FRAGILE = id("fragile");
    /** Храбрый: болевой шок слабее. */
    public static final ResourceLocation BRAVE = id("brave");
    /** Трусливый: болевой шок сильнее. */
    public static final ResourceLocation COWARD = id("coward");
    public static final ResourceLocation DIABETIC = id("diabetic");
    public static final ResourceLocation SMOKER = id("smoker");
    public static final ResourceLocation ALCOHOLIC = id("alcoholic");
    public static final ResourceLocation TEETOTALER = id("teetotaler");
    public static final ResourceLocation CAFFEINE_ADDICT = id("caffeine_addict");
    /** Анорексия: маленький желудок, плохой аппетит. */
    public static final ResourceLocation ANOREXIC = id("anorexic");

    // ------------------------------------------------------------------ части физиологии (BodyProvider.handles)
    /** Ванильное здоровье заменено своим (максимум здоровья от перков не имеет смысла). */
    public static final ResourceLocation BODY_HEALTH = id("body/health");
    /** Привыкание и ломка от кофеина ведёт мод тела. */
    public static final ResourceLocation BODY_CAFFEINE = id("body/caffeine");
    /** Травмы ног (растяжение, вывих) ведёт мод тела. */
    public static final ResourceLocation BODY_SPRAIN = id("body/sprain");
    /** Как нехватка и избыток нутриентов действуют на тело (заживление, иммунитет, выносливость), решает мод тела. */
    public static final ResourceLocation BODY_NUTRITION = id("body/nutrition");

    // ------------------------------------------------------------------ части питания (NutritionProvider.handles)
    /** Голод в калориях вместо ванильной сытости. */
    public static final ResourceLocation FOOD_HUNGER = id("food/hunger");
    /** Жажда (своя или LSO полоской). */
    public static final ResourceLocation FOOD_THIRST = id("food/thirst");
    /** Вес и жир меняются от калорий. */
    public static final ResourceLocation FOOD_WEIGHT = id("food/weight");
}
