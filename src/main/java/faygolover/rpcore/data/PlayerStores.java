package faygolover.rpcore.data;

import faygolover.rpcore.RpCore;
import faygolover.rpcore.api.data.PersistentTag;
import faygolover.rpcore.api.data.PlayerStore;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Общий жизненный цикл данных игрока: все {@link PlayerStore} и {@link PersistentTag} семейства. RP Core один раз
 * подписывается на события игрока и раздаёт их хранилищам — всем одинаково и в одном порядке (по имени хранилища).
 * <p>
 * Моды сюда не обращаются: описание и доступ — через {@link PlayerStore} и {@link PersistentTag}.
 */
public final class PlayerStores {
    private PlayerStores() {}

    private static final List<PlayerStore<?>> STORES = new ArrayList<>();
    private static final List<PersistentTag> TAGS = new ArrayList<>();
    /** После начала загрузки мира список закрыт: данные уже вешаются на игроков. */
    private static volatile boolean frozen;
    private static volatile PlayerStore<?>[] stores = new PlayerStore<?>[0];
    private static volatile PersistentTag[] tags = new PersistentTag[0];

    /** Конструкторы модов идут в разных потоках — поэтому под замком, а порядок потом выравнивается по имени. */
    public static void add(PlayerStore<?> store) {
        synchronized (STORES) {
            if (frozen) throw new IllegalStateException("PlayerStore " + store.id() + " зарегистрирован слишком поздно: "
                    + "вызывай register() в конструкторе мода");
            for (PlayerStore<?> s : STORES)
                if (s.id().equals(store.id())) throw new IllegalStateException("PlayerStore " + store.id() + " уже зарегистрирован");
            STORES.add(store);
        }
    }

    public static void add(PersistentTag tag) {
        synchronized (STORES) {
            if (frozen) throw new IllegalStateException("PersistentTag " + tag.key() + " зарегистрирован слишком поздно: "
                    + "вызывай register() в конструкторе мода");
            for (PersistentTag t : TAGS)
                if (t.key().equals(tag.key())) throw new IllegalStateException("PersistentTag " + tag.key() + " уже зарегистрирован");
            TAGS.add(tag);
        }
    }

    public static void init(IEventBus modBus, IEventBus bus) {
        modBus.addListener(PlayerStores::onRegisterCaps);
        bus.addGenericListener(Entity.class, PlayerStores::onAttach);
        bus.addListener(PlayerStores::onClone);
        bus.addListener(PlayerStores::onLogin);
        bus.addListener(PlayerStores::onRespawn);
        bus.addListener(PlayerStores::onDimension);
        bus.addListener(PlayerStores::onLogout);
    }

    /** Закрыть список и выровнять порядок. Идёт после конструкторов всех модов. */
    private static void freeze() {
        if (frozen) return;
        synchronized (STORES) {
            if (frozen) return;
            STORES.sort(Comparator.comparing(s -> s.id().toString()));
            TAGS.sort(Comparator.comparing(PersistentTag::key));
            stores = STORES.toArray(new PlayerStore<?>[0]);
            tags = TAGS.toArray(new PersistentTag[0]);
            frozen = true;
            if (stores.length + tags.length > 0)
                RpCore.LOGGER.info("RP Core: данные игрока — хранилищ {}, разделов {}", stores.length, tags.length);
        }
    }

    private static void onRegisterCaps(RegisterCapabilitiesEvent e) {
        freeze();
        for (PlayerStore<?> s : stores) e.register(s.type());
    }

    private static void onAttach(AttachCapabilitiesEvent<Entity> e) {
        if (!(e.getObject() instanceof Player player)) return;
        freeze();
        boolean fake = player instanceof FakePlayer;
        for (PlayerStore<?> s : stores) {
            if (fake && !s.onFakePlayers()) continue;
            e.addCapability(s.id(), new Provider<>(s));
        }
    }

    private static void onClone(PlayerEvent.Clone e) {
        PlayerStore<?>[] ss = stores;
        PersistentTag[] ts = tags;
        if (ss.length == 0 && ts.length == 0) return;
        Player old = e.getOriginal();
        PlayerStore.CloneContext ctx = new PlayerStore.CloneContext(old, e.getEntity(), e.isWasDeath());
        if (ss.length > 0) {
            // Capability прежнего игрока к этому моменту закрыты; обработчики других модов могли открыть и снова
            // закрыть их до нас — поэтому открываем сами прямо перед чтением.
            old.reviveCaps();
            try {
                for (PlayerStore<?> s : ss) copy(s, ctx);
            } finally {
                old.invalidateCaps();
            }
        }
        for (PersistentTag t : ts) {
            try {
                t.copyOnClone(ctx);
            } catch (RuntimeException ex) {
                RpCore.LOGGER.error("RP Core: раздел данных {} не перенесён при возрождении", t.key(), ex);
            }
        }
    }

    private static <T> void copy(PlayerStore<T> s, PlayerStore.CloneContext ctx) {
        T from = s.get(ctx.oldPlayer());
        T to = s.get(ctx.newPlayer());
        if (from == null || to == null) return;
        try {
            s.copyOnClone(from, to, ctx);
        } catch (RuntimeException ex) {
            // Сбой одного мода не должен оставить без данных остальные.
            RpCore.LOGGER.error("RP Core: данные {} не перенесены при возрождении", s.id(), ex);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        resync(e.getEntity(), PlayerStore.Sync.LOGIN);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        resync(e.getEntity(), PlayerStore.Sync.RESPAWN);
    }

    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        resync(e.getEntity(), PlayerStore.Sync.DIMENSION);
    }

    private static void resync(Player player, PlayerStore.Sync why) {
        if (!(player instanceof ServerPlayer sp)) return;
        for (PlayerStore<?> s : stores) {
            try {
                s.resync(sp, why);
            } catch (RuntimeException ex) {
                RpCore.LOGGER.error("RP Core: данные {} не отправлены клиенту ({})", s.id(), why, ex);
            }
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        for (PlayerStore<?> s : stores) {
            try {
                s.forget(sp);
            } catch (RuntimeException ex) {
                RpCore.LOGGER.error("RP Core: уборка данных {} при выходе не удалась", s.id(), ex);
            }
        }
    }

    /** Данные одного хранилища у одного игрока. Не гасится при закрытии capability: иначе их не прочитать при возрождении. */
    private static final class Provider<T> implements ICapabilitySerializable<CompoundTag> {
        private final PlayerStore<T> store;
        private final T data;
        private final LazyOptional<T> optional;

        Provider(PlayerStore<T> store) {
            this.store = store;
            this.data = store.create();
            this.optional = LazyOptional.of(() -> data);
        }

        @Override
        public @NotNull <C> LazyOptional<C> getCapability(@NotNull Capability<C> cap, @Nullable Direction side) {
            return store.capability().orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return store.write(data);
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            store.read(data, nbt);
        }
    }
}
