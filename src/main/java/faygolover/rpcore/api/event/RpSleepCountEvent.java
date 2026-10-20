package faygolover.rpcore.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Как считать игрока, когда решается, ускорять ли ночь (RP Time: ночь идёт быстрее, только когда спят все). Сервер,
 * {@code MinecraftForge.EVENT_BUS}. RP Time ставит начальное значение по ванилле (в кровати — {@link Count#SLEEPING},
 * иначе — {@link Count#BLOCKS}); моды поправляют: RP Medicine — больничная койка спит, без сознания держит ночь,
 * клиническая смерть не считается. ГМов (творческий, наблюдатель) и мёртвых RP Time не спрашивает — их не считает.
 *
 * @since 0.7.2
 */
public class RpSleepCountEvent extends PlayerEvent {
    public enum Count {
        /** Спит — ночь можно ускорять. */
        SLEEPING,
        /** Не спит — пока он так, ночь идёт обычным ходом. */
        BLOCKS,
        /** Не считается вовсе. */
        IGNORED
    }

    private Count count;

    public RpSleepCountEvent(Player player, Count initial) {
        super(player);
        this.count = initial;
    }

    public Count count() {
        return count;
    }

    public void setCount(Count value) {
        if (value != null) count = value;
    }
}
