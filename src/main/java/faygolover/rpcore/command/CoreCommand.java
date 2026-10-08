package faygolover.rpcore.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import faygolover.rpcore.api.RpCoreAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.OptionalInt;

/**
 * {@code /rpcore} — что из семейства установлено и кто ведёт навыки; {@code /rpcore skill} — уровень навыка
 * (если навык кто-то ведёт, например RP Perks).
 */
public final class CoreCommand {
    private CoreCommand() {}

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("rpcore")
                .executes(CoreCommand::info)
                .then(Commands.literal("skill").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("skill", ResourceLocationArgument.id())
                                        .executes(c -> showSkill(c))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 10))
                                                .executes(c -> setSkill(c, IntegerArgumentType.getInteger(c, "level"))))
                                        .then(Commands.literal("reset").executes(c -> setSkill(c, -1)))))));
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        var src = c.getSource();
        src.sendSuccess(() -> Component.translatable("rpcore.cmd.modules").withStyle(ChatFormatting.GOLD), false);
        RpCoreAPI.modules().forEach((id, what) ->
                src.sendSuccess(() -> Component.literal(" • " + id + " — " + what), false));
        boolean medicine = RpCoreAPI.isSkillManaged(faygolover.rpcore.api.RpIds.MEDICINE);
        src.sendSuccess(() -> Component.translatable(medicine ? "rpcore.cmd.skills_managed" : "rpcore.cmd.skills_unmanaged")
                .withStyle(ChatFormatting.GRAY), false);
        boolean food = RpCoreAPI.nutrition() != null;
        src.sendSuccess(() -> Component.translatable(food ? "rpcore.cmd.food_managed" : "rpcore.cmd.food_unmanaged")
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int showSkill(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        ResourceLocation skill = ResourceLocationArgument.getId(c, "skill");
        OptionalInt lvl = RpCoreAPI.skill(p, skill);
        c.getSource().sendSuccess(() -> lvl.isPresent()
                ? Component.translatable("rpcore.cmd.skill_level", p.getDisplayName(), skill.toString(), lvl.getAsInt())
                : Component.translatable("rpcore.cmd.skill_unmanaged", skill.toString()), false);
        return lvl.orElse(0);
    }

    private static int setSkill(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        ResourceLocation skill = ResourceLocationArgument.getId(c, "skill");
        if (!RpCoreAPI.setSkill(p, skill, level)) {
            c.getSource().sendFailure(Component.translatable("rpcore.cmd.skill_unmanaged", skill.toString()));
            return 0;
        }
        return showSkill(c);
    }
}
