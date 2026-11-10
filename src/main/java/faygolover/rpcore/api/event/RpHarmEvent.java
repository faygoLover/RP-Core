package faygolover.rpcore.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Игрок своей рукой довёл другого игрока до нокдауна или до клинической смерти и смерти (сервер). Шлёт мод тела, что
 * из этого следует — решает мод психики. Игрок события — тот, кто ударил.
 * @since 0.14.0
 */
public class RpHarmEvent extends PlayerEvent {
    public enum Stage { KNOCKDOWN, LETHAL }

    private final ServerPlayer victim;
    private final Stage stage;

    public RpHarmEvent(ServerPlayer attacker, ServerPlayer victim, Stage stage) {
        super(attacker);
        this.victim = victim;
        this.stage = stage;
    }

    public ServerPlayer victim() {
        return victim;
    }

    public Stage stage() {
        return stage;
    }
}
