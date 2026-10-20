package faygolover.rpcore.api;

import net.minecraft.world.entity.player.Player;

/**
 * Источник поправок к инвентарю: особенности (RP Perks), тело (RP Medicine) и т.п. Регистрируется в Core
 * ({@link RpCoreAPI#registerInventoryModifier}); мод ограничения ({@link InventoryProvider}) спрашивает все источники
 * при каждом пересчёте (раз в секунду и по {@link RpCoreAPI#inventoryChanged}) и складывает. Источники о моде
 * ограничения не знают; без него их никто не спрашивает.
 * <p>
 * Вызывается на сервере. Должен быть быстрым и без побочных эффектов: только записать поправки в {@code changes}.
 *
 * @since 0.7.0
 */
@FunctionalInterface
public interface InventoryModifier {
    void apply(Player player, Changes changes);

    /** Куда источник пишет поправки. Порядок сложения: база + все прибавки, потом все ограничения (меньшее), заморозка. */
    interface Changes {
        /** Прибавить ячейки части (отрицательное — убрать). Итог не выходит за 0…{@link InventoryProvider.Part#size}. */
        void add(InventoryProvider.Part part, int amount);

        /** Не больше {@code max} ячеек части, сколько бы ни прибавили (ампутация обеих рук → хотбар 1). */
        void limit(InventoryProvider.Part part, int max);

        /** Убрать часть совсем («снята»): вещи из неё переедут в открытые ячейки или выпадут. */
        default void close(InventoryProvider.Part part) {
            limit(part, 0);
        }

        /** Заморозить все ячейки части: вещи остаются, трогать нельзя (перелом, нокдаун, путы). */
        void freeze(InventoryProvider.Part part);

        /** Прибавить ячейки типа Curios (отрицательное — убрать). */
        void addCurios(String type, int amount);

        /** Не больше {@code max} ячеек типа Curios. */
        void limitCurios(String type, int max);
    }
}
