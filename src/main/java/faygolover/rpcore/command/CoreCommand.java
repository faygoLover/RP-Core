package faygolover.rpcore.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import faygolover.rpcore.api.BodyProfile;
import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rpcore.api.RpIds;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.OptionalInt;

/**
 * {@code /rpcore} — что из семейства установлено и кто ведёт навыки; {@code /rpcore commands [мод]} — общий список
 * команд семейства; {@code /rpcore skill} — уровень и прогресс навыка, ГМ ставит уровень или прибавляет {@code add 1 | add 0.1}
 * поверх наработанного (если навык кто-то ведёт, например RP Perks);
 * {@code /rpcore profile} — анкета тела (пол, возраст, рост); {@code reset} — игрок сможет заполнить её заново.
 */
public final class CoreCommand {
    private CoreCommand() {}

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("rpcore")
                .executes(CoreCommand::info)
                .then(Commands.literal("compat").requires(s -> s.hasPermission(2))
                        .executes(CoreCommand::compat))
                .then(Commands.literal("commands")
                        .executes(c -> commands(c, null))
                        .then(Commands.argument("mod", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(RpCoreAPI.modules().keySet(), b))
                                .executes(c -> commands(c, StringArgumentType.getString(c, "mod")))))
                .then(Commands.literal("skill").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("skill", ResourceLocationArgument.id()).suggests(SKILLS)
                                        .executes(c -> showSkill(c))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 10))
                                                .executes(c -> setSkill(c, IntegerArgumentType.getInteger(c, "level"))))
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("levels", DoubleArgumentType.doubleArg(-10, 10))
                                                        .executes(c -> addSkill(c, DoubleArgumentType.getDouble(c, "levels"))))))))
                .then(Commands.literal("profile").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(CoreCommand::showProfile)
                                .then(Commands.literal("sex")
                                        .then(Commands.literal("male").executes(c -> editProfile(c, p -> p.withSex(BodyProfile.Sex.MALE))))
                                        .then(Commands.literal("female").executes(c -> editProfile(c, p -> p.withSex(BodyProfile.Sex.FEMALE))))
                                        .then(Commands.literal("none").executes(c -> editProfile(c, p -> p.withSex(BodyProfile.Sex.NONE)))))
                                .then(Commands.literal("age")
                                        .then(Commands.argument("age", IntegerArgumentType.integer(BodyProfile.MIN_AGE, BodyProfile.MAX_AGE))
                                                .executes(c -> editProfile(c, p -> p.withAge(IntegerArgumentType.getInteger(c, "age"))))))
                                .then(Commands.literal("height")
                                        .then(Commands.argument("cm", IntegerArgumentType.integer((int) BodyProfile.MIN_HEIGHT, (int) BodyProfile.MAX_HEIGHT))
                                                .executes(c -> editProfile(c, p -> p.withHeight(IntegerArgumentType.getInteger(c, "cm"))))))
                                .then(Commands.literal("reset").executes(c -> editProfile(c, p -> p.withFilled(false))))))
                .then(Commands.literal("wipe").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", net.minecraft.commands.arguments.GameProfileArgument.gameProfile())
                                .executes(c -> wipe(c, false))
                                .then(Commands.literal("confirm").executes(c -> wipe(c, true)))))
                .then(Commands.literal("taste").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(CoreCommand::showTaste)
                                .then(Commands.literal("reset").executes(c -> setTaste(c, null, null)))
                                .then(Commands.argument("favourite", ResourceLocationArgument.id()).suggests(TASTES)
                                        .then(Commands.argument("disliked", ResourceLocationArgument.id()).suggests(TASTES)
                                                .executes(c -> setTaste(c, ResourceLocationArgument.getId(c, "favourite").toString(),
                                                        ResourceLocationArgument.getId(c, "disliked").toString())))))));
    }

    /** Варианты вкусов: kind:… и method:…. */
    private static final SuggestionProvider<CommandSourceStack> TASTES =
            (c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(faygolover.rpcore.api.FoodTaste.options(), b);

    /** Вайп персонажа: без {@code confirm} только предупреждает. Игрока нет в сети — сработает при входе. */
    private static int wipe(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> c, boolean confirmed)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var src = c.getSource();
        var profiles = net.minecraft.commands.arguments.GameProfileArgument.getGameProfiles(c, "player");
        for (var p : profiles) {
            if (!confirmed) {
                src.sendSuccess(() -> Component.translatable("rpcore.cmd.wipe_confirm", p.getName()), false);
                continue;
            }
            boolean now = faygolover.rpcore.api.RpCoreAPI.wipeCharacter(src.getServer(), p.getId(), p.getName(), src.getTextName());
            src.sendSuccess(() -> Component.translatable(now ? "rpcore.cmd.wiped" : "rpcore.cmd.wipe_pending", p.getName()), true);
        }
        return profiles.size();
    }

    private static int showTaste(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var p = EntityArgument.getPlayer(c, "player");
        var t = faygolover.rpcore.api.RpCoreAPI.taste(p);
        c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("rpcore.taste.show", p.getDisplayName(),
                net.minecraft.network.chat.Component.translatable(faygolover.rpcore.api.FoodTaste.langKey(t.favourite())),
                net.minecraft.network.chat.Component.translatable(faygolover.rpcore.api.FoodTaste.langKey(t.disliked()))), false);
        return 1;
    }

    /** ГМ: вкусы игрока; null — сбросить (игрок выберет сам заново). */
    private static int setTaste(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, String favourite, String disliked)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var p = EntityArgument.getPlayer(c, "player");
        if (favourite == null) {
            faygolover.rpcore.api.RpCoreAPI.setTaste(p, faygolover.rpcore.api.FoodTaste.NONE);
        } else {
            if (!faygolover.rpcore.api.FoodTaste.validPair(favourite, disliked)) {
                c.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("rpcore.taste.invalid"));
                return 0;
            }
            faygolover.rpcore.api.RpCoreAPI.setTaste(p, new faygolover.rpcore.api.FoodTaste(favourite, disliked, true));
        }
        return showTaste(c);
    }

    /** Подсказка по Tab: короткие имена навыков с названием рядом (переводы Core). */
    private static final SuggestionProvider<CommandSourceStack> SKILLS = (c, b) -> {
        String typed = b.getRemaining().toLowerCase(java.util.Locale.ROOT);
        for (ResourceLocation id : RpIds.SKILLS) {
            if (id.getPath().startsWith(typed) || id.toString().startsWith(typed))
                b.suggest(id.getPath(), Component.translatable("skill.rpcore." + id.getPath()));
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
        src.sendSuccess(() -> Component.translatable("rpcore.cmd.commands_hint").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    /** Общий список команд семейства: что можно этому игроку; отладочные — только если включены в конфиге. */
    private static int commands(CommandContext<CommandSourceStack> c, String mod) {
        var src = c.getSource();
        boolean debug = RpCoreAPI.debugCommands();
        String current = null;
        int n = 0;
        for (RpCoreAPI.CommandInfo info : RpCoreAPI.commands().stream()
                .sorted(java.util.Comparator.comparing(RpCoreAPI.CommandInfo::modId)).toList()) {
            if (mod != null && !info.modId().equals(mod)) continue;
            if (!src.hasPermission(info.permission()) || info.debug() && !debug) continue;
            if (!info.modId().equals(current)) {
                current = info.modId();
                String title = current;
                src.sendSuccess(() -> Component.literal(title + ":").withStyle(ChatFormatting.GOLD), false);
            }
            src.sendSuccess(() -> Component.literal(" " + info.syntax()).withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" — ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.translatable(info.descriptionKey()).withStyle(ChatFormatting.GRAY)), false);
            n++;
        }
        if (n == 0) src.sendFailure(Component.translatable("rpcore.cmd.commands_none"));
        return n;
    }

    /** Уровень и точный прогресс до следующего уровня (до сотых процента). */
    private static int showSkill(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        ResourceLocation skill = skillArg(c);
        OptionalInt lvl = RpCoreAPI.skill(p, skill);
        if (lvl.isEmpty()) {
            c.getSource().sendFailure(Component.translatable("rpcore.cmd.skill_unmanaged", skill.toString()));
            return 0;
        }
        double progress = RpCoreAPI.skillProgress(p, skill);
        Component line;
        if (!Double.isFinite(progress)) {
            line = Component.translatable("rpcore.cmd.skill_level", p.getDisplayName(), skill.toString(), lvl.getAsInt());
        } else if (progress >= 10) {
            line = Component.translatable("rpcore.cmd.skill_max", p.getDisplayName(), skill.toString(), lvl.getAsInt());
        } else {
            int base = (int) Math.floor(progress);
            String pct = String.format(java.util.Locale.ROOT, "%.2f", (progress - base) * 100);
            line = Component.translatable("rpcore.cmd.skill_progress", p.getDisplayName(), skill.toString(), lvl.getAsInt(), base, pct);
        }
        c.getSource().sendSuccess(() -> line, false);
        return lvl.getAsInt();
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

    /** Прибавить или отнять уровни: 1 — уровень, 0.1 — 10 % уровня. */
    private static int addSkill(CommandContext<CommandSourceStack> c, double levels) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        ResourceLocation skill = skillArg(c);
        if (!RpCoreAPI.addSkill(p, skill, levels)) {
            c.getSource().sendFailure(Component.translatable("rpcore.cmd.skill_unmanaged", skill.toString()));
            return 0;
        }
        return showSkill(c);
    }

    private static int showProfile(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        BodyProfile prof = RpCoreAPI.profile(p);
        double w = RpCoreAPI.weightKg(p), fat = RpCoreAPI.bodyFatPercent(p);
        c.getSource().sendSuccess(() -> Component.translatable("rpcore.cmd.profile", p.getDisplayName(),
                Component.translatable("rpcore.profile.sex." + prof.sex().id()), prof.age(), Math.round(prof.heightCm()),
                Component.translatable(prof.filled() ? "rpcore.cmd.profile_filled" : "rpcore.cmd.profile_empty")), false);
        if (Double.isFinite(w) && Double.isFinite(fat)) {
            c.getSource().sendSuccess(() -> Component.translatable("rpcore.cmd.profile_body", String.format(java.util.Locale.ROOT, "%.1f", w),
                    String.format(java.util.Locale.ROOT, "%.1f", fat),
                    Component.translatable("rpcore.body_state." + RpCoreAPI.bodyState(p).name().toLowerCase(java.util.Locale.ROOT)),
                    Math.round(faygolover.rpcore.api.BodyNorms.bloodVolumeMl(prof.sex(), prof.heightCm(), w))).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int editProfile(CommandContext<CommandSourceStack> c, java.util.function.UnaryOperator<BodyProfile> edit)
            throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        RpCoreAPI.setProfile(p, edit.apply(RpCoreAPI.profile(p)));
        return showProfile(c);
    }

    /** /rpcore compat — что стало с необязательными интеграциями модов семейства: сначала сломанные. */
    private static int compat(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> c) {
        java.util.List<RpCoreAPI.CompatInfo> all = new java.util.ArrayList<>(RpCoreAPI.compat());
        all.sort(java.util.Comparator.comparing((RpCoreAPI.CompatInfo i) -> i.state() == RpCoreAPI.CompatState.BROKEN ? 0
                : i.state() == RpCoreAPI.CompatState.ON ? 1 : 2).thenComparing(RpCoreAPI.CompatInfo::modId));
        if (all.isEmpty()) {
            c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("rpcore.compat.empty"), false);
            return 0;
        }
        for (RpCoreAPI.CompatInfo i : all) {
            net.minecraft.ChatFormatting color = switch (i.state()) {
                case ON -> net.minecraft.ChatFormatting.GREEN;
                case ABSENT -> net.minecraft.ChatFormatting.GRAY;
                case BROKEN -> net.minecraft.ChatFormatting.RED;
            };
            String line = i.modId() + " → " + i.integration() + (i.reason().isEmpty() ? "" : " — " + i.reason());
            c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("rpcore.compat." + i.state().name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(color).append(net.minecraft.network.chat.Component.literal(" " + line).withStyle(net.minecraft.ChatFormatting.WHITE)), false);
        }
        return all.size();
    }

    /** Старт сервера: одна сводная строка в лог — сколько интеграций включено, скольких модов нет, что сломано. */
    public static void logCompat(net.minecraftforge.event.server.ServerStartedEvent e) {
        int on = 0, absent = 0;
        StringBuilder broken = new StringBuilder();
        for (RpCoreAPI.CompatInfo i : RpCoreAPI.compat()) {
            switch (i.state()) {
                case ON -> on++;
                case ABSENT -> absent++;
                case BROKEN -> broken.append(broken.length() == 0 ? "" : "; ").append(i.modId()).append(" → ").append(i.integration())
                        .append(i.reason().isEmpty() ? "" : " (" + i.reason() + ")");
            }
        }
        if (on + absent == 0 && broken.length() == 0) return;
        if (broken.length() == 0) faygolover.rpcore.RpCore.LOGGER.info("RP Core: интеграции семейства — включено {}, модов нет {}, сломанных нет", on, absent);
        else faygolover.rpcore.RpCore.LOGGER.warn("RP Core: интеграции семейства — включено {}, модов нет {}, СЛОМАНО: {}", on, absent, broken);
    }
}
