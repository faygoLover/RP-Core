package faygolover.rpcore.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Мод выносливости (RP Stamina). Розетка — только вопросы к нему; траты, восстановление, броня, предметы и HUD
 * остаются в самом моде. Без него розетка пустая ({@link RpCoreAPI#stamina()} — null), и моды обходятся без
 * выносливости. Регистрирует — {@link RpCoreAPI#registerStamina}. Методы — на сервере.
 * <p>
 * Прибавки к выносливости (перки, голод, раны) моды вешают обычными модификаторами атрибутов на атрибут из
 * {@link #attribute}: так все прибавки складываются в одном месте, и никто не ищет чужой атрибут по имени.
 *
 * @since 0.4.0
 */
public interface StaminaProvider {
    /** Характеристики выносливости, к которым другие моды добавляют свои модификаторы. */
    enum Stat {
        /** Максимум (обычный человек — 1000). */
        MAX,
        /** Множитель восстановления (1 — обычное). */
        REGEN,
        /** Общий множитель расхода (1 — обычный). */
        COST,
        /** Множитель расхода на бег, прыжки, лестницы. */
        MOVE_COST,
        /** Множитель расхода на плавание. */
        SWIM_COST,
        /** Множитель расхода на добычу блоков. */
        MINE_COST,
        /** Множитель расхода на удары в ближнем бою. */
        MELEE_COST,
        /** Насколько мешает вес брони (1 — обычно, 0 — не мешает). */
        ARMOR_BURDEN
    }

    /** Сколько выносливости сейчас. Если механика на игрока не действует (креатив) — максимум. */
    float stamina(Player player);

    /** Максимум выносливости. */
    float maxStamina(Player player);

    /** Измотан ли: выносливость падала до нуля и ещё не поднялась настолько, чтобы снова бегать. */
    boolean exhausted(Player player);

    /**
     * Потратить {@code amount} выносливости через общий порядок мода (множители, событие траты, измотанность).
     * {@code source} — кто тратит (для событий и отладки).
     */
    void spend(Player player, float amount, ResourceLocation source);

    /**
     * Выдать {@code amount} выносливости сразу (стимуляторы, еда, адреналин): без множителей трат, не выше максимума.
     * {@code source} — кто выдаёт (для отладки). Мод выносливости до 0.4.2 этого не умеет — тогда ничего не происходит.
     *
     * @since 0.4.2
     */
    default void restore(Player player, float amount, ResourceLocation source) {}

    /** Атрибут характеристики (для модификаторов) или null, если мод её не ведёт. */
    @Nullable
    Attribute attribute(Stat stat);
}
