package faygolover.rpcore.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Прокрутка времени RP для игрока ({@code /rptime sleep|skip}, сервер). Мир не меняется: каждый мод сам
 * прокручивает своё — питание тратит голод и воду, мод тела заживляет раны, выносливость восполняется.
 * Слушать на {@code MinecraftForge.EVENT_BUS}.
 *
 * @since 0.4.0
 */
public class RpTimeSkipEvent extends PlayerEvent {
    /** Что прокручивать: всё, только раны или только голод. */
    public enum Scope { ALL, WOUNDS, FOOD }

    private final long seconds;
    private final boolean sleeping;
    private final Scope scope;

    public RpTimeSkipEvent(Player player, long seconds, boolean sleeping, Scope scope) {
        super(player);
        this.seconds = seconds;
        this.sleeping = sleeping;
        this.scope = scope;
    }

    /** Сколько секунд реального времени прошло. */
    public long seconds() {
        return seconds;
    }

    public double hours() {
        return seconds / 3600.0;
    }

    /** Во сне ({@code sleep}): голод тратится медленнее, раны заживают как во сне. Иначе — бодрствуя ({@code skip}). */
    public boolean sleeping() {
        return sleeping;
    }

    public Scope scope() {
        return scope;
    }

    /** Прокручивать ли голод, воду, вес. */
    public boolean food() {
        return scope != Scope.WOUNDS;
    }

    /** Прокручивать ли раны и таймеры тела. */
    public boolean wounds() {
        return scope != Scope.FOOD;
    }
}
