package faygolover.rpcore.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rpcore.api.RpCoreAPI.Posture;
import faygolover.rpcore.api.event.RpTimeSkipEvent;
import faygolover.rpcore.api.event.RpTimeSkipOfflineEvent;
import faygolover.rpcore.pose.Postures;
import faygolover.rpcore.time.RpTime;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code /rptime} — время RP (решения 30.10). Длительности — в RP-единицах: {@code 30m}, {@code 2h}, {@code 1h30m},
 * {@code 1d} (RP-сутки = день + ночь RP Time) или число часов. Мягко: еда, вода и бодрость не ниже порога Core.
 * <ul>
 *   <li>{@code /rptime} — сколько времени и идёт ли оно; {@code pause|resume} — одна пауза на всё;</li>
 *   <li>{@code add <длительность>}, {@code set <ЧЧ:ММ>} — вперёд для всех: солнце и тела (и вышедших);</li>
 *   <li>{@code sleep add|set …} — то же во сне: спящие и лежащие спят как есть, стоящие и сидящие садятся и спят
 *       хуже (×0,8); после — все просыпаются;</li>
 *   <li>{@code player add|sleep add <игроки> <длительность>} — только тела выбранных, солнце стоит.</li>
 * </ul>
 * Длина суток, скорость, ночь во сне и рост растений — {@code /rptime info|length|speed|night|growth} (RP Time).
 */
public final class TimeCommand {
    private TimeCommand() {}

    /** Больше 30 RP-суток за раз не прокручиваем. */
    private static final double MAX_RP_SECONDS = 30 * 86400.0;
    private static final Pattern PART = Pattern.compile("(\\d+(?:\\.\\d+)?)([dhm]?)");
    private static final Pattern CLOCK = Pattern.compile("(\\d{1,2})[:.](\\d{2})");

    private static final SimpleCommandExceptionType BAD_TIME =
            new SimpleCommandExceptionType(Component.translatable("rpcore.time.bad_time"));
    private static final SimpleCommandExceptionType BAD_CLOCK =
            new SimpleCommandExceptionType(Component.translatable("rpcore.time.bad_clock"));

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("rptime")
                .executes(TimeCommand::status)
                .then(Commands.literal("pause").requires(s -> s.hasPermission(2)).executes(c -> setRunning(c, false)))
                .then(Commands.literal("resume").requires(s -> s.hasPermission(2)).executes(c -> setRunning(c, true)))
                .then(Commands.literal("add").requires(s -> s.hasPermission(2)).then(duration(c -> all(c, rp(c), false))))
                .then(Commands.literal("set").requires(s -> s.hasPermission(2)).then(clock(c -> all(c, untilClock(c), false))))
                .then(Commands.literal("sleep").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("add").then(duration(c -> all(c, rp(c), true))))
                        .then(Commands.literal("set").then(clock(c -> all(c, untilClock(c), true)))))
                .then(Commands.literal("player").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("add").then(Commands.argument("targets", EntityArgument.players())
                                .then(duration(c -> players(c, false)))))
                        .then(Commands.literal("sleep").then(Commands.literal("add").then(Commands.argument("targets", EntityArgument.players())
                                .then(duration(c -> players(c, true))))))));
    }

    private interface Run {
        int run(CommandContext<CommandSourceStack> c) throws CommandSyntaxException;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> duration(Run run) {
        return Commands.argument("time", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"30m", "1h", "2h", "8h", "1d"}, b))
                .executes(run::run);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> clock(Run run) {
        return Commands.argument("clock", StringArgumentType.greedyString())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"6:00", "8:00", "12:00", "18:00", "22:00"}, b))
                .executes(run::run);
    }

    // ------------------------------------------------------------------ состояние и пауза

    private static int status(CommandContext<CommandSourceStack> c) {
        MinecraftServer server = c.getSource().getServer();
        String key = !RpTime.enabled() ? "rpcore.time.disabled" : RpTime.running() ? "rpcore.time.running" : "rpcore.time.stopped";
        c.getSource().sendSuccess(() -> Component.translatable(key).append(" ")
                .append(Component.translatable("rpcore.time.now", clockText(server), day(server))), false);
        return RpTime.running() ? 1 : 0;
    }

    private static int setRunning(CommandContext<CommandSourceStack> c, boolean value) {
        if (!RpTime.enabled()) {
            c.getSource().sendFailure(Component.translatable("rpcore.time.disabled"));
            return 0;
        }
        RpTime.setStarted(value);
        c.getSource().sendSuccess(() -> Component.translatable(value ? "rpcore.time.started" : "rpcore.time.stopped")
                .withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    // ------------------------------------------------------------------ прокрутка

    /** Для всех: солнце, тела в сети и вышедших. */
    private static int all(CommandContext<CommandSourceStack> c, double rpSeconds, boolean sleep) {
        MinecraftServer server = c.getSource().getServer();
        if (rpSeconds <= 0) {
            c.getSource().sendFailure(Component.translatable("rpcore.time.already"));
            return 0;
        }
        long real = realSeconds(rpSeconds);
        long ticks = Math.round(rpSeconds * 24000.0 / 86400.0);
        for (ServerLevel level : server.getAllLevels()) level.setDayTime(level.getDayTime() + ticks);
        List<ServerPlayer> online = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) if (!p.isSpectator()) online.add(p);
        skipPlayers(online, real, sleep);
        RpTimeSkipOfflineEvent offline = new RpTimeSkipOfflineEvent(server, real, sleep, RpTimeSkipEvent.Scope.ALL);
        MinecraftForge.EVENT_BUS.post(offline);
        c.getSource().sendSuccess(() -> Component.translatable(sleep ? "rpcore.time.slept" : "rpcore.time.passed",
                formatRp(rpSeconds), formatReal(real), clockText(server), day(server), online.size(), offline.players())
                .withStyle(ChatFormatting.GOLD), true);
        return online.size() + offline.players();
    }

    /** Только тела выбранных игроков; солнце стоит. */
    private static int players(CommandContext<CommandSourceStack> c, boolean sleep) throws CommandSyntaxException {
        double rpSeconds = rp(c);
        long real = realSeconds(rpSeconds);
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
        skipPlayers(targets, real, sleep);
        c.getSource().sendSuccess(() -> Component.translatable(sleep ? "rpcore.time.player_slept" : "rpcore.time.player_passed",
                targets.size(), formatRp(rpSeconds), formatReal(real)).withStyle(ChatFormatting.GOLD), true);
        return targets.size();
    }

    /**
     * Прокрутить тела. Во сне: кто спал или лежал — спит как есть; кто стоял или сидел — садится и засыпает хуже
     * (качество ×0,8); после — все просыпаются (позу оставляем).
     */
    private static void skipPlayers(Collection<ServerPlayer> players, long real, boolean sleep) {
        for (ServerPlayer p : players) {
            double quality = 1;
            if (sleep) {
                Posture posture = Postures.posture(p);
                boolean resting = p.isSleeping() || Postures.sleeping(p) || posture == Posture.LAY || posture == Posture.HOSPITAL_BED;
                if (!resting) {
                    quality = 0.8;
                    if (posture == Posture.NONE) Postures.sitDown(p);
                }
            }
            MinecraftForge.EVENT_BUS.post(new RpTimeSkipEvent(p, real, sleep, RpTimeSkipEvent.Scope.ALL, quality, sleep));
            if (sleep) Postures.wakeUp(p);
        }
    }

    // ------------------------------------------------------------------ разбор и вывод

    /** RP-секунды из «1h30m», «45m», «1d», «2.5» (часы). */
    private static double rp(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String text = StringArgumentType.getString(c, "time").toLowerCase(Locale.ROOT);
        Matcher m = PART.matcher(text);
        double seconds = 0;
        int pos = 0;
        while (pos < text.length() && m.find(pos) && m.start() == pos) {
            double v = Double.parseDouble(m.group(1));
            seconds += switch (m.group(2)) {
                case "d" -> v * 86400;
                case "m" -> v * 60;
                default -> v * 3600;
            };
            pos = m.end();
            if (m.group(2).isEmpty()) break;
        }
        if (pos != text.length() || seconds < 60 || seconds > MAX_RP_SECONDS) throw BAD_TIME.create();
        return seconds;
    }

    /** Сколько RP-секунд до ближайшего ЧЧ:ММ (вперёд; то же время — 0). */
    private static double untilClock(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Matcher m = CLOCK.matcher(StringArgumentType.getString(c, "clock").trim());
        if (!m.matches()) throw BAD_CLOCK.create();
        int h = Integer.parseInt(m.group(1)), min = Integer.parseInt(m.group(2));
        if (h > 23 || min > 59) throw BAD_CLOCK.create();
        long target = Math.floorMod(Math.round((h - 6) * 1000 + min * 1000.0 / 60.0), 24000L);
        long now = c.getSource().getServer().overworld().getDayTime() % 24000L;
        long delta = Math.floorMod(target - now, 24000L);
        return delta * 86400.0 / 24000.0;
    }

    /** RP-секунды → реальные: доля RP-суток × длина суток. */
    private static long realSeconds(double rpSeconds) {
        return Math.max(1, Math.round(rpSeconds / 86400.0 * RpCoreAPI.dayLengthSeconds()));
    }

    private static String clockText(MinecraftServer server) {
        long t = server.overworld().getDayTime() % 24000L;
        int h = (int) ((t / 1000 + 6) % 24);
        int m = (int) (t % 1000 * 60 / 1000);
        return String.format(Locale.ROOT, "%d:%02d", h, m);
    }

    private static long day(MinecraftServer server) {
        return server.overworld().getDayTime() / 24000L + 1;
    }

    private static String formatRp(double rpSeconds) {
        long total = Math.round(rpSeconds / 60);
        long d = total / 1440, h = total % 1440 / 60, m = total % 60;
        StringBuilder b = new StringBuilder();
        if (d > 0) b.append(d).append("d");
        if (h > 0) b.append(h).append("h");
        if (m > 0 || b.isEmpty()) b.append(m).append("m");
        return b.toString();
    }

    private static String formatReal(long seconds) {
        long h = seconds / 3600, m = seconds % 3600 / 60;
        return h > 0 ? h + "h" + (m > 0 ? m + "m" : "") : Math.max(1, m) + "m";
    }
}
