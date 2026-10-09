package faygolover.rpcore.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rpcore.api.RpIds;
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
                                .then(Commands.argument("skill", ResourceLocationArgument.id()).suggests(SKILLS)
                                        .executes(c -> showSkill(c))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 10))
                                                .executes(c -> setSkill(c, IntegerArgumentType.getInteger(c, "level"))))
                                        .then(Commands.literal("reset").executes(c -> setSkill(c, -1)))))));
    }

    /** Подсказка по Tab: короткие имена навыков с русским названием рядом (если стоит RP Perks). */
    private static final SuggestionProvider<CommandSourceStack> SKILLS = (c, b) -> {
        String typed = b.getRemaining().toLowerCase(java.util.Locale.ROOT);
        for (ResourceLocation id : RpIds.SKILLS) {
            if (id.getPath().startsWith(typed) || id.toString().startsWith(typed))
                b.suggest(id.getPath(), Component.translatable("skill.rpperks." + id.getPath()));
        }
        return b.buildFuture();
    };

    /** «cooking» и «rpcore:cooking» — одно и то же. */
    private static ResourceLocation skillArg(CommandContext<CommandSourceStack> c) {
        ResourceLocation id = ResourceLocationArgument.getId(c, "skill");
        if (id.getNamespace().equals("minecraft")) {
            for (ResourceLocation s : RpIds.SKILLS) if (s.getPath().equals(id.getPath())) return s;
        }
        return id;
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
        ResourceLocation skill = skillArg(c);
        OptionalInt lvl = RpCoreAPI.skill(p, skill);
        c.getSource().sendSuccess(() -> lvl.isPresent()
                ? Component.translatable("rpcore.cmd.skill_level", p.getDisplayName(), skill.toString(), lvl.getAsInt())
                : Component.translatable("rpcore.cmd.skill_unmanaged", skill.toString()), false);
        return lvl.orElse(0);
    }

    private static int setSkill(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        ResourceLocation skill = skillArg(c);
        if (!RpCoreAPI.setSkill(p, skill, level)) {
            c.getSource().sendFailure(Component.translatable("rpcore.cmd.skill_unmanaged", skill.toString()));
            return 0;
        }
        return showSkill(c);
    }
}
