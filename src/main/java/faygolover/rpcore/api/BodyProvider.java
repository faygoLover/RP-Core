package faygolover.rpcore.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Мод, который ведёт тело игрока (сейчас — RP Medicine). Когда он стоит, другие моды семейства не убирают свои
 * механики, а переводят их на него: перк «Живучий» — через черту, «Бешенство ботаника» — от состояния тела,
 * «Неудачливый» подворачивает ногу настоящей травмой, кофеиновая ломка — физиологией веществ.
 * Регистрирует — {@link RpCoreAPI#registerBody}. Методы вызываются на сервере.
 * <p>
 * Новые вызовы — с ответом «не умею» по умолчанию (false, 1, true): вызывающий мод тогда обходится простой версией.
 * Что мод тела ведёт на самом деле — {@link #handles} ({@link RpIds#BODY_BRUISES}, {@link RpIds#BODY_MUSCLES}…).
 */
public interface BodyProvider {
    /** Ведёт ли мод эту часть физиологии ({@link RpIds#BODY_HEALTH}, {@link RpIds#BODY_CAFFEINE}…). */
    boolean handles(ResourceLocation feature);

    /** Общее состояние, 0–1 (1 — здоров). Вместо ванильной доли здоровья. */
    float condition(Player player);

    /**
     * Человек без сознания (нокдаун и т. п.): не говорит, не пользуется КПК. По умолчанию — в сознании.
     * @since 0.3.3
     */
    default boolean unconscious(Player player) {
        return false;
    }

    /** Неудачное падение: подвернуть ногу настоящей травмой. true — сделано. */
    default boolean sprain(Player player) {
        return false;
    }

    /** Обморок (голодный, от жажды) на {@code seconds} секунд. true — сделано; иначе вызывающий обходится сам. */
    default boolean faint(Player player, int seconds) {
        return false;
    }

    /** Вес тела, кг, или NaN — мод тела его не ведёт (первое знакомство мода питания с игроком). */
    default double weightKg(Player player) {
        return Double.NaN;
    }

    /** Доля жира, %, или NaN. */
    default double bodyFatPercent(Player player) {
        return Double.NaN;
    }

    /**
     * Ушиб части тела ({@link RpIds#PARTS}): уронил гантель на ногу, упал с турника, груша по голове.
     * {@code severity} 0–1 (1 — сильный ушиб). true — сделано.
     * @since 0.5.0
     */
    default boolean bruise(Player player, ResourceLocation part, double severity) {
        return false;
    }

    /**
     * Перегрузка мышц части тела: 1 — боль в мышцах, 2 — растяжение, 3 — надрыв. true — сделано.
     * @since 0.5.0
     */
    default boolean strain(Player player, ResourceLocation part, int stage) {
        return false;
    }

    /**
     * Можно ли сейчас нагружать часть тела (тренировка): нельзя при переломе, вывихе, сильной боли, без конечности.
     * @since 0.5.0
     */
    default boolean canExert(Player player, ResourceLocation part) {
        return true;
    }

    /**
     * Насколько хорошо человек сейчас учится, 0–1 (боль, обморок): множитель опыта всех навыков.
     * @since 0.5.0
     */
    default double learningFactor(Player player) {
        return 1.0;
    }

    /**
     * Последствие питания и прочих долгих состояний ({@link RpIds#COND_OVERFILL}, {@link RpIds#COND_SCURVY}…).
     * {@code severity} 0–1; 0 — состояние прошло. Вызывающий шлёт его заново, пока оно длится (не чаще раза в
     * секунду), мод тела сам решает, что человек чувствует и что видно при осмотре. true — мод это ведёт.
     * @since 0.5.0
     */
    default boolean condition(Player player, ResourceLocation id, double severity) {
        return false;
    }

    /**
     * Показать игроку ощущение (ключ перевода, например {@code rpculinary.sense.heavy}) так же, как свои — текстом
     * над панелью. true — показано; иначе вызывающий показывает сам.
     * @since 0.5.0
     */
    default boolean sensation(Player player, String key) {
        return false;
    }

    /**
     * Снотворное в крови: 0 — нет; больше 0 — действует (сила, 1 — одна доза). RP Needs: уснуть можно при любой
     * бодрости, сон крепкий. @since 0.11.0
     */
    default double sleepAid(Player player) {
        return 0;
    }

    /** Лежит на больничной койке (RP Medicine): для сна — как кровать, засыпать через {@code /sleep}. @since 0.11.0 */
    default boolean inHospitalBed(Player player) {
        return false;
    }

    /** Боль 0–100 (что чувствует, с обезболиванием) и седация 0–100 (сила успокоительного). @since 0.11.0 */
    default double pain(Player player) {
        return 0;
    }

    default double sedation(Player player) {
        return 0;
    }

    /** Руки работают: не связан, не зафиксирован, не лежит, есть хотя бы одна рабочая. @since 0.14.0 */
    default boolean handsFree(Player player) {
        return true;
    }

    /** Опьянение 0–100. @since 0.14.0 */
    default double intoxication(Player player) {
        return 0;
    }

    /** Действует стимулятор: уснуть нельзя, как при кофеине. @since 0.14.0 */
    default boolean stimulated(Player player) {
        return false;
    }

    /** Температура среды за пределами нормы (минус — холод, 0 — норма или не ведётся). @since 0.14.0 */
    default double temperatureOffset(Player player) {
        return 0;
    }

    /** Чьё это тело вышедшего игрока, или null — это не тело. @since 0.14.0 */
    @org.jetbrains.annotations.Nullable
    default java.util.UUID bodyOwner(net.minecraft.world.entity.Entity entity) {
        return null;
    }
}
