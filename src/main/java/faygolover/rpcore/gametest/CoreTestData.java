package faygolover.rpcore.gametest;

import faygolover.rpcore.RpCore;
import faygolover.rpcore.api.data.PersistentTag;
import faygolover.rpcore.api.data.PlayerStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Тестовые данные игрока для игровых тестов RP Core. Регистрируются из конструктора мода только при запуске
 * {@code runGameTestServer} самого Core (см. {@code RpCore.registerTestData}); в релизном jar этого класса нет.
 */
public final class CoreTestData {
    /** Простые данные: число сбрасывается смертью, строка переживает её. */
    public int value;
    public String note = "";

    public static final Capability<CoreTestData> CAP = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = new ResourceLocation(RpCore.MODID, "test_store");
    public static final ResourceLocation ID_VERSIONED = new ResourceLocation(RpCore.MODID, "test_versioned");

    /** Версионированные данные — отдельный тип: у одного типа одна capability. */
    public static final class Versioned {
        public int value;
        public static final Capability<Versioned> CAP = CapabilityManager.get(new CapabilityToken<>() {});
    }

    public static PlayerStore<CoreTestData> STORE;
    public static PlayerStore<Versioned> VERSIONED;
    public static PersistentTag TAG;

    /** Журнал вызовов жизненного цикла: «LOGIN имя», «RESPAWN имя», «FORGET имя», «CLONE death|end». */
    public static final List<String> LOG = new ArrayList<>();

    public static void register() {
        STORE = PlayerStore.of(ID, CoreTestData.class, CAP, CoreTestData::new)
                .save(d -> {
                    CompoundTag t = new CompoundTag();
                    t.putInt("value", d.value);
                    t.putString("note", d.note);
                    return t;
                })
                .load((d, t) -> {
                    d.value = t.getInt("value");
                    d.note = t.getString("note");
                })
                .clone((from, to, ctx) -> {
                    LOG.add("CLONE " + (ctx.wasDeath() ? "death" : "end"));
                    to.note = from.note;
                    if (!ctx.wasDeath()) to.value = from.value;
                })
                .resync((sp, why) -> LOG.add(why + " " + sp.getGameProfile().getName()))
                .forget(sp -> LOG.add("FORGET " + sp.getGameProfile().getName()))
                .register();
        VERSIONED = PlayerStore.of(ID_VERSIONED, Versioned.class, Versioned.CAP, Versioned::new)
                .save(d -> {
                    CompoundTag t = new CompoundTag();
                    t.putInt("value2", d.value);
                    return t;
                })
                .load((d, t) -> d.value = t.getInt("value2"))
                .cloneAll()
                // Версия 0 хранила число под именем «old».
                .dataVersion(2, (tag, from) -> {
                    if (from < 1) tag.putInt("value2", tag.getInt("old") * 10);
                    return tag;
                })
                .register();
        TAG = PersistentTag.of("rpcore_test")
                .clone((old, ctx) -> {
                    if (ctx.wasDeath()) old.putBoolean("asleep", false);
                    return old;
                })
                .register();
    }

    private CoreTestData() {}
}
