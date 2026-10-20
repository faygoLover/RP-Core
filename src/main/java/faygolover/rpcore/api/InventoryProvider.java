package faygolover.rpcore.api;

import net.minecraft.world.entity.player.Player;

/**
 * Мод ограничения инвентаря (RP Inventory). Розетка — вопросы к нему: какие ячейки инвентаря игрока есть и какие
 * заморожены. Без него розетка пустая ({@link RpCoreAPI#inventory()} — null) — открыто всё. Регистрирует —
 * {@link RpCoreAPI#registerInventory}.
 * <p>
 * Индексы — как в ванильном {@code Inventory}: хотбар 0–8, хранилище 9–35, броня 36–39 (ступни, ноги, грудь, голова),
 * вторая рука 40.
 * <ul>
 *   <li><b>Открыта</b> — ячейка есть у игрока. Закрытой («снятой») ячейки не видно, вещей в ней не бывает.</li>
 *   <li><b>Заморожена</b> — ячейка открыта и вещь в ней остаётся, но взять, положить, выбрать нельзя (рука не
 *   работает). Прошло — ячейка как была.</li>
 * </ul>
 * Сервер — для любого игрока; клиент — для любого игрока, данные которого пришли с сервера (свой — всегда).
 * Прибавки и ограничения другие моды дают через {@link InventoryModifier} ({@link RpCoreAPI#registerInventoryModifier}).
 *
 * @since 0.7.0
 */
public interface InventoryProvider {
    /** Части инвентаря, у каждой — своя ёмкость. */
    enum Part {
        /** Хотбар, ячейки 0–8 (открываются с 0). */
        HOTBAR(0, 9),
        /** Хранилище, ячейки 9–35 (открываются по порядку: слева направо, сверху вниз). */
        STORAGE(9, 27),
        /** Броня ступней, ячейка 36. */
        FEET(36, 1),
        /** Броня ног, ячейка 37. */
        LEGS(37, 1),
        /** Броня груди, ячейка 38. */
        CHEST(38, 1),
        /** Шлем, ячейка 39. */
        HEAD(39, 1),
        /** Вторая рука, ячейка 40. */
        OFFHAND(40, 1);

        public final int first;
        public final int size;

        Part(int first, int size) {
            this.first = first;
            this.size = size;
        }

        /** Часть, к которой относится ячейка, или null (индекс вне 0–40). */
        public static Part of(int index) {
            for (Part p : values()) if (index >= p.first && index < p.first + p.size) return p;
            return null;
        }
    }

    /** Есть ли у игрока эта ячейка (замороженная — тоже есть). Индекс вне 0–40 — открыт. */
    boolean isSlotOpen(Player player, int index);

    /** Заморожена ли ячейка: вещь остаётся, но трогать нельзя. */
    boolean isSlotFrozen(Player player, int index);

    /** Сколько ячеек части открыто сейчас (0…{@link Part#size}). */
    int capacity(Player player, Part part);

    /**
     * Сколько ячеек Curios этого типа оставляет мод ограничения, или -1 — тип не ограничен (как задали моды Curios).
     */
    default int curiosLimit(Player player, String type) {
        return -1;
    }

    /** Действуют ли ограничения на игрока (ГМ, творческий режим — нет). */
    default boolean limited(Player player) {
        return true;
    }

    /**
     * Пересчитать ёмкость игрока в ближайший тик (сервер): источник модификаторов сообщает, что у него что-то
     * поменялось. Мод и сам пересчитывает раз в секунду — звать не обязательно, но так быстрее.
     */
    default void refresh(Player player) {}
}
