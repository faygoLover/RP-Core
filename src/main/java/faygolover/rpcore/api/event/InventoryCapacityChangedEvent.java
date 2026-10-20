package faygolover.rpcore.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Ёмкость инвентаря игрока изменилась (открылись, закрылись, заморозились или оттаяли ячейки). Посылает мод
 * ограничения ({@link faygolover.rpcore.api.InventoryProvider}) на сервере после пересчёта — когда вещи из снятых
 * ячеек уже разложены. Слушать на {@code MinecraftForge.EVENT_BUS}.
 *
 * @since 0.7.0
 */
public class InventoryCapacityChangedEvent extends PlayerEvent {
    public InventoryCapacityChangedEvent(Player player) {
        super(player);
    }
}
