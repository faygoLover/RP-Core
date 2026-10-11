package faygolover.rpcore.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Мод, который ведёт тело игрока (сейчас — RP Medicine). Когда он стоит, другие моды семейства не убирают свои
 * механики, а переводят их на него: перк «Живучий» — через черту, «Бешенство ботаника» — от состояния тела,
 * «Неудачливый» подворачивает ногу настоящей травмой, кофеиновая ломка — физиологией веществ.
 * Регистрирует — {@link RpCoreAPI#registerBody}. Методы вызываются на сервере.
 */
public interface BodyProvider {
    /** Ведёт ли мод эту часть физиологии ({@link RpIds#BODY_HEALTH}, {@link RpIds#BODY_CAFFEINE}…). */
    boolean handles(ResourceLocation feature);

    /** Общее состояние, 0–1 (1 — здоров). Вместо ванильной доли здоровья. */
    float condition(Player player);

    /** Неудачное падение: подвернуть ногу настоящей травмой. true — сделано. */
    /**
     * Человек без сознания (нокдаун и т. п.): не говорит, не пользуется КПК. По умолчанию — в сознании.
     * @since 0.3.3
     */
    default boolean unconscious(Player player) {
        return false;
    }

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
}
