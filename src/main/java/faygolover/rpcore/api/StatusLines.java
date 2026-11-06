package faygolover.rpcore.api;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Строки состояния от модов семейства для мода тела (RP Medicine): что чувствует сам игрок, что видно при осмотре
 * другим и что видит ГМ сканером и в панели. Мод-источник (RP Needs: сон) регистрирует себя, мод тела спрашивает
 * строки, когда строит осмотр или отчёт. Строка — ключ перевода и целые аргументы. Сервер.
 *
 * @since 0.11.0
 */
public final class StatusLines {
    private StatusLines() {}

    /** Кто смотрит. */
    public enum Viewer {
        /** Сам игрок — свои состояния, ощущениями, без цифр. */
        SELF,
        /** Другой игрок — осмотр, только то, что видно со стороны. */
        OTHER,
        /** ГМ — сканер и панель, с цифрами. */
        GM
    }

    /** Строка: ключ перевода (полный, например {@code rpneeds.status.drowsy}) и целые аргументы. */
    public record Line(String key, int... args) {}

    public interface Source {
        void lines(Player target, Viewer viewer, List<Line> out);

        /**
         * То же с уровнем медицины смотрящего (0–5; для SELF — уже урезанный, как свои строки мода тела): врач видит
         * больше признаков. По умолчанию — старый метод. @since 0.13.0
         */
        default void lines(Player target, Viewer viewer, int medicineLevel, List<Line> out) {
            lines(target, viewer, out);
        }
    }

    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();

    public static void register(Source source) {
        if (source != null && !SOURCES.contains(source)) SOURCES.add(source);
    }

    /** Все строки о {@code target} для {@code viewer}, уровень медицины смотрящего 0. */
    public static List<Line> collect(Player target, Viewer viewer) {
        return collect(target, viewer, 0);
    }

    /** Все строки о {@code target} для {@code viewer} с уровнем медицины смотрящего. @since 0.13.0 */
    public static List<Line> collect(Player target, Viewer viewer, int medicineLevel) {
        List<Line> out = new ArrayList<>();
        for (Source s : SOURCES) {
            try {
                s.lines(target, viewer, medicineLevel, out);
            } catch (RuntimeException e) {
                faygolover.rpcore.RpCore.LOGGER.warn("RP Core: строки состояния не собраны: {}", e.toString());
            }
        }
        return out;
    }
}
