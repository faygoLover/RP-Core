package faygolover.rpcore.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * Мод, который ведёт психику игрока (сейчас — RP Psyche): через него другие моды семейства давят на стресс и
 * рассудок, не зная о нём. Регистрирует — {@link RpCoreAPI#registerMind}; вызывают — {@link RpCoreAPI#stressImpulse}
 * и {@link RpCoreAPI#sanityHit}. Читать стресс и рассудок — {@link RpCoreAPI#stress} и {@link RpCoreAPI#sanity}.
 * <p>
 * Кого психика не касается (креатив, наблюдатель, пауза ГМа), решает сам мод психики. Только сервер.
 *
 * @since 0.15.0
 */
public interface MindProvider {
    /** Разовый толчок стресса на {@code amount} единиц (шкала 0–100): войдёт в стресс на ближайшем шаге. */
    void stressImpulse(ServerPlayer player, double amount);

    /** Разовый удар по рассудку: минус {@code amount} единиц (шкала 0–100). */
    void sanityHit(ServerPlayer player, double amount);
}
