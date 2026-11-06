package faygolover.rpcore.api;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

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
    /** Телосложение (бывшая «Форма»; растёт от нагрузки: бег, плавание, лестницы). Id не меняется — сохранения. */
    public static final ResourceLocation FITNESS = id("fitness");
    /** Все навыки семейства по порядку (подсказки команд, окна). */
    public static final List<ResourceLocation> SKILLS = List.of(MEDICINE, COOKING, SHORT_ARMS, LONG_ARMS, HEAVY_ARMS,
            EXPLOSIVES, MELEE, STRENGTH, FITNESS);

    // ------------------------------------------------------------------ черты тела и характера
    /** Крепкий: тяжелее ранить, дольше держится. */
    public static final ResourceLocation TOUGH = id("tough");
    /** Хрупкий: легче ломается и теряет сознание. */
    public static final ResourceLocation FRAGILE = id("fragile");
    /** Храбрый: болевой шок слабее. */
    public static final ResourceLocation BRAVE = id("brave");
    /** Трусливый: болевой шок сильнее. */
    public static final ResourceLocation COWARD = id("coward");
    /** Раны и переломы заживают быстрее. */
    public static final ResourceLocation FAST_HEALING = id("fast_healing");
    /** Раны и переломы заживают медленнее. */
    public static final ResourceLocation SLOW_HEALING = id("slow_healing");
    /** Крепкий иммунитет: инфекции слабее. */
    public static final ResourceLocation STRONG_IMMUNITY = id("strong_immunity");
    /** Слабый иммунитет: инфекции тяжелее. */
    public static final ResourceLocation WEAK_IMMUNITY = id("weak_immunity");
    public static final ResourceLocation DIABETIC = id("diabetic");
    public static final ResourceLocation SMOKER = id("smoker");
    /** Стойкий организм (бывш. «Закалённый алкоголик»): все вещества и лекарства действуют слабее (Medicine). Id прежний. */
    public static final ResourceLocation ALCOHOLIC = id("alcoholic");
    public static final ResourceLocation TEETOTALER = id("teetotaler");
    public static final ResourceLocation CAFFEINE_ADDICT = id("caffeine_addict");
    /** Анорексия: маленький желудок, плохой аппетит. */
    public static final ResourceLocation ANOREXIC = id("anorexic");
    /** Диетолог: точный состав еды и советы. */
    public static final ResourceLocation DIETITIAN = id("dietitian");
    /** Полевой повар: на костре и в коптильне готовит как на 2 уровня выше. */
    public static final ResourceLocation FIELD_COOK = id("field_cook");
    /**
     * Чистюля: почти не травится сырым и сомнительным.
     * @deprecated с 0.5.0 — особенность «Железный желудок», {@link #IRON_STOMACH}. Perks отдаёт обе черты.
     */
    @Deprecated
    public static final ResourceLocation CLEAN = id("clean");
    /** Гурман: однообразное надоедает быстрее, вкусное радует сильнее. */
    public static final ResourceLocation GOURMET = id("gourmet");
    /** Кухонный кошмар: готовит на 3 уровня хуже. */
    public static final ResourceLocation KITCHEN_NIGHTMARE = id("kitchen_nightmare");
    /** Быстрый обмен веществ: тратит больше, худеет легче. */
    public static final ResourceLocation FAST_METABOLISM = id("fast_metabolism");
    /** Склонность к полноте: тратит меньше, полнеет легче. */
    public static final ResourceLocation SLOW_METABOLISM = id("slow_metabolism");

    // ------------------------------------------------------------------ особенности 0.5.0 (каталог: RP Stamina Sports, docs/07)
    // Здесь — только те, чьи механики живут не в RP Perks: другой мод спрашивает «есть ли черта» и делает своё.
    /** Живучий: заживление быстрее, кровопотеря и боль слабее (Medicine). @since 0.5.0 */
    public static final ResourceLocation HARDY = id("hardy");
    /** Нежный: заживление медленнее, кровопотеря и боль сильнее (Medicine). @since 0.5.0 */
    public static final ResourceLocation DELICATE = id("delicate");
    /** Выносливый: восстановление выносливости быстрее, бег и плавание дешевле (Stamina). @since 0.5.0 */
    public static final ResourceLocation ENDURING = id("enduring");
    /** Задохлик: восстановление медленнее, бег и плавание дороже (Stamina). @since 0.5.0 */
    public static final ResourceLocation SHORT_WINDED = id("short_winded");
    /** Астматик: на бегу падает кислород, приступы (Medicine, Stamina). @since 0.5.0 */
    public static final ResourceLocation ASTHMATIC = id("asthmatic");
    /** Второе дыхание: раз в час при нуле выносливости — сразу половина (Stamina). @since 0.5.0 */
    public static final ResourceLocation SECOND_WIND = id("second_wind");
    /** Быстрый отдых: сидя восстанавливается вдвое быстрее (Stamina). @since 0.5.0 */
    public static final ResourceLocation QUICK_REST = id("quick_rest");
    /** Любитель спорта: тренировки полезнее, мышцы восстанавливаются быстрее (Sports). @since 0.5.0 */
    public static final ResourceLocation GYM_LOVER = id("gym_lover");
    /** Ампутант: старт без руки или ноги (Medicine). @since 0.5.0 */
    public static final ResourceLocation AMPUTEE = id("amputee");
    /** Гемофоб: паника при виде крови, лечение с кровью труднее (Medicine). @since 0.5.0 */
    public static final ResourceLocation HEMOPHOBIC = id("hemophobic");
    /** Чувствительность к солнцу: днём под небом болит голова (Medicine). @since 0.5.0 */
    public static final ResourceLocation SUN_SENSITIVE = id("sun_sensitive");
    /** Железный желудок: отравление едой вдвое реже, переедание позже (Culinary). @since 0.5.0 */
    public static final ResourceLocation IRON_STOMACH = id("iron_stomach");
    /** Слабый желудок: отравление чаще, переедание раньше (Culinary). @since 0.5.0 */
    public static final ResourceLocation WEAK_STOMACH = id("weak_stomach");
    /** Малоежка: голод медленнее, желудок меньше (Culinary). @since 0.5.0 */
    public static final ResourceLocation LIGHT_EATER = id("light_eater");
    /** Обжора: голод быстрее, желудок больше (Culinary). @since 0.5.0 */
    public static final ResourceLocation BIG_EATER = id("big_eater");
    /** Мало пьёт: вода тратится медленнее (Culinary). @since 0.5.0 */
    public static final ResourceLocation LOW_THIRST = id("low_thirst");
    /** Много пьёт: вода тратится быстрее (Culinary). @since 0.5.0 */
    public static final ResourceLocation HIGH_THIRST = id("high_thirst");
    /** Плохие зубы: боль при еде (Culinary, Medicine). @since 0.5.0 */
    public static final ResourceLocation BAD_TEETH = id("bad_teeth");
    /** Дистрофик: старт ниже нормы веса (Culinary). @since 0.5.0 */
    public static final ResourceLocation EMACIATED = id("emaciated");
    /** Избыточный вес: старт выше нормы (Culinary). @since 0.5.0 */
    public static final ResourceLocation OVERWEIGHT = id("overweight");
    /** Жирный: старт с ожирением (Culinary). @since 0.5.0 */
    public static final ResourceLocation OBESE = id("obese");
    /** Ловкий: зоны мини-игр шире (Medicine, Sports). @since 0.5.0 */
    public static final ResourceLocation AGILE = id("agile");
    /** Плохая координация: зоны мини-игр уже, роняет предметы (Medicine, Sports). @since 0.5.0 */
    public static final ResourceLocation CLUMSY = id("clumsy");
    /** Неуклюжий бегун: на бегу иногда спотыкается (Stamina, Medicine). @since 0.5.0 */
    public static final ResourceLocation CLUMSY_RUNNER = id("clumsy_runner");
    /** Неудачливый: неприятности случаются с ним (Sports — плохие события раньше). @since 0.5.0 */
    public static final ResourceLocation UNLUCKY = id("unlucky");
    /** Удачливый: зеркало {@link #UNLUCKY} — беды обходят его (Perks, Sports, Medicine). @since 0.13.0 */
    public static final ResourceLocation LUCKY = id("lucky");
    /** Взлом: отмычки КПК, замки SA и Cuffed (PDA, RP Fixes). @since 0.13.0 */
    public static final ResourceLocation HACKER = id("hacker");
    /** Технофоб: не взламывает и не шифрует, не перестраивает рацию (PDA, RP Fixes). @since 0.13.0 */
    public static final ResourceLocation TECHNOPHOBE = id("technophobe");
    /** Боец ближнего боя: удары и груша тратят меньше выносливости (Sports). @since 0.5.0 */
    public static final ResourceLocation MELEE_FIGHTER = id("melee_fighter");
    /** Расторопный: медицина и готовка быстрее (Medicine, Culinary). @since 0.5.0 */
    public static final ResourceLocation QUICK_WORKER = id("quick_worker");
    /** Медлительный: медицина и готовка медленнее (Medicine, Culinary). @since 0.5.0 */
    public static final ResourceLocation SLOW_WORKER = id("slow_worker");
    /** Алкоголик: зависимость, без алкоголя — стресс и ломка (Medicine). Не путать с {@link #ALCOHOLIC} — «Стойкий организм». @since 0.5.0 */
    public static final ResourceLocation ALCOHOL_ADDICT = id("alcohol_addict");
    /** Неграмотный: не читает тексты (PDA). @since 0.5.0 */
    public static final ResourceLocation ILLITERATE = id("illiterate");
    /** Волевой: рассудок крепче, не теряет контроль в панике (Psyche). @since 0.7.1 */
    public static final ResourceLocation STRONG_WILLED = id("strong_willed");
    /** Внушаемый: рассудок слабее, Пси-зона бьёт сильнее (Psyche). @since 0.7.1 */
    public static final ResourceLocation SUGGESTIBLE = id("suggestible");

    // ------------------------------------------------------------------ части тела (BodyProvider: ушибы, мышцы)
    /** @since 0.5.0 */
    public static final ResourceLocation PART_HEAD = id("part/head");
    /** @since 0.5.0 */
    public static final ResourceLocation PART_CHEST = id("part/chest");
    /** Живот (пресс). @since 0.5.0 */
    public static final ResourceLocation PART_ABDOMEN = id("part/abdomen");
    /** @since 0.5.0 */
    public static final ResourceLocation PART_LEFT_ARM = id("part/left_arm");
    /** @since 0.5.0 */
    public static final ResourceLocation PART_RIGHT_ARM = id("part/right_arm");
    /** @since 0.5.0 */
    public static final ResourceLocation PART_LEFT_LEG = id("part/left_leg");
    /** @since 0.5.0 */
    public static final ResourceLocation PART_RIGHT_LEG = id("part/right_leg");
    /** @since 0.5.0 */
    public static final ResourceLocation PART_LEFT_FOOT = id("part/left_foot");
    /** @since 0.5.0 */
    public static final ResourceLocation PART_RIGHT_FOOT = id("part/right_foot");
    /** Все части тела по порядку. @since 0.5.0 */
    public static final List<ResourceLocation> PARTS = List.of(PART_HEAD, PART_CHEST, PART_ABDOMEN, PART_LEFT_ARM,
            PART_RIGHT_ARM, PART_LEFT_LEG, PART_RIGHT_LEG, PART_LEFT_FOOT, PART_RIGHT_FOOT);

    // ------------------------------------------------------------------ состояния (BodyProvider.condition)
    /** Переполнение желудка (переел через силу): тяжесть → тошнота → боль, рвота. @since 0.5.0 */
    public static final ResourceLocation COND_OVERFILL = id("cond/overfill");
    /** Травма желудка (впихивал до предела не раз). @since 0.5.0 */
    public static final ResourceLocation COND_STOMACH_INJURY = id("cond/stomach_injury");
    /** Перепил воды: водная интоксикация → отёк мозга. @since 0.5.0 */
    public static final ResourceLocation COND_OVERHYDRATION = id("cond/overhydration");
    /** Цинга (долго мало витаминов). @since 0.5.0 */
    public static final ResourceLocation COND_SCURVY = id("cond/scurvy");
    /** Камни в почках (долго много белка или мало воды). @since 0.5.0 */
    public static final ResourceLocation COND_KIDNEY_STONES = id("cond/kidney_stones");
    /** Белковая недостаточность (долго мало белка). @since 0.5.0 */
    public static final ResourceLocation COND_PROTEIN_DEFICIENCY = id("cond/protein_deficiency");
    /** Повышенное давление (долго много жиров). @since 0.5.0 */
    public static final ResourceLocation COND_HIGH_PRESSURE = id("cond/high_pressure");

    // ------------------------------------------------------------------ части физиологии (BodyProvider.handles)
    /** Ванильное здоровье заменено своим (максимум здоровья от перков не имеет смысла). */
    public static final ResourceLocation BODY_HEALTH = id("body/health");
    /** Привыкание и ломка от кофеина ведёт мод тела. */
    public static final ResourceLocation BODY_CAFFEINE = id("body/caffeine");
    /** Травмы ног (растяжение, вывих) ведёт мод тела. */
    public static final ResourceLocation BODY_SPRAIN = id("body/sprain");
    /** Как нехватка и избыток нутриентов действуют на тело (заживление, иммунитет, выносливость), решает мод тела. */
    public static final ResourceLocation BODY_NUTRITION = id("body/nutrition");
    /** Ушибы частей тела ведёт мод тела ({@link BodyProvider#bruise}). @since 0.5.0 */
    public static final ResourceLocation BODY_BRUISES = id("body/bruises");
    /** Боль в мышцах, растяжения и надрывы ведёт мод тела ({@link BodyProvider#strain}). @since 0.5.0 */
    public static final ResourceLocation BODY_MUSCLES = id("body/muscles");
    /** Последствия питания ведёт мод тела ({@link BodyProvider#condition}). @since 0.5.0 */
    public static final ResourceLocation BODY_CONDITIONS = id("body/conditions");
    /** Ощущения текстом показывает мод тела ({@link BodyProvider#sensation}). @since 0.5.0 */
    public static final ResourceLocation BODY_SENSATIONS = id("body/sensations");

    // ------------------------------------------------------------------ части питания (NutritionProvider.handles)
    /** Голод в калориях вместо ванильной сытости. */
    public static final ResourceLocation FOOD_HUNGER = id("food/hunger");
    /** Жажда (своя или LSO полоской). */
    public static final ResourceLocation FOOD_THIRST = id("food/thirst");
    /** Вес и жир меняются от калорий. */
    public static final ResourceLocation FOOD_WEIGHT = id("food/weight");
    /**
     * Укусы и глотки: мод питания сам шлёт {@link faygolover.rpcore.api.event.RpConsumeEvent} с долями (иначе — RP Core,
     * целым предметом, когда доеден). @since 0.10.0
     */
    public static final ResourceLocation FOOD_PORTIONS = id("food/portions");
}
