package faygolover.rpcore.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * Анкета тела: пол, возраст, рост. Заполняет игрок при создании персонажа (один раз) или ГМ ({@code /rpcore profile}).
 * Вес и жир здесь не хранятся — их ведёт мод питания (RP Culinary) или мод тела (RP Medicine).
 * Читают все моды семейства: объём крови, нормы веса, возраст для выносливости и заживления — {@link BodyNorms}.
 *
 * @param filled анкета заполнена (игрок больше не может менять её сам, только ГМ)
 * @since 0.5.0
 */
public record BodyProfile(Sex sex, int age, double heightCm, boolean filled) {
    public static final int MIN_AGE = 16, MAX_AGE = 90, DEFAULT_AGE = 25;
    public static final double MIN_HEIGHT = 140, MAX_HEIGHT = 220, DEFAULT_HEIGHT = 175;
    /** Пока анкеты нет: пол не указан, 25 лет, 175 см. */
    public static final BodyProfile DEFAULT = new BodyProfile(Sex.NONE, DEFAULT_AGE, DEFAULT_HEIGHT, false);

    public enum Sex {
        /** Не указан: нормы — средние между мужскими и женскими. */
        NONE,
        MALE,
        FEMALE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Sex byId(String id) {
            for (Sex s : values()) if (s.id().equalsIgnoreCase(id)) return s;
            return NONE;
        }
    }

    public BodyProfile {
        if (sex == null) sex = Sex.NONE;
        age = Mth.clamp(age, MIN_AGE, MAX_AGE);
        heightCm = Double.isFinite(heightCm) ? Mth.clamp(heightCm, MIN_HEIGHT, MAX_HEIGHT) : DEFAULT_HEIGHT;
    }

    public BodyProfile withSex(Sex s) {
        return new BodyProfile(s, age, heightCm, filled);
    }

    public BodyProfile withAge(int a) {
        return new BodyProfile(sex, a, heightCm, filled);
    }

    public BodyProfile withHeight(double h) {
        return new BodyProfile(sex, age, h, filled);
    }

    public BodyProfile withFilled(boolean f) {
        return new BodyProfile(sex, age, heightCm, f);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("sex", sex.id());
        t.putInt("age", age);
        t.putDouble("height", heightCm);
        t.putBoolean("filled", filled);
        return t;
    }

    public static BodyProfile load(CompoundTag t) {
        if (t == null || t.isEmpty()) return DEFAULT;
        return new BodyProfile(Sex.byId(t.getString("sex")), t.contains("age") ? t.getInt("age") : DEFAULT_AGE,
                t.contains("height") ? t.getDouble("height") : DEFAULT_HEIGHT, t.getBoolean("filled"));
    }
}
