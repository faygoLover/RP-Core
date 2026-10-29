package faygolover.rpcore.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Прокрутка времени RP для игрока ({@code /rptime add|set|sleep|player}, ускоренная ночь RP Time; сервер). Мир не меняется: каждый мод сам
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

    private final double quality;
    private final boolean wakeAfter;

    public RpTimeSkipEvent(Player player, long seconds, boolean sleeping, Scope scope) {
        this(player, seconds, sleeping, scope, 1, false);
    }

    /**
     * @param quality   доля качества сна (1 — как обычно; {@code /rptime sleep} тому, кого усадили спать, — 0,8)
     * @param wakeAfter после прокрутки спящий просыпается (как утром); у ускоренной ночи RP Time — нет
     * @since 0.11.0
     */
    public RpTimeSkipEvent(Player player, long seconds, boolean sleeping, Scope scope, double quality, boolean wakeAfter) {
        super(player);
        this.seconds = seconds;
        this.sleeping = sleeping;
        this.scope = scope;
        this.quality = quality;
        this.wakeAfter = wakeAfter;
    }

    /** Доля качества сна поверх места (RP Needs). @since 0.11.0 */
    public double quality() {
        return quality;
    }

    /** Проснуться после прокрутки. @since 0.11.0 */
    public boolean wakeAfter() {
        return wakeAfter;
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
