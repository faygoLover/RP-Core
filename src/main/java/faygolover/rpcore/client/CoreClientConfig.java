package faygolover.rpcore.client;

import net.minecraftforge.common.ForgeConfigSpec;

/** Клиентский конфиг RP Core: {@code config/rpcore-client.toml} — ключ ведущего и режим пустой руки. */
public final class CoreClientConfig {
    private CoreClientConfig() {}

    /** Когда видна рука от первого лица, если в ней ничего нет. */
    public enum HandMode { ALWAYS, WITH_ITEM, ITEM_AND_ACTION }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> KEY;
    public static final ForgeConfigSpec.EnumValue<HandMode> HAND_MODE;

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
        SPEC = b.build();
    }
}
