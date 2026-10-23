package faygolover.rpcore.client;

import net.minecraftforge.common.ForgeConfigSpec;

/** Клиентский конфиг RP Core: {@code config/rpcore-client.toml} — ключ ведущего, режим пустой руки, вид эффектов. */
public final class CoreClientConfig {
    private CoreClientConfig() {}

    /** Когда видна рука от первого лица, если в ней ничего нет. */
    public enum HandMode { ALWAYS, WITH_ITEM, ITEM_AND_ACTION }

    /** Вид «Красных глаз»: светящиеся шары к зрителю или плоские пятна на лице (как в 0.8.6). */
    public enum EyesStyle { SPHERE, FLAT }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> KEY;
    public static final ForgeConfigSpec.EnumValue<HandMode> HAND_MODE;
    public static final ForgeConfigSpec.EnumValue<EyesStyle> EYES_STYLE;

    static {
        var b = new ForgeConfigSpec.Builder();
        b.push("access");
        KEY = b.comment("Ключ ведущего RP, если выдан.").define("key", "");
        b.pop();
        b.push("hand");
        HAND_MODE = b.comment("Пустая рука от первого лица: ALWAYS — видна всегда (как в ванилле), WITH_ITEM — только с предметом,",
                        "ITEM_AND_ACTION — с предметом и во время действия (удар, карабканье, плавание, ползком).",
                        "Переключается клавишей «Пустая рука» (RP Core) в управлении.")
                .defineEnum("empty_hand", HandMode.ITEM_AND_ACTION);
        b.pop();
        b.push("effects");
        EYES_STYLE = b.comment("Эффект «Красные глаза»: SPHERE — ярко светящиеся красные шары, всегда повёрнуты к зрителю, с ореолом;",
                        "FLAT — плоские светящиеся пятна на лице с лучами вперёд.")
                .defineEnum("red_eyes_style", EyesStyle.SPHERE);
        b.pop();
        SPEC = b.build();
    }
}
