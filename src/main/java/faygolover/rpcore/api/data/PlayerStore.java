package faygolover.rpcore.api.data;

import faygolover.rpcore.data.PlayerStores;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Данные игрока с общим жизненным циклом: хранение вместе с игроком, копия при смерти и возврате из Края, отправка
 * клиенту при входе, возрождении и смене измерения, уборка при выходе. Мод описывает данные один раз, RP Core сам
 * вызывает его в нужные моменты — одинаково для всех модов семейства.
 * <p>
 * <b>Когда использовать:</b> у мода есть состояние, которое принадлежит игроку и должно сохраняться вместе с ним.
 * Свою связку «capability + провайдер + обработчик Clone» писать не нужно.
 * <pre>{@code
 * public static final Capability<MuscleData> CAP = CapabilityManager.get(new CapabilityToken<>() {});
 * public static final PlayerStore<MuscleData> STORE = PlayerStore
 *         .of(new ResourceLocation("rpsports", "muscles"), MuscleData.class, CAP, MuscleData::new)
 *         .save(MuscleData::save)
 *         .load(MuscleData::load)
 *         .clone((from, to, ctx) -> { if (ctx.wasDeath()) to.copyRecords(from); else to.copyFrom(from); })
 *         .register();                       // в конструкторе мода
 *
 * MuscleData d = STORE.get(player);          // null, если данных нет
 * }</pre>
 * <b>Совместимость сохранений.</b> {@code id} — это имя ветки в {@code ForgeCaps} файла игрока. Мод, который переходит
 * на {@code PlayerStore} со своей capability, обязан передать прежнее имя, иначе данные игроков потеряются.
 * <p>
 * {@code Capability<T>} мод создаёт сам: обобщённый токен нельзя создать внутри RP Core для чужого типа.
 * Регистрировать тип в {@code RegisterCapabilitiesEvent} и вешать провайдер в {@code AttachCapabilitiesEvent} мод
 * не должен — это делает RP Core.
 *
 * @since 0.15.0
 */
public final class PlayerStore<T> {
    /** Почему данные надо отправить клиенту заново. */
    public enum Sync {
        /** Игрок вошёл на сервер. */
        LOGIN,
        /** Игрок возродился (после смерти или вернулся из Края). */
        RESPAWN,
        /** Игрок сменил измерение: клиент создал игрока заново и прежних данных у него нет. */
        DIMENSION
    }

    /**
     * Что известно о возрождении.
     *
     * @param oldPlayer прежний игрок (его данные уже доступны для чтения)
     * @param newPlayer новый игрок
     * @param wasDeath  true — смерть, false — возврат из Края
     */
    public record CloneContext(Player oldPlayer, Player newPlayer, boolean wasDeath) {}

    /** Правило копирования при возрождении: что перенести из прежних данных в новые. */
    @FunctionalInterface
    public interface CloneRule<T> {
        void copy(T from, T to, CloneContext ctx);
    }

    /** Миграция старого формата: тег версии {@code fromVersion} превратить в тег текущей версии. */
    @FunctionalInterface
    public interface Migration {
        CompoundTag migrate(CompoundTag tag, int fromVersion);
    }

    /** Имя поля с версией формата в теге данных (пишется, только если мод задал версию). */
    public static final String VERSION_KEY = "rp_data_version";

    private final ResourceLocation id;
    private final Class<T> type;
    private final Capability<T> capability;
    private final Supplier<T> factory;
    private final Function<T, CompoundTag> save;
    private final BiConsumer<T, CompoundTag> load;
    private final CloneRule<T> clone;
    @Nullable
    private final BiConsumer<ServerPlayer, Sync> resync;
    @Nullable
    private final Consumer<ServerPlayer> forget;
    private final int dataVersion;
    @Nullable
    private final Migration migration;
    private final boolean fakePlayers;

    private PlayerStore(Builder<T> b) {
        this.id = b.id;
        this.type = b.type;
        this.capability = b.capability;
        this.factory = b.factory;
        this.save = b.save;
        this.load = b.load;
        this.clone = b.clone;
        this.resync = b.resync;
        this.forget = b.forget;
        this.dataVersion = b.dataVersion;
        this.migration = b.migration;
        this.fakePlayers = b.fakePlayers;
    }

    /**
     * Начать описание хранилища.
     *
     * @param id         имя ветки в сохранении игрока ({@code ForgeCaps}); у переходящего мода — прежнее имя capability
     * @param type       класс данных
     * @param capability capability этого типа, созданная модом через {@code CapabilityManager.get(new CapabilityToken<>() {})}
     * @param factory    новые данные для нового игрока
     */
    public static <T> Builder<T> of(ResourceLocation id, Class<T> type, Capability<T> capability, Supplier<T> factory) {
        return new Builder<>(Objects.requireNonNull(id), Objects.requireNonNull(type), Objects.requireNonNull(capability),
                Objects.requireNonNull(factory));
    }

    /** Данные игрока или null (у чужих игроков на клиенте, у фальшивых игроков, если они исключены). */
    @Nullable
    public T get(Player player) {
        return player.getCapability(capability).resolve().orElse(null);
    }

    public ResourceLocation id() {
        return id;
    }

    public Class<T> type() {
        return type;
    }

    public Capability<T> capability() {
        return capability;
    }

    // ------------------------------------------------------------------ вызывает RP Core (PlayerStores)

    /** Новые данные для нового игрока. */
    public T create() {
        return factory.get();
    }

    /** Данные → тег для сохранения (с версией формата, если она задана). */
    public CompoundTag write(T data) {
        CompoundTag tag = save.apply(data);
        if (dataVersion > 0) tag.putInt(VERSION_KEY, dataVersion);
        return tag;
    }

    /** Тег из сохранения → данные (старый формат сначала проходит миграцию). */
    public void read(T data, CompoundTag tag) {
        if (dataVersion > 0 && migration != null) {
            int from = tag.getInt(VERSION_KEY);
            if (from < dataVersion) tag = migration.migrate(tag.copy(), from);
        }
        load.accept(data, tag);
    }

    public void copyOnClone(T from, T to, CloneContext ctx) {
        clone.copy(from, to, ctx);
    }

    public void resync(ServerPlayer player, Sync why) {
        if (resync != null) resync.accept(player, why);
    }

    public void forget(ServerPlayer player) {
        if (forget != null) forget.accept(player);
    }

    /** Вешать ли данные на фальшивых игроков (автоматы, {@code FakePlayer}). */
    public boolean onFakePlayers() {
        return fakePlayers;
    }

    // ------------------------------------------------------------------ описание

    public static final class Builder<T> {
        private final ResourceLocation id;
        private final Class<T> type;
        private final Capability<T> capability;
        private final Supplier<T> factory;
        private Function<T, CompoundTag> save;
        private BiConsumer<T, CompoundTag> load;
        private CloneRule<T> clone;
        private BiConsumer<ServerPlayer, Sync> resync;
        private Consumer<ServerPlayer> forget;
        private int dataVersion;
        private Migration migration;
        private boolean fakePlayers = true;

        private Builder(ResourceLocation id, Class<T> type, Capability<T> capability, Supplier<T> factory) {
            this.id = id;
            this.type = type;
            this.capability = capability;
            this.factory = factory;
        }

        /** Обязательно: данные → тег. Каждый раз новый тег. */
        public Builder<T> save(Function<T, CompoundTag> save) {
            this.save = save;
            return this;
        }

        /** Обязательно: тег → данные. Тег может быть пустым (старое сохранение) — тогда значения по умолчанию. */
        public Builder<T> load(BiConsumer<T, CompoundTag> load) {
            this.load = load;
            return this;
        }

        /**
         * Обязательно: что перенести при возрождении. Значения по умолчанию нет нарочно: «что переживает смерть» —
         * решение механики, а не библиотеки. Вызывается и при смерти, и при возврате из Края
         * ({@link CloneContext#wasDeath()}).
         */
        public Builder<T> clone(CloneRule<T> clone) {
            this.clone = clone;
            return this;
        }

        /** Скопировать всё и при смерти, и при возврате из Края: через запись в тег и чтение из него. */
        public Builder<T> cloneAll() {
            this.clone = null;
            this.cloneAllFlag = true;
            return this;
        }

        private boolean cloneAllFlag;

        /**
         * Отправить данные клиенту заново: вход, возрождение, смена измерения. Только сервер. Отправка должна быть
         * полной (весь снимок), а не разницей: клиент к этому моменту может не иметь прежних данных.
         */
        public Builder<T> resync(BiConsumer<ServerPlayer, Sync> resync) {
            this.resync = resync;
            return this;
        }

        /** Игрок вышел: убрать служебные таблицы по игроку, которые мод держит вне этих данных. */
        public Builder<T> forget(Consumer<ServerPlayer> forget) {
            this.forget = forget;
            return this;
        }

        /**
         * Версия формата данных (от 1) и миграция старых тегов. В тег пишется поле {@link #VERSION_KEY}; тег без
         * него считается версией 0. Мод, который версию не задаёт, поле не получает — формат его данных не меняется.
         */
        public Builder<T> dataVersion(int version, Migration migration) {
            if (version < 1) throw new IllegalArgumentException("Версия формата данных — от 1");
            this.dataVersion = version;
            this.migration = Objects.requireNonNull(migration);
            return this;
        }

        /** Не вешать данные на фальшивых игроков ({@code FakePlayer}). По умолчанию вешаются на всех. */
        public Builder<T> skipFakePlayers() {
            this.fakePlayers = false;
            return this;
        }

        /**
         * Зарегистрировать хранилище. Вызывать в конструкторе мода: позже RP Core уже начал вешать данные на
         * игроков, и регистрация бросит {@link IllegalStateException}.
         */
        public PlayerStore<T> register() {
            if (save == null || load == null) throw new IllegalStateException("PlayerStore " + id + ": не заданы save и load");
            if (cloneAllFlag) {
                Function<T, CompoundTag> s = save;
                BiConsumer<T, CompoundTag> l = load;
                clone = (from, to, ctx) -> l.accept(to, s.apply(from));
            }
            if (clone == null) throw new IllegalStateException("PlayerStore " + id + ": не задано правило clone (или cloneAll)");
            PlayerStore<T> store = new PlayerStore<>(this);
            PlayerStores.add(store);
            return store;
        }
    }
}
