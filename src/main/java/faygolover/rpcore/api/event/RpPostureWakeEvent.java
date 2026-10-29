package faygolover.rpcore.api.event;

import faygolover.rpcore.api.RpCoreAPI;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Игрок проснулся после сна сидя или лёжа ({@code /sleep}): сам, от удара, встал или вышел из игры. Сервер,
 * {@code MinecraftForge.EVENT_BUS}. {@link #seconds} — сколько реальных секунд проспал. RP Medicine по нему может дать
 * «отлежал»; RP Needs считает сон сам, пока {@link RpCoreAPI#postureSleeping} истинно.
 *
 * @since 0.11.0
 */
public class RpPostureWakeEvent extends PlayerEvent {
    private final RpCoreAPI.Posture posture;
    private final long seconds;

    public RpPostureWakeEvent(Player player, RpCoreAPI.Posture posture, long seconds) {
        super(player);
        this.posture = posture;
        this.seconds = seconds;
    }

    public RpCoreAPI.Posture posture() {
        return posture;
    }

    public long seconds() {
        return seconds;
    }
}
