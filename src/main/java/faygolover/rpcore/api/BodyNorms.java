package faygolover.rpcore.api;

/**
 * Общие формулы тела, чтобы все моды семейства считали одинаково (дизайн — RP Stamina Sports, docs/06 п. 3).
 * <ul>
 *   <li>объём крови по Надлеру — от пола, роста и веса;</li>
 *   <li>нормы жира — от пола и возраста; по ним — «истощение / ниже нормы / норма / выше нормы / ожирение»;</li>
 *   <li>множители возраста: выносливость, заживление, рост Силы и Телосложения.</li>
 * </ul>
 *
 * @since 0.5.0
 */
public final class BodyNorms {
    private BodyNorms() {}

    /** Тело относительно нормы своего пола и возраста. */
    public enum BodyState {
        /** Сильно ниже нормы (ИМТ ниже 16 или жира меньше половины нижней границы). */
        EXHAUSTED,
        UNDER,
        NORMAL,
        OVER,
        OBESE
    }

    /**
     * Границы жира, %: ниже {@code low} — ниже нормы; до {@code high} — норма; до {@code obese} — выше нормы; дальше —
     * ожирение.
     */
    public record FatNorm(double low, double high, double obese) {}

    /** Объём крови в норме, мл (формула Надлера). Пол не указан — среднее мужской и женской. */
    public static double bloodVolumeMl(BodyProfile.Sex sex, double heightCm, double weightKg) {
        double h = heightCm / 100.0, h3 = h * h * h;
        double male = 0.3669 * h3 + 0.03219 * weightKg + 0.6041;
        double female = 0.3561 * h3 + 0.03308 * weightKg + 0.1833;
        double litres = switch (sex) {
            case MALE -> male;
            case FEMALE -> female;
            case NONE -> (male + female) / 2;
        };
        return Math.max(1.0, litres) * 1000.0;
    }

    /** Нормы жира по полу и возрасту. После 40 лет границы +2, после 60 — +4. */
    public static FatNorm fatNorm(BodyProfile.Sex sex, int age) {
        double shift = age >= 60 ? 4 : age >= 40 ? 2 : 0;
        return switch (sex) {
            case MALE -> new FatNorm(8 + shift, 20 + shift, 25 + shift);
            case FEMALE -> new FatNorm(20 + shift, 32 + shift, 38 + shift);
            case NONE -> new FatNorm(14 + shift, 26 + shift, 31.5 + shift);
        };
    }

    public static double bmi(double heightCm, double weightKg) {
        double h = Math.max(1.0, heightCm / 100.0);
        return weightKg / (h * h);
    }

    /** Тело относительно нормы. Вес или жир неизвестны (NaN) — норма. */
    public static BodyState state(BodyProfile p, double weightKg, double fatPercent) {
        if (!Double.isFinite(weightKg) || !Double.isFinite(fatPercent)) return BodyState.NORMAL;
        FatNorm n = fatNorm(p.sex(), p.age());
        double bmi = bmi(p.heightCm(), weightKg);
        if (bmi < 16 || fatPercent < n.low() / 2) return BodyState.EXHAUSTED;
        if (fatPercent < n.low() || bmi < 17.5) return BodyState.UNDER;
        if (fatPercent >= n.obese()) return BodyState.OBESE;
        if (fatPercent >= n.high()) return BodyState.OVER;
        return BodyState.NORMAL;
    }

    /** Возраст: множитель запаса и восстановления выносливости. */
    public static double ageStamina(int age) {
        if (age < 18) return 0.9;
        if (age <= 30) return 1.0;
        if (age <= 45) return 0.95;
        if (age <= 60) return 0.85;
        return 0.7;
    }

    /** Возраст: множитель заживления. */
    public static double ageHealing(int age) {
        if (age < 18) return 1.1;
        if (age <= 30) return 1.0;
        if (age <= 45) return 0.9;
        if (age <= 60) return 0.8;
        return 0.7;
    }

    /** Возраст: множитель роста Силы и Телосложения. Умственные навыки от возраста не зависят. */
    public static double agePhysicalLearning(int age) {
        if (age < 18) return 1.1;
        if (age <= 30) return 1.0;
        if (age <= 45) return 0.9;
        if (age <= 60) return 0.75;
        return 0.6;
    }
}
