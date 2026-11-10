package faygolover.rpcore.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * ГМ обнулил персонажа (сервер): каждый мод стирает своё, как у нового игрока. Вещи и КПК не трогаем. Обычная смерть
 * этого события не шлёт.
 * @since 0.14.0
 */
public class RpCharacterWipeEvent extends PlayerEvent {
    public RpCharacterWipeEvent(Player player) {
        super(player);
    }
}
