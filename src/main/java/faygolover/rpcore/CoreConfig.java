package faygolover.rpcore;

import net.minecraftforge.common.ForgeConfigSpec;

/** Общий конфиг RP Core: {@code config/rpcore-common.toml}. */
public final class CoreConfig {
    private CoreConfig() {}

    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue DEBUG_COMMANDS;
    public static final ForgeConfigSpec.IntValue PHOTO_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue RPTIME_ENABLED;
    public static final ForgeConfigSpec.IntValue ACCESS_PERMISSION_LEVEL;

    static {
        B.push("commands");
        DEBUG_COMMANDS = B.comment("Отладочные команды модов семейства (/rpstamina debug, /rpstamina export_defaults,",
                        "/rpculinary audit и т. п.). Выключены — их нет в подсказках и они не работают.")
                .define("debug_commands", false);
        B.pop();

        B.push("photo");
        PHOTO_INTERVAL = B.comment("Фото игрока (портрет по скину для медкарты и КПК): не чаще одного снимка за столько секунд.")
                .defineInRange("min_interval_seconds", 60, 5, 86400);
        B.pop();

        B.push("rptime");
        RPTIME_ENABLED = B.comment("Время RP (/rptime start|stop). Включено — после запуска сервера время стоит, пока ведущий",
                        "не скажет /rptime start: голод не тратится, раны не заживают, таймеры тела стоят.",
                        "Выключено — время идёт всегда, как без этой механики. /rptime sleep и skip работают в обоих случаях.")
                .define("enabled", false);
        B.pop();

        B.push("access");
        ACCESS_PERMISSION_LEVEL = B.comment("Ведущие: с таким уровнем прав на выделенном сервере игроку открыты все предметы RP-модов",
                        "(в сборках с замком). В одиночном мире и LAN не действует.")
                .defineInRange("gm_permission_level", 2, 1, 4);
        B.pop();
        SPEC = B.build();
    }
}
