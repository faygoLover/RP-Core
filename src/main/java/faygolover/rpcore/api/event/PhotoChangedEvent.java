package faygolover.rpcore.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Сервер принял новое фото игрока ({@link faygolover.rpcore.api.Photos}). Клиентам Core сам рассылает, что
 * кэш устарел; событие — для модов, которым надо что-то обновить у себя на сервере.
 *
 * @since 0.4.0
 */
public class PhotoChangedEvent extends PlayerEvent {
    public PhotoChangedEvent(Player player) {
        super(player);
    }
}
