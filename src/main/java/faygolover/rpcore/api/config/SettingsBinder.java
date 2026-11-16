package faygolover.rpcore.api.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Привязка полей класса настроек к конфигу Forge: одно поле — один ключ в {@code toml}. Мод держит числа механики
 * в обычном классе с публичными полями (его можно проверять без игры), а этот класс строит по нему конфиг и
 * перекладывает значения из конфига в новый объект настроек.
 * <p>
 * <b>Когда использовать:</b> у мода есть класс настроек и файл конфига с теми же числами. Свои {@code bind(...)},
 * списки «поле + значение» и перевод имён писать не нужно.
 * <pre>{@code
 * static final SettingsBinder<Settings> B = SettingsBinder.of(Settings.class, Settings::new)
 *         .doubleRange(0, 1e9).intRange(0, 1_000_000);
 * static {
 *     B.push("wounds", "Раны.");
 *     B.bind("severityPerDamage", "Тяжесть за единицу урона.");       // ключ severity_per_damage
 *     B.bindInt("maxWoundsPerPart", "Ран на часть тела.", 1, 32);
 *     B.pop();
 * }
 * public static final ForgeConfigSpec SPEC = B.build();
 *
 * Settings.set(B.bake());                    // при загрузке и перечитывании конфига
 * }</pre>
 * <b>Правила, от которых зависит вид файла</b> (менять их — значит менять файлы на серверах):
 * имя ключа — имя поля, где каждая заглавная буква заменена на «_» и строчную ({@link #snake}); текст описания
 * идёт в файл как передан; порядок ключей равен порядку вызовов; границы чисел по умолчанию задаются на привязчик
 * ({@link #doubleRange}, {@link #intRange}, {@link #longRange}).
 * <p>
 * Обычные значения {@code ForgeConfigSpec}, которым поле настроек не нужно, объявляются рядом через
 * {@link #builder()} — в том же файле и в том же порядке.
 *
 * @param <S> класс настроек: публичные нестатические поля, значения по умолчанию — в самом классе
 * @since 0.15.0
 */
public final class SettingsBinder<S> {
    private record Bound(Field field, ForgeConfigSpec.ConfigValue<?> value) {}

    private final Class<S> type;
    private final Supplier<S> factory;
    private final S defaults;
    private final ForgeConfigSpec.Builder builder;
    private final List<Consumer<S>> appliers = new ArrayList<>();
    private final Map<String, Bound> simple = new LinkedHashMap<>();
    private final List<String> bound = new ArrayList<>();
    private double doubleMin = -1.0e9, doubleMax = 1.0e9;
    private int intMin = 0, intMax = Integer.MAX_VALUE;
    private long longMin = 0L, longMax = Long.MAX_VALUE;
    @Nullable
    private ForgeConfigSpec spec;

    private SettingsBinder(Class<S> type, Supplier<S> factory, ForgeConfigSpec.Builder builder) {
        this.type = type;
        this.factory = factory;
        this.defaults = factory.get();
        this.builder = builder;
    }

    /** Привязчик со своим новым конфигом. */
    public static <S> SettingsBinder<S> of(Class<S> type, Supplier<S> factory) {
        return of(type, factory, new ForgeConfigSpec.Builder());
    }

    /** Привязчик поверх уже начатого конфига (например, второй класс настроек в том же файле). */
    public static <S> SettingsBinder<S> of(Class<S> type, Supplier<S> factory, ForgeConfigSpec.Builder builder) {
        return new SettingsBinder<>(Objects.requireNonNull(type), Objects.requireNonNull(factory), Objects.requireNonNull(builder));
    }

    // ------------------------------------------------------------------ границы по умолчанию

    /** Границы для полей {@code double} (и значений {@link #bindMap}). По умолчанию −1e9 … 1e9. */
    public SettingsBinder<S> doubleRange(double min, double max) {
        this.doubleMin = min;
        this.doubleMax = max;
        return this;
    }

    /** Границы для полей {@code int} в {@link #bind}. По умолчанию 0 … {@code Integer.MAX_VALUE}. */
    public SettingsBinder<S> intRange(int min, int max) {
        this.intMin = min;
        this.intMax = max;
        return this;
    }

    /** Границы для полей {@code long}. По умолчанию 0 … {@code Long.MAX_VALUE}. */
    public SettingsBinder<S> longRange(long min, long max) {
        this.longMin = min;
        this.longMax = max;
        return this;
    }

    // ------------------------------------------------------------------ разделы

    public SettingsBinder<S> push(String section) {
        builder.push(section);
        return this;
    }

    public SettingsBinder<S> push(String section, String comment) {
        builder.comment(comment).push(section);
        return this;
    }

    public SettingsBinder<S> pop() {
        builder.pop();
        return this;
    }

    /** Конфиг, в который идёт привязка: для обычных значений рядом с привязанными. */
    public ForgeConfigSpec.Builder builder() {
        return builder;
    }

    // ------------------------------------------------------------------ привязка

    /**
     * Поле → ключ. Тип берётся у поля: {@code double}, {@code int}, {@code long} (границы привязчика),
     * {@code boolean}, {@code List<String>} (может быть пустым).
     */
    public SettingsBinder<S> bind(String field, String comment) {
        bindField(field(field), comment);
        return this;
    }

    /** Поле {@code int} со своими границами. */
    public SettingsBinder<S> bindInt(String field, String comment, int min, int max) {
        Field f = field(field);
        ForgeConfigSpec.IntValue v = builder.comment(comment).defineInRange(snake(field), (int) (Integer) read(f, defaults), min, max);
        simple(f, v);
        return this;
    }

    /** Поле {@code double} со своими границами. */
    public SettingsBinder<S> bindDouble(String field, String comment, double min, double max) {
        Field f = field(field);
        ForgeConfigSpec.DoubleValue v = builder.comment(comment).defineInRange(snake(field), (double) (Double) read(f, defaults), min, max);
        simple(f, v);
        return this;
    }

    /**
     * Поле {@code EnumMap<E, Double>}: раздел с именем поля и описанием, в нём по ключу на каждое значение
     * перечисления (без описаний, границы — как у {@code double}). Таблица в объекте настроек должна уже
     * существовать: значения кладутся в неё.
     *
     * @param values какие значения и в каком порядке идут в файл
     * @param key    имя ключа для значения
     */
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> SettingsBinder<S> bindMap(String field, String comment, Iterable<E> values, Function<E, String> key) {
        Field f = field(field);
        EnumMap<E, Double> def = (EnumMap<E, Double>) read(f, defaults);
        builder.comment(comment).push(snake(field));
        for (E e : values) {
            ForgeConfigSpec.DoubleValue v = builder.defineInRange(key.apply(e), def.get(e), doubleMin, doubleMax);
            appliers.add(s -> ((EnumMap<E, Double>) read(f, s)).put(e, v.get()));
        }
        builder.pop();
        return this;
    }

    /**
     * Поле {@code double[]} — список чисел той же длины, что по умолчанию. Лишние числа в файле отбрасываются,
     * недостающие берутся из значений по умолчанию.
     */
    public SettingsBinder<S> bindArray(String field, String comment) {
        Field f = field(field);
        double[] def = (double[]) read(f, defaults);
        List<Double> defList = new ArrayList<>();
        for (double d : def) defList.add(d);
        ForgeConfigSpec.ConfigValue<List<? extends Double>> v = builder.comment(comment)
                .defineList(snake(field), defList, o -> o instanceof Number);
        appliers.add(s -> {
            List<? extends Number> list = v.get();
            double[] arr = def.clone();
            for (int i = 0; i < arr.length && i < list.size(); i++) arr[i] = list.get(i).doubleValue();
            write(f, s, arr);
        });
        return this;
    }

    /**
     * Привязать все поля с пометкой: раздел и описание берутся из пометки. Разделы идут в порядке первого
     * появления, поля внутри раздела — в порядке объявления. Поле без пометки и статическое поле пропускаются;
     * пустое описание — ключ без описания.
     *
     * @param annotation пометка мода на полях настроек (с {@code RetentionPolicy.RUNTIME})
     * @param section    раздел из пометки
     * @param comment    описание из пометки — как оно пойдёт в файл
     */
    public <A extends Annotation> SettingsBinder<S> bindAnnotated(Class<A> annotation, Function<A, String> section,
                                                                 Function<A, String> comment) {
        Map<String, List<Field>> sections = new LinkedHashMap<>();
        for (Field f : type.getFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            A a = f.getAnnotation(annotation);
            if (a != null) sections.computeIfAbsent(section.apply(a), k -> new ArrayList<>()).add(f);
        }
        for (Map.Entry<String, List<Field>> e : sections.entrySet()) {
            builder.push(e.getKey());
            for (Field f : e.getValue()) {
                String doc = comment.apply(f.getAnnotation(annotation));
                bindField(f, doc == null || doc.isEmpty() ? null : doc);
            }
            builder.pop();
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private void bindField(Field f, @Nullable String comment) {
        String key = snake(f.getName());
        Object def = read(f, defaults);
        Class<?> t = f.getType();
        if (comment != null) builder.comment(comment);
        if (t == double.class) {
            simple(f, builder.defineInRange(key, (double) (Double) def, doubleMin, doubleMax));
        } else if (t == boolean.class) {
            simple(f, builder.define(key, (boolean) (Boolean) def));
        } else if (t == int.class) {
            simple(f, builder.defineInRange(key, (int) (Integer) def, intMin, intMax));
        } else if (t == long.class) {
            simple(f, builder.defineInRange(key, (long) (Long) def, longMin, longMax));
        } else if (t == List.class) {
            List<String> list = (List<String>) def;
            ForgeConfigSpec.ConfigValue<List<? extends String>> v =
                    builder.defineListAllowEmpty(List.of(key), () -> list, o -> o instanceof String);
            appliers.add(s -> write(f, s, List.copyOf(v.get())));
        } else {
            throw new IllegalStateException("Настройка " + type.getSimpleName() + "." + f.getName() + ": тип " + t.getSimpleName()
                    + " не привязывается (double, int, long, boolean, List<String>; для остального — bindMap, bindArray)");
        }
    }

    private void simple(Field f, ForgeConfigSpec.ConfigValue<?> v) {
        simple.put(f.getName(), new Bound(f, v));
        appliers.add(s -> write(f, s, v.get()));
    }

    private Field field(String name) {
        try {
            Field f = type.getField(name);
            if (Modifier.isStatic(f.getModifiers())) throw new IllegalStateException("Настройка " + type.getSimpleName() + "." + name + " — статическое поле");
            if (bound.contains(name)) throw new IllegalStateException("Настройка " + type.getSimpleName() + "." + name + " привязана дважды");
            bound.add(name);
            return f;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Нет поля настроек " + type.getSimpleName() + "." + name, e);
        }
    }

    private static Object read(Field f, Object from) {
        try {
            return f.get(from);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Настройка " + f.getName() + " не читается", e);
        }
    }

    private static void write(Field f, Object to, Object value) {
        try {
            f.set(to, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Настройка " + f.getName() + " не записывается", e);
        }
    }

    // ------------------------------------------------------------------ итог

    /** Закончить описание. Вызывать один раз, после всех привязок и обычных значений. */
    public ForgeConfigSpec build() {
        if (spec != null) throw new IllegalStateException("Конфиг настроек " + type.getSimpleName() + " уже собран");
        spec = builder.build();
        return spec;
    }

    /** Собранный конфиг (после {@link #build()}). */
    public ForgeConfigSpec spec() {
        if (spec == null) throw new IllegalStateException("Конфиг настроек " + type.getSimpleName() + " ещё не собран: вызови build()");
        return spec;
    }

    /** Конфиг собран и файл загружен — значения можно читать. */
    public boolean loaded() {
        return spec != null && spec.isLoaded();
    }

    /** Объект настроек со значениями по умолчанию (как в классе настроек). Не менять. */
    public S defaults() {
        return defaults;
    }

    /**
     * Новый объект настроек со значениями из конфига. Вызывать, когда конфиг загружен или перечитан
     * ({@link #loaded()}); подменить им текущие настройки мод должен сам.
     */
    public S bake() {
        S s = factory.get();
        for (Consumer<S> a : appliers) a.accept(s);
        return s;
    }

    /**
     * Поменять значение на ходу (команда): записать в конфиг и сохранить файл. После этого мод вызывает
     * {@link #bake()} сам. Работает для полей {@code double}, {@code int}, {@code long} (значение — число)
     * и {@code boolean}.
     *
     * @return false — такого поля среди привязанных нет или значение не того типа
     */
    @SuppressWarnings("unchecked")
    public boolean set(String field, Object value) {
        Bound b = simple.get(field);
        if (b == null) return false;
        Class<?> t = b.field().getType();
        Object v;
        if (t == boolean.class) {
            if (!(value instanceof Boolean)) return false;
            v = value;
        } else {
            if (!(value instanceof Number n)) return false;
            v = t == double.class ? (Object) n.doubleValue() : t == int.class ? (Object) n.intValue() : (Object) n.longValue();
        }
        ((ForgeConfigSpec.ConfigValue<Object>) b.value()).set(v);
        spec().save();
        return true;
    }

    /** Имя ключа по имени поля: каждая заглавная буква → «_» и строчная ({@code maxWoundsPerPart} → {@code max_wounds_per_part}). */
    public static String snake(String camel) {
        StringBuilder sb = new StringBuilder();
        for (char c : camel.toCharArray()) {
            if (Character.isUpperCase(c)) sb.append('_').append(Character.toLowerCase(c));
            else sb.append(c);
        }
        return sb.toString();
    }
}
