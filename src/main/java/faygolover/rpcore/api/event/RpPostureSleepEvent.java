package faygolover.rpcore.api.event;

import faygolover.rpcore.api.RpCoreAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;
import org.jetbrains.annotations.Nullable;

/**
 * Игрок пытается уснуть сидя или лёжа ({@code /sleep}). Сервер, {@code MinecraftForge.EVENT_BUS}. Отменить — не уснёт
 * (бодр, кофеин, сильная боль); {@link #deny} заодно задаёт, что ему сказать. Кровати — ванильный {@code PlayerSleepInBedEvent}.
 *
 * @since 0.11.0
 */
@Cancelable
public class RpPostureSleepEvent extends PlayerEvent {
    private final RpCoreAPI.Posture posture;
    @Nullable
    private Component reason;

    public RpPostureSleepEvent(Player player, RpCoreAPI.Posture posture) {
        super(player);
        this.posture = posture;
    }

    public RpCoreAPI.Posture posture() {
        return posture;
    }

    public void deny(Component reason) {
        this.reason = reason;
        setCanceled(true);
    }

    @Nullable
    public Component reason() {
        return reason;
    }
}
