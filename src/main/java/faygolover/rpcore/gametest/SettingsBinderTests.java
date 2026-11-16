package faygolover.rpcore.gametest;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import faygolover.rpcore.RpCore;
import faygolover.rpcore.api.config.SettingsBinder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Привязка настроек к конфигу ({@link SettingsBinder}). Главная проверка — файл: конфиг, собранный привязчиком,
 * должен совпасть до байта с конфигом, собранным теми же вызовами {@code ForgeConfigSpec}, какими его сейчас
 * собирают RP Medicine, RP Psyche и RP Culinary. На этом держится правило «файлы настроек на серверах не меняются».
 */
@GameTestHolder(RpCore.MODID)
@PrefixGameTestTemplate(false)
public final class SettingsBinderTests {
    private SettingsBinderTests() {}

    static final String T = "platform";

    public enum Kind {
        CUT("cut"), BURN("burn");
        public final String id;

        Kind(String id) {
            this.id = id;
        }
    }

    /** Как {@code MedicalSettings}: числа, флаг, целое с границами, таблица по перечислению, массив. */
    public static final class MedLike {
        public double severityPerDamage = 1.5;
        public boolean bleedingEnabled = true;
        public int stepTicks = 20;
        public int maxWoundsPerPart = 8;
        public EnumMap<Kind, Double> bleedPerSeverity = new EnumMap<>(Kind.class);
        public double[] skillSpeed = {1.0, 0.75, 0.5};

        public MedLike() {
            bleedPerSeverity.put(Kind.CUT, 2.0);
            bleedPerSeverity.put(Kind.BURN, 0.25);
        }
    }

    /** Как {@code Settings} RP Psyche: ещё длинное целое и список строк. */
    public static final class PsyLike {
        public double stressDecay = -0.8;
        public int selfTalkCount = 3;
        public long therapyPatientTicks = 12000L;
        public List<String> lightItems = List.of("minecraft:torch", "mod:lamp{on}");
        public List<String> voiceRadioItems = List.of();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Cfg {
        String section();

        String doc();
    }

    /** Как {@code Settings} RP Culinary: раздел и описание в пометке на поле, разделы вперемешку. */
    public static final class CulLike {
        @Cfg(section = "general", doc = "Голод в калориях.")
        public boolean hungerEnabled = true;
        @Cfg(section = "weight", doc = "Скорость изменения веса.")
        public double weightChangeSpeed = 1.0;
        @Cfg(section = "general", doc = "")
        public double consumptionSpeed = 2.5;
        /** Без пометки — в конфиг не идёт. */
        public double internal = 7;
        public static double ignoredStatic = 1;
    }

    private static String toml(ForgeConfigSpec spec) {
        return TomlFormat.instance().createWriter().writeToString(load(spec));
    }

    private static CommentedConfig load(ForgeConfigSpec spec) {
        CommentedConfig c = TomlFormat.newConfig(LinkedHashMap::new);
        spec.setConfig(c);
        return c;
    }

    private static SettingsBinder<MedLike> medBinder() {
        SettingsBinder<MedLike> b = SettingsBinder.of(MedLike.class, MedLike::new).doubleRange(0.0, 1.0e9).intRange(0, 1_000_000);
        b.push("general", "Общее.");
        b.bindInt("stepTicks", "Шаг, тики.", 1, 100);
        b.builder().comment("Обычное значение рядом.").defineInRange("search_seconds", 10.0, 0.0, 600.0);
        b.pop();
        b.push("wounds", "Раны.");
        b.bind("severityPerDamage", "Тяжесть за единицу урона.");
        b.bind("bleedingEnabled", "Кровотечения.");
        b.bind("maxWoundsPerPart", "Ран на часть тела.");
        b.bindMap("bleedPerSeverity", "Кровотечение раны" + " — по типам ран.", List.of(Kind.values()), k -> k.id);
        b.bindArray("skillSpeed", "Скорость по уровням." + " Ровно " + b.defaults().skillSpeed.length + " чисел.");
        b.pop();
        return b;
    }

    /** Те же вызовы, что сейчас в {@code ServerConfig} RP Medicine. */
    private static ForgeConfigSpec medByHand() {
        ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();
        MedLike d = new MedLike();
        B.comment("Общее.").push("general");
        B.comment("Шаг, тики.").defineInRange("step_ticks", d.stepTicks, 1, 100);
        B.comment("Обычное значение рядом.").defineInRange("search_seconds", 10.0, 0.0, 600.0);
        B.pop();
        B.comment("Раны.").push("wounds");
        B.comment("Тяжесть за единицу урона.").defineInRange("severity_per_damage", d.severityPerDamage, 0.0, 1.0e9);
        B.comment("Кровотечения.").define("bleeding_enabled", d.bleedingEnabled);
        B.comment("Ран на часть тела.").defineInRange("max_wounds_per_part", d.maxWoundsPerPart, 0, 1_000_000);
        B.comment("Кровотечение раны" + " — по типам ран.").push("bleed_per_severity");
        for (Kind k : Kind.values()) B.defineInRange(k.id, d.bleedPerSeverity.get(k), 0.0, 1.0e9);
        B.pop();
        List<Double> defList = new ArrayList<>();
        for (double x : d.skillSpeed) defList.add(x);
        B.comment("Скорость по уровням." + " Ровно " + d.skillSpeed.length + " чисел.").defineList("skill_speed", defList, o -> o instanceof Number);
        B.pop();
        return B.build();
    }

    private static SettingsBinder<PsyLike> psyBinder() {
        SettingsBinder<PsyLike> b = SettingsBinder.of(PsyLike.class, PsyLike::new);
        b.push("stress", "Стресс (0–100).");
        b.bind("stressDecay", "Спад стресса.");
        b.bind("selfTalkCount", "Фраз подряд.");
        b.bind("therapyPatientTicks", "Откат пациента, тиков.");
        b.bind("lightItems", "Что светит.");
        b.bind("voiceRadioItems", "Рации.");
        b.pop();
        return b;
    }

    /** Те же вызовы, что сейчас в {@code PsycheConfig}. */
    private static ForgeConfigSpec psyByHand() {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        PsyLike d = new PsyLike();
        b.comment("Стресс (0–100).").push("stress");
        b.comment("Спад стресса.").defineInRange("stress_decay", d.stressDecay, -1e9, 1e9);
        b.comment("Фраз подряд.").defineInRange("self_talk_count", d.selfTalkCount, 0, Integer.MAX_VALUE);
        b.comment("Откат пациента, тиков.").defineInRange("therapy_patient_ticks", d.therapyPatientTicks, 0L, Long.MAX_VALUE);
        b.comment("Что светит.").defineListAllowEmpty(List.of("light_items"), () -> d.lightItems, o -> o instanceof String);
        b.comment("Рации.").defineListAllowEmpty(List.of("voice_radio_items"), () -> d.voiceRadioItems, o -> o instanceof String);
        b.pop();
        return b.build();
    }

    private static SettingsBinder<CulLike> culBinder() {
        return SettingsBinder.of(CulLike.class, CulLike::new)
                .bindAnnotated(Cfg.class, Cfg::section, c -> c.doc().isEmpty() ? "" : " " + c.doc());
    }

    /** Те же вызовы, что сейчас в {@code CulinaryConfig}: разделы по первому появлению, пробел перед описанием. */
    private static ForgeConfigSpec culByHand() {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        CulLike d = new CulLike();
        b.push("general");
        b.comment(" " + "Голод в калориях.");
        b.define("hunger_enabled", d.hungerEnabled);
        b.defineInRange("consumption_speed", d.consumptionSpeed, -1e9, 1e9);
        b.pop();
        b.push("weight");
        b.comment(" " + "Скорость изменения веса.");
        b.defineInRange("weight_change_speed", d.weightChangeSpeed, -1e9, 1e9);
        b.pop();
        return b.build();
    }

    private static void same(GameTestHelper h, String who, String byBinder, String byHand) {
        if (byBinder.equals(byHand)) return;
        RpCore.LOGGER.error("SettingsBinder, {}: привязчик дал\n{}\nа вручную\n{}", who, byBinder, byHand);
        h.fail("Файл настроек «как у " + who + "» отличается от собранного вручную (оба текста — в логе)");
    }

    /** Файл от привязчика совпадает до байта с файлом от прежних вызовов — для всех трёх стилей. */
    @GameTest(template = T, batch = "core_settings_1")
    public static void tomlIsByteIdentical(GameTestHelper h) {
        String med = toml(medBinder().build());
        same(h, "Medicine", med, toml(medByHand()));
        same(h, "Psyche", toml(psyBinder().build()), toml(psyByHand()));
        String cul = toml(culBinder().build());
        same(h, "Culinary", cul, toml(culByHand()));
        // Сравнение двух пустых файлов тоже «совпало бы» — убеждаемся, что в файле то, что ждём.
        for (String line : List.of("[general]", "step_ticks = 20", "search_seconds = 10.0", "[wounds.bleed_per_severity]",
                "burn = 0.25", "skill_speed = [1.0, 0.75, 0.5]", "#Тяжесть за единицу урона.", "max_wounds_per_part = 8"))
            if (!med.contains(line)) h.fail("В файле «как у Medicine» нет строки «" + line + "»:\n" + med);
        if (cul.contains("internal") || cul.contains("ignored_static")) h.fail("В конфиг попало поле без пометки:\n" + cul);
        if (!cul.contains("# Голод в калориях.")) h.fail("Описание из пометки не дошло до файла:\n" + cul);
        h.succeed();
    }

    /** bake читает значения из конфига, в том числе таблицу, массив и списки; значения по умолчанию не портятся. */
    @GameTest(template = T, batch = "core_settings_2")
    public static void bakeReadsConfig(GameTestHelper h) {
        SettingsBinder<MedLike> mb = medBinder();
        if (mb.loaded()) h.fail("Привязчик считает конфиг загруженным до build()");
        ForgeConfigSpec spec = mb.build();
        CommentedConfig c = TomlFormat.newConfig(LinkedHashMap::new);
        c.set("wounds.severity_per_damage", 3.25);
        c.set("wounds.bleeding_enabled", false);
        c.set("general.step_ticks", 40);
        c.set("wounds.bleed_per_severity.burn", 9.0);
        c.set("wounds.skill_speed", List.of(5.0, 6));
        spec.setConfig(c);
        if (!mb.loaded()) h.fail("После загрузки конфига привязчик считает его незагруженным");
        MedLike s = mb.bake();
        if (s.severityPerDamage != 3.25 || s.bleedingEnabled || s.stepTicks != 40) h.fail("Простые значения прочитаны не так");
        if (s.maxWoundsPerPart != 8) h.fail("Нетронутое значение изменилось: " + s.maxWoundsPerPart);
        if (s.bleedPerSeverity.get(Kind.BURN) != 9.0 || s.bleedPerSeverity.get(Kind.CUT) != 2.0)
            h.fail("Таблица прочитана не так: " + s.bleedPerSeverity);
        // В файле два числа из трёх: третье — из значений по умолчанию.
        if (s.skillSpeed.length != 3 || s.skillSpeed[0] != 5.0 || s.skillSpeed[1] != 6.0 || s.skillSpeed[2] != 0.5)
            h.fail("Массив прочитан не так: " + java.util.Arrays.toString(s.skillSpeed));
        MedLike d = mb.defaults();
        if (d.severityPerDamage != 1.5 || d.bleedPerSeverity.get(Kind.BURN) != 0.25 || d.skillSpeed[0] != 1.0)
            h.fail("bake испортил значения по умолчанию");
        if (mb.bake() == s) h.fail("bake вернул тот же объект, а не новый");

        SettingsBinder<PsyLike> pb = psyBinder();
        CommentedConfig pc = TomlFormat.newConfig(LinkedHashMap::new);
        pc.set("stress.therapy_patient_ticks", 5_000_000_000L);
        pc.set("stress.light_items", List.of("a:b"));
        pb.build().setConfig(pc);
        PsyLike p = pb.bake();
        if (p.therapyPatientTicks != 5_000_000_000L) h.fail("Длинное целое прочитано не так: " + p.therapyPatientTicks);
        if (!p.lightItems.equals(List.of("a:b")) || !p.voiceRadioItems.isEmpty()) h.fail("Списки прочитаны не так");
        h.succeed();
    }

    /**
     * Число вне границ Forge подтягивает к ближайшей границе, значение не того типа заменяет значением по умолчанию —
     * и в файле, и в настройках. Границы берутся и с привязчика, и свои у поля.
     */
    @GameTest(template = T, batch = "core_settings_3")
    public static void outOfRangeIsCorrected(GameTestHelper h) {
        SettingsBinder<MedLike> mb = medBinder();
        CommentedConfig c = TomlFormat.newConfig(LinkedHashMap::new);
        c.set("wounds.severity_per_damage", -4.0);          // границы привязчика: от 0
        c.set("general.step_ticks", 500);                    // свои границы поля: до 100
        c.set("wounds.max_wounds_per_part", 2_000_000);      // границы привязчика для целых: до 1 000 000
        c.set("wounds.bleeding_enabled", "да");
        mb.build().setConfig(c);
        MedLike s = mb.bake();
        if (s.severityPerDamage != 0.0) h.fail("Число ниже границы не подтянуто к границе: " + s.severityPerDamage);
        if (s.stepTicks != 100) h.fail("Целое выше своей границы не подтянуто: " + s.stepTicks);
        if (s.maxWoundsPerPart != 1_000_000) h.fail("Целое выше общей границы не подтянуто: " + s.maxWoundsPerPart);
        if (!s.bleedingEnabled) h.fail("Флаг не того типа не заменён значением по умолчанию");
        if (c.<Number>get("general.step_ticks").intValue() != 100) h.fail("В файле осталось значение вне границ");
        h.succeed();
    }

    /** set меняет значение в конфиге; чужое поле и не тот тип — отказ; ошибки описания — понятные исключения. */
    @GameTest(template = T, batch = "core_settings_4")
    public static void setAndMistakes(GameTestHelper h) {
        SettingsBinder<CulLike> cb = culBinder();
        CommentedConfig c = load(cb.build());
        if (!cb.set("consumptionSpeed", 4)) h.fail("set отказал для числового поля");
        if (cb.bake().consumptionSpeed != 4.0) h.fail("После set bake читает старое значение");
        if (c.<Number>get("general.consumption_speed").doubleValue() != 4.0) h.fail("set не дошёл до конфига");
        if (!cb.set("hungerEnabled", false) || cb.bake().hungerEnabled) h.fail("set не поменял флаг");
        if (cb.set("hungerEnabled", 1)) h.fail("set принял число для флага");
        if (cb.set("consumptionSpeed", "много")) h.fail("set принял строку для числа");
        if (cb.set("internal", 1)) h.fail("set принял поле, которого нет в конфиге");

        SettingsBinder<MedLike> mb = SettingsBinder.of(MedLike.class, MedLike::new);
        try {
            mb.bind("noSuchField", "…");
            h.fail("Привязка несуществующего поля прошла без ошибки");
        } catch (IllegalStateException expected) {
            // так и должно быть
        }
        try {
            mb.bind("skillSpeed", "…");
            h.fail("Массив привязан как простое поле без ошибки");
        } catch (IllegalStateException expected) {
            // так и должно быть
        }
        mb.bind("stepTicks", "…");
        try {
            mb.bind("stepTicks", "…");
            h.fail("Поле привязано дважды без ошибки");
        } catch (IllegalStateException expected) {
            // так и должно быть
        }
        if (!SettingsBinder.snake("maxWoundsPerPart").equals("max_wounds_per_part") || !SettingsBinder.snake("withdrawalOnsetRpHours").equals("withdrawal_onset_rp_hours"))
            h.fail("Имя ключа строится не так");
        h.succeed();
    }
}
