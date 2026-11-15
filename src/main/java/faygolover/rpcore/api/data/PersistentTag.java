package faygolover.rpcore.api.data;

import faygolover.rpcore.data.PlayerStores;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Простые данные игрока в виде тега: раздел в {@code getPersistentData()} игрока ({@code ForgeData} в его файле).
 * Сохраняется вместе с игроком сам; при возрождении ваниль его <b>не переносит</b> — это делает правило, которое
 * задаёт мод, а вызывает RP Core.
 * <p>
 * <b>Когда использовать:</b> данных немного, отдельный класс и capability им не нужны (несколько чисел и флагов).
 * Для состояния со своим классом — {@link PlayerStore}.
 * <pre>{@code
 * public static final PersistentTag DATA = PersistentTag.of("rpneeds")
 *         .clone((old, ctx) -> { old.putBoolean("asleep", false); return old; })   // old — уже копия
 *         .register();                                                              // в конструкторе мода
 *
 * CompoundTag t = DATA.of(player);          // раздел игрока, создаётся при первом обращении
 * }</pre>
 * <b>Совместимость сохранений.</b> Имя раздела — как оно уже лежит в файлах игроков; менять нельзя.
 *
 * @since 0.15.0
 */
public final class PersistentTag {
    private final String key;
    private final BiFunction<CompoundTag, PlayerStore.CloneContext, CompoundTag> clone;

    private PersistentTag(String key, BiFunction<CompoundTag, PlayerStore.CloneContext, CompoundTag> clone) {
        this.key = key;
        this.clone = clone;
    }

    /** Начать описание раздела с этим именем в {@code getPersistentData()} игрока. */
    public static Builder of(String key) {
        return new Builder(Objects.requireNonNull(key));
    }

    public String key() {
        return key;
    }

    /** Раздел игрока; если его ещё нет — создаётся пустой и остаётся у игрока. */
    public CompoundTag of(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(key, 10)) root.put(key, new CompoundTag());
        return root.getCompound(key);
    }

    /** Раздел игрока или null, если его нет (ничего не создаёт). */
    @Nullable
    public CompoundTag peek(Player player) {
        CompoundTag root = player.getPersistentData();
        return root.contains(key, 10) ? root.getCompound(key) : null;
    }

    /** Вызывает RP Core при возрождении: перенести раздел прежнего игрока новому по правилу мода. */
    public void copyOnClone(PlayerStore.CloneContext ctx) {
        CompoundTag old = peek(ctx.oldPlayer());
        if (old == null) return;
        CompoundTag result = clone.apply(old.copy(), ctx);
        if (result != null) ctx.newPlayer().getPersistentData().put(key, result);
    }

    public static final class Builder {
        private final String key;
        private BiFunction<CompoundTag, PlayerStore.CloneContext, CompoundTag> clone;

        private Builder(String key) {
            this.key = key;
        }

        /**
         * Обязательно: что перенести при возрождении. Правило получает <b>копию</b> раздела прежнего игрока, может
         * её поправить и вернуть; null — не переносить ничего. Вызывается и при смерти, и при возврате из Края.
         * Если у прежнего игрока раздела не было, правило не вызывается.
         */
        public Builder clone(BiFunction<CompoundTag, PlayerStore.CloneContext, CompoundTag> clone) {
            this.clone = clone;
            return this;
        }

        /** Переносить раздел целиком и при смерти, и при возврате из Края. */
        public Builder cloneAll() {
            this.clone = (old, ctx) -> old;
            return this;
        }

        /** Зарегистрировать. Вызывать в конструкторе мода. */
        public PersistentTag register() {
            if (clone == null) throw new IllegalStateException("PersistentTag " + key + ": не задано правило clone (или cloneAll)");
            PersistentTag tag = new PersistentTag(key, clone);
            PlayerStores.add(tag);
            return tag;
        }
    }
}
