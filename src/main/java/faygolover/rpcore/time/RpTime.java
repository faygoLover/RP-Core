package faygolover.rpcore.time;

import faygolover.rpcore.CoreConfig;
import net.minecraftforge.event.server.ServerStartingEvent;

/**
 * Время RP ({@code /rptime}, итоговый дизайн Medicine §6.2). Механика выключена в конфиге — время идёт всегда.
 * Включена — после каждого запуска сервера время стоит, пока ведущий не скажет {@code /rptime start}.
 * Само состояние не сохраняется: «стоп» после перезапуска — так задумано.
 */
public final class RpTime {
    private RpTime() {}

    private static volatile boolean started;

    public static boolean enabled() {
        return CoreConfig.SPEC.isLoaded() && CoreConfig.RPTIME_ENABLED.get();
    }

    public static boolean running() {
        return !enabled() || started;
    }

    public static void setStarted(boolean value) {
        started = value;
    }

    public static void onServerStarting(ServerStartingEvent e) {
        started = false;
    }
}
