package faygolover.rpcore.api.event;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Прокрутка времени RP для тех, кого нет в сети ({@code /rptime sleep|skip … all|offline}, сервер). Игрока-сущности у
 * них нет, поэтому событие одно на всех: каждый мод прокручивает то, что хранит о вышедших (RP Medicine — тела
 * вышедших игроков). Сколько прокрутил — {@link #count}. Слушать на {@code MinecraftForge.EVENT_BUS}.
 *
 * @since 0.4.2
 */
public class RpTimeSkipOfflineEvent extends Event {
    private final MinecraftServer server;
    private final long seconds;
    private final boolean sleeping;
    private final RpTimeSkipEvent.Scope scope;
    private int players;

    public RpTimeSkipOfflineEvent(MinecraftServer server, long seconds, boolean sleeping, RpTimeSkipEvent.Scope scope) {
        this.server = server;
        this.seconds = seconds;
        this.sleeping = sleeping;
        this.scope = scope;
    }

    public MinecraftServer server() {
        return server;
    }

    /** Сколько секунд реального времени прошло. */
    public long seconds() {
        return seconds;
    }

    public double hours() {
        return seconds / 3600.0;
    }

    /** Во сне ({@code sleep}) или бодрствуя ({@code skip}). */
    public boolean sleeping() {
        return sleeping;
    }

    public RpTimeSkipEvent.Scope scope() {
        return scope;
    }

    /** Прокручивать ли голод, воду, вес. */
    public boolean food() {
        return scope != RpTimeSkipEvent.Scope.WOUNDS;
    }

    /** Прокручивать ли раны и таймеры тела. */
    public boolean wounds() {
        return scope != RpTimeSkipEvent.Scope.FOOD;
    }

    /** Мод прокрутил столько вышедших игроков (для ответа команды; от нескольких модов берётся наибольшее). */
    public void count(int players) {
        this.players = Math.max(this.players, players);
    }

    public int players() {
        return players;
    }
}
