package faygolover.rpcore.client.gm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Описание окна настроек «ГМской штуки»: поля (выбор, флажок, текст, число) и кнопки-помощники. Подписи —
 * {@code rpcore.gm.<id>.<поле>}, варианты — {@code rpcore.gm.<id>.<поле>.<значение>}. Другие моды семейства
 * (RP Portals) регистрируют свои схемы: {@link #register}.
 */
public final class GmSchema {
    public enum Kind { CHOICE, BOOL, TEXT, INT, FLOAT, DOUBLE }

    public record Field(String key, Kind kind, String[] values, double min, double max, int maxLen, Predicate<CompoundTag> visible) {}

    public record Action(String labelKey, Consumer<CompoundTag> apply) {}

    private static final Map<Block, GmSchema> SCHEMAS = new HashMap<>();

    public final String id;
    final List<Field> fields = new ArrayList<>();
    final List<Action> actions = new ArrayList<>();
    private Predicate<CompoundTag> when = t -> true;

    public GmSchema(String id) {
        this.id = id;
    }

    public static void register(Block block, GmSchema schema) {
        SCHEMAS.put(block, schema);
    }

    static GmSchema of(Block block) {
        return SCHEMAS.get(block);
    }

    /** Следующие поля видны, только если {@code key} — одно из {@code values}. {@link #always()} — снять условие. */
    public GmSchema when(String key, String... values) {
        when = t -> {
            String v = t.getString(key);
            for (String s : values) if (s.equals(v)) return true;
            return false;
        };
        return this;
    }

    /** Следующие поля видны при произвольном условии на текущие значения. */
    public GmSchema when(Predicate<CompoundTag> condition) {
        when = condition;
        return this;
    }

    /** Следующие поля видны, только если флажок {@code key} включён. */
    public GmSchema whenTrue(String key) {
        when = t -> t.getBoolean(key);
        return this;
    }

    public GmSchema always() {
        when = t -> true;
        return this;
    }

    public GmSchema choice(String key, String... values) {
        fields.add(new Field(key, Kind.CHOICE, values, 0, 0, 0, when));
        return this;
    }

    public GmSchema bool(String key) {
        fields.add(new Field(key, Kind.BOOL, null, 0, 0, 0, when));
        return this;
    }

    public GmSchema text(String key, int maxLen) {
        fields.add(new Field(key, Kind.TEXT, null, 0, 0, maxLen, when));
        return this;
    }

    public GmSchema integer(String key, int min, int max) {
        fields.add(new Field(key, Kind.INT, null, min, max, 12, when));
        return this;
    }

    public GmSchema decimal(String key, double min, double max) {
        fields.add(new Field(key, Kind.FLOAT, null, min, max, 12, when));
        return this;
    }

    public GmSchema coord(String key) {
        fields.add(new Field(key, Kind.DOUBLE, null, -3.0e7, 3.0e7, 16, when));
        return this;
    }

    public GmSchema action(String labelKey, Consumer<CompoundTag> apply) {
        actions.add(new Action(labelKey, apply));
        return this;
    }
}
