package faygolover.rpcore.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import faygolover.rpcore.api.event.RpTimeSkipEvent;
import faygolover.rpcore.api.event.RpTimeSkipOfflineEvent;
import faygolover.rpcore.time.RpTime;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code /rptime} — время RP (итоговый дизайн Medicine §6.2):
 * <ul>
 *   <li>{@code /rptime} — идёт ли время;</li>
 *   <li>{@code /rptime start|stop} — пустить или остановить (если механика включена в конфиге Core);</li>
 *   <li>{@code /rptime sleep <время> <игроки> [all|wounds|food] [online|all|offline]} — прокрутка во сне, мир не меняется;</li>
 *   <li>{@code /rptime skip <время> <игроки> [all|wounds|food] [online|all|offline]} — то же бодрствуя.</li>
 * </ul>
 * Время — {@code 8h}, {@code 30m}, {@code 1h30m}, {@code 45s} или просто число часов ({@code 2.5}).
 * Последнее слово — кого: {@code online} (или ничего) — выбранных игроков в сети, {@code all} — их и всех, кого нет в
 * сети, {@code offline} — только всех, кого нет в сети (выбранные в сети тогда не прокручиваются). Вышедшего игрока
 * в селекторе не выбрать — их прокручивают только всех вместе.
 */
public final class TimeCommand {
    private TimeCommand() {}

    /** Больше недели за раз не прокручиваем. */
    private static final long MAX_SECONDS = 7L * 24 * 3600;
    private static final Pattern PART = Pattern.compile("(\\d+(?:\\.\\d+)?)([hms]?)");
    /** Кого прокручивать: выбранных в сети, их и вышедших, только вышедших. @since 0.4.2 */
    private enum Who { ONLINE, ALL, OFFLINE }

    private static final SimpleCommandExceptionType BAD_TIME =
            new SimpleCommandExceptionType(Component.translatable("rpcore.time.bad_time"));

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("rptime")
                .executes(TimeCommand::status)
                .then(Commands.literal("start").requires(s -> s.hasPermission(2)).executes(c -> setStarted(c, true)))
                .then(Commands.literal("stop").requires(s -> s.hasPermission(2)).executes(c -> setStarted(c, false)))
                .then(skipBranch("sleep", true))
                .then(skipBranch("skip", false)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> skipBranch(String name, boolean sleeping) {
        return Commands.literal(name).requires(s -> s.hasPermission(2))
                .then(Commands.argument("time", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"30m", "1h", "2h", "8h"}, b))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> skip(c, sleeping, RpTimeSkipEvent.Scope.ALL, Who.ONLINE))
                                .then(scopeBranch("all", sleeping, RpTimeSkipEvent.Scope.ALL))
                                .then(scopeBranch("wounds", sleeping, RpTimeSkipEvent.Scope.WOUNDS))
                                .then(scopeBranch("food", sleeping, RpTimeSkipEvent.Scope.FOOD))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> scopeBranch(String name, boolean sleeping, RpTimeSkipEvent.Scope scope) {
        return Commands.literal(name)
                .executes(c -> skip(c, sleeping, scope, Who.ONLINE))
                .then(Commands.literal("online").executes(c -> skip(c, sleeping, scope, Who.ONLINE)))
                .then(Commands.literal("all").executes(c -> skip(c, sleeping, scope, Who.ALL)))
                .then(Commands.literal("offline").executes(c -> skip(c, sleeping, scope, Who.OFFLINE)));
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        String key = !RpTime.enabled() ? "rpcore.time.disabled" : RpTime.running() ? "rpcore.time.running" : "rpcore.time.stopped";
        c.getSource().sendSuccess(() -> Component.translatable(key), false);
        return RpTime.running() ? 1 : 0;
    }

    private static int setStarted(CommandContext<CommandSourceStack> c, boolean value) {
        if (!RpTime.enabled()) {
            c.getSource().sendFailure(Component.translatable("rpcore.time.disabled"));
            return 0;
        }
        RpTime.setStarted(value);
        c.getSource().sendSuccess(() -> Component.translatable(value ? "rpcore.time.started" : "rpcore.time.stopped")
                .withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int skip(CommandContext<CommandSourceStack> c, boolean sleeping, RpTimeSkipEvent.Scope scope, Who who)
            throws CommandSyntaxException {
        long seconds = parse(StringArgumentType.getString(c, "time"));
        Collection<ServerPlayer> targets = who == Who.OFFLINE ? java.util.List.of() : EntityArgument.getPlayers(c, "targets");
        for (ServerPlayer p : targets) MinecraftForge.EVENT_BUS.post(new RpTimeSkipEvent(p, seconds, sleeping, scope));
        String what = scope.name().toLowerCase(Locale.ROOT);
        Component scopeName = Component.translatable("rpcore.time.scope." + what);
        if (who == Who.ONLINE) {
            c.getSource().sendSuccess(() -> Component.translatable(sleeping ? "rpcore.time.slept" : "rpcore.time.skipped",
                    targets.size(), format(seconds), scopeName), true);
            return targets.size();
        }
        RpTimeSkipOfflineEvent offline = new RpTimeSkipOfflineEvent(c.getSource().getServer(), seconds, sleeping, scope);
        MinecraftForge.EVENT_BUS.post(offline);
        c.getSource().sendSuccess(() -> Component.translatable(sleeping ? "rpcore.time.slept_offline" : "rpcore.time.skipped_offline",
                targets.size(), offline.players(), format(seconds), scopeName), true);
        return targets.size() + offline.players();
    }

    /** «1h30m», «45m», «2.5» (часы) → секунды. */
    static long parse(String s) throws CommandSyntaxException {
        String text = s.toLowerCase(Locale.ROOT);
        Matcher m = PART.matcher(text);
        double seconds = 0;
        int pos = 0;
        while (pos < text.length() && m.find(pos) && m.start() == pos) {
            double v = Double.parseDouble(m.group(1));
            seconds += switch (m.group(2)) {
                case "m" -> v * 60;
                case "s" -> v;
                default -> v * 3600;
            };
            pos = m.end();
            if (m.group(2).isEmpty()) break;
        }
        if (pos != text.length() || seconds < 1 || seconds > MAX_SECONDS) throw BAD_TIME.create();
        return Math.round(seconds);
    }

    static String format(long seconds) {
        long h = seconds / 3600, m = seconds % 3600 / 60, s = seconds % 60;
        StringBuilder b = new StringBuilder();
        if (h > 0) b.append(h).append("h");
        if (m > 0) b.append(m).append("m");
        if (s > 0 || b.isEmpty()) b.append(s).append("s");
        return b.toString();
    }
}
