package faygolover.rpcore.api.event;

import faygolover.rpcore.api.BodyProfile;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Анкета тела игрока изменилась (игрок заполнил её или ГМ поправил). Сервер. Мод тела пересчитывает объём крови,
 * мод питания — нормы веса. Слушать на {@code MinecraftForge.EVENT_BUS}.
 *
 * @since 0.5.0
 */
public class BodyProfileChangedEvent extends PlayerEvent {
    private final BodyProfile before;
    private final BodyProfile after;

    public BodyProfileChangedEvent(Player player, BodyProfile before, BodyProfile after) {
        super(player);
        this.before = before;
        this.after = after;
    }

    public BodyProfile before() {
        return before;
    }

    public BodyProfile after() {
        return after;
    }
}
