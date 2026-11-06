package faygolover.rpcore.time;

import faygolover.rpcore.CoreConfig;
import net.minecraftforge.event.server.ServerStartingEvent;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/**
 * Время RP ({@code /rptime}, итоговый дизайн Medicine §6.2).
 * <p>Стоит RP Time — он источник: пауза ({@code /time pause} = {@code /rptime stop}) и длина суток берутся у него, а
 * своя механика Core ({@code [rptime] enabled}) не нужна. Без RP Time: механика выключена в конфиге — время идёт
 * всегда; включена — после каждого запуска сервера время стоит, пока ведущий не скажет {@code /rptime start}.
 */
public final class RpTime {
    private RpTime() {}

    private static volatile boolean started;
    private static volatile BooleanSupplier sourceRunning;
    private static volatile Consumer<Boolean> sourceSet;
    private static volatile DoubleSupplier sourceDay;
    private static volatile DoubleSupplier sourceSpeed;

    /** RP Time сообщает себя источником времени (сервер, при загрузке мода). */
    public static void setSource(BooleanSupplier running, Consumer<Boolean> setRunning, DoubleSupplier dayLengthSeconds) {
        sourceRunning = running;
        sourceSet = setRunning;
        sourceDay = dayLengthSeconds;
    }

    /** То же и скорость хода ({@code /time speed}): тело идёт как часы. */
    public static void setSource(BooleanSupplier running, Consumer<Boolean> setRunning, DoubleSupplier dayLengthSeconds,
                                 DoubleSupplier speed) {
        setSource(running, setRunning, dayLengthSeconds);
        sourceSpeed = speed;
    }

    public static boolean external() {
        return sourceRunning != null;
    }

    public static boolean enabled() {
        return external() || (CoreConfig.SPEC.isLoaded() && CoreConfig.RPTIME_ENABLED.get());
    }

    public static boolean running() {
        BooleanSupplier s = sourceRunning;
        if (s != null) return s.getAsBoolean();
        return !enabled() || started;
    }

    public static void setStarted(boolean value) {
        Consumer<Boolean> s = sourceSet;
        if (s != null) s.accept(value);
        else started = value;
    }

    /** Сутки RP в реальных секундах: у RP Time — его день + ночь, без него — из конфига Core. */
    public static double dayLengthSeconds() {
        DoubleSupplier s = sourceDay;
        double v = s != null ? s.getAsDouble() : 0;
        if (v > 0) return v;
        return (CoreConfig.SPEC.isLoaded() ? CoreConfig.DAY_LENGTH_MINUTES.get() : 180.0) * 60.0;
    }

    /** Множитель хода часов ({@code /time speed} RP Time), без паузы. Без RP Time или у старого — 1. */
    public static double speed() {
        DoubleSupplier s = sourceSpeed;
        double v = s != null ? s.getAsDouble() : 1;
        return v >= 0 && Double.isFinite(v) ? v : 1;
    }

    /** Сколько RP-секунд проходит за реальную секунду при идущем времени: сутки (86400) за длину суток, умножить на скорость. */
    public static double rpPerReal() {
        return 86400.0 / dayLengthSeconds() * speed();
    }

    /** То же, но 0 на паузе — множитель для долгих процессов тела. */
    public static double bodyScale() {
        return running() ? rpPerReal() : 0;
    }

    public static void onServerStarting(ServerStartingEvent e) {
        started = false;
    }
}
