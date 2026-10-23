package faygolover.rpcore.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import faygolover.rpcore.gm.GmTeleport;
import faygolover.rpcore.gm.RpAnchors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * {@code /rpcore point} — именованные точки прибытия без блока: ведущий встаёт туда, куда переносить, смотрит, куда
 * должны смотреть прибывшие, и пишет {@code /rpcore point set <имя>}. Пластина «к точке по имени» ведёт сюда.
 */
public final class PointCommand {
    private PointCommand() {}

    private static final SuggestionProvider<CommandSourceStack> NAMES = (c, b) ->
            SharedSuggestionProvider.suggest(RpAnchors.get(c.getSource().getServer()).all().stream().map(RpAnchors.Anchor::name), b);

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("rpcore").then(Commands.literal("point").requires(s -> s.hasPermission(2))
                .then(Commands.literal("set").then(Commands.argument("name", StringArgumentType.word()).executes(PointCommand::set)))
                .then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.word()).suggests(NAMES).executes(PointCommand::remove)))
                .then(Commands.literal("tp").then(Commands.argument("name", StringArgumentType.word()).suggests(NAMES).executes(PointCommand::tp)))
                .then(Commands.literal("list").executes(PointCommand::list))));
    }

    private static int set(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(c, "name");
        RpAnchors.Anchor old = RpAnchors.get(c.getSource().getServer()).find(name);
        if (old != null && !old.owner().equals("command")) {
            c.getSource().sendFailure(Component.translatable("rpcore.point.taken", name, old.owner()));
            return 0;
        }
        RpAnchors.get(c.getSource().getServer()).put(new RpAnchors.Anchor(name, p.level().dimension(), p.position(),
                Mth.wrapDegrees(Math.round(p.getYRot())), Math.round(p.getXRot()), "command"));
        c.getSource().sendSuccess(() -> Component.translatable("rpcore.point.set", name), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "name");
        RpAnchors.get(c.getSource().getServer()).remove(name, "command");
        c.getSource().sendSuccess(() -> Component.translatable("rpcore.point.removed", name), true);
        return 1;
    }

    private static int tp(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        RpAnchors.Anchor a = RpAnchors.get(c.getSource().getServer()).find(StringArgumentType.getString(c, "name"));
        ServerLevel to = a == null ? null : RpAnchors.level(c.getSource().getServer(), a);
        if (to == null) {
            c.getSource().sendFailure(Component.translatable("rpcore.point.none"));
            return 0;
        }
        GmTeleport.teleport(p, to, a.pos(), a.yaw(), a.pitch(), false, 20);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        var all = RpAnchors.get(c.getSource().getServer()).all();
        if (all.isEmpty()) {
            c.getSource().sendSuccess(() -> Component.translatable("rpcore.point.empty"), false);
            return 0;
        }
        for (RpAnchors.Anchor a : all) {
            String where = String.format("%s %.1f %.1f %.1f · %s°", a.dim().location(), a.pos().x, a.pos().y, a.pos().z, Math.round(a.yaw()));
            c.getSource().sendSuccess(() -> Component.literal(" • " + a.name()).withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" — " + where + " (" + a.owner() + ")").withStyle(ChatFormatting.GRAY)), false);
        }
        return all.size();
    }
}
