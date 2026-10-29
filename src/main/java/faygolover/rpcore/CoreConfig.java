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
    public static final ForgeConfigSpec.DoubleValue DAY_LENGTH_MINUTES;
    public static final ForgeConfigSpec.DoubleValue SKIP_FLOOR;
    public static final ForgeConfigSpec.BooleanValue POSTURES_ENABLED;
    public static final ForgeConfigSpec.DoubleValue POSTURE_STAND_DISTANCE;

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

        B.push("time");
        DAY_LENGTH_MINUTES = B.comment("Длина RP-суток в реальных минутах, если RP Time нет. С ним — его день + ночь.",
                        "От суток считаются голод, жажда, бодрость и другие медленные механики семейства.")
                .defineInRange("day_length_minutes", 180.0, 1.0, 100000.0);
        SKIP_FLOOR = B.comment("Прокрутка времени (/rptime skip|sleep, ускоренная ночь) и время вне сети тратят еду и воду",
                        "не ниже этой доли шкалы. 0.2 — до 1/5. В живом времени шкалы падают как обычно.")
                .defineInRange("skip_floor", 0.2, 0.0, 1.0);
        B.pop();

        B.push("postures");
        POSTURES_ENABLED = B.comment("Команды /sit (сесть), /lay (лечь на спину) и /sleep (уснуть сидя или лёжа).",
                        "Позы — эмоции Emotecraft, их видят все. Встать — Shift или прыжок.")
                .define("enabled", true);
        POSTURE_STAND_DISTANCE = B.comment("Игрок сдвинулся дальше этого (блоки) от места, где сел или лёг, — встаёт.")
                .defineInRange("stand_distance", 0.6, 0.1, 5.0);
        B.pop();

        B.push("access");
        ACCESS_PERMISSION_LEVEL = B.comment("Ведущие: с таким уровнем прав на выделенном сервере игроку открыты все предметы RP-модов",
                        "(в сборках с замком). В одиночном мире и LAN не действует.")
                .defineInRange("gm_permission_level", 2, 1, 4);
        B.pop();
        SPEC = B.build();
    }
}
