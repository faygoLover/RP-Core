package faygolover.rpcore.profile;

import faygolover.rpcore.api.BodyProfile;
import faygolover.rpcore.api.event.BodyProfileChangedEvent;
import faygolover.rpcore.network.CoreNetwork;
import faygolover.rpcore.network.ProfilePackets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Анкета тела: хранится в постоянных данных игрока ({@code PlayerPersisted} — переживает смерть), своему клиенту
 * уходит копия. Игрок заполняет анкету сам один раз ({@link #onSubmit}); дальше менять может только ГМ.
 */
public final class ProfileServer {
    private ProfileServer() {}

    private static final String TAG = "rpcore_profile";
    private static final String TASTE_TAG = "rpcore_taste";

    /** Клиентская копия своей анкеты (по UUID; на выделенном сервере пуста). */
    private static final Map<UUID, BodyProfile> CLIENT = new ConcurrentHashMap<>();
    private static final Map<UUID, faygolover.rpcore.api.FoodTaste> CLIENT_TASTE = new ConcurrentHashMap<>();

    public static BodyProfile get(Player p) {
        if (p.level().isClientSide) return CLIENT.getOrDefault(p.getUUID(), BodyProfile.DEFAULT);
        CompoundTag persisted = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return BodyProfile.load(persisted.getCompound(TAG));
    }

    /** Записать анкету (сервер): сохранить, отправить игроку, сообщить модам. */
    public static void set(ServerPlayer p, BodyProfile profile) {
        BodyProfile before = get(p);
        CompoundTag root = p.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(TAG, profile.save());
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        sync(p);
        if (!before.equals(profile)) MinecraftForge.EVENT_BUS.post(new BodyProfileChangedEvent(p, before, profile));
    }

    /** Игрок заполнил анкету в окне создания персонажа: принимается один раз, пока анкета не заполнена. */
    public static void onSubmit(ServerPlayer p, BodyProfile.Sex sex, int age, double height) {
        if (get(p).filled()) return;
        set(p, new BodyProfile(sex, age, height, true));
    }

    public static void sync(ServerPlayer p) {
        CoreNetwork.send(p, new ProfilePackets.Sync(get(p)));
        CoreNetwork.send(p, new ProfilePackets.TasteSync(taste(p)));
    }

    // ---------------------------------------------------------------- вкусы

    public static faygolover.rpcore.api.FoodTaste taste(Player p) {
        if (p.level().isClientSide) return CLIENT_TASTE.getOrDefault(p.getUUID(), faygolover.rpcore.api.FoodTaste.NONE);
        CompoundTag persisted = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return faygolover.rpcore.api.FoodTaste.load(persisted.getCompound(TASTE_TAG));
    }

    public static void setTaste(ServerPlayer p, faygolover.rpcore.api.FoodTaste taste) {
        CompoundTag root = p.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(TASTE_TAG, taste.save());
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        CoreNetwork.send(p, new ProfilePackets.TasteSync(taste));
    }

    /** Игрок выбрал вкусы сам: один раз (дальше — ГМ); пара должна быть допустимой. */
    public static void onSubmitTaste(ServerPlayer p, String favourite, String disliked) {
        if (taste(p).chosen() || !faygolover.rpcore.api.FoodTaste.validPair(favourite, disliked)) return;
        setTaste(p, new faygolover.rpcore.api.FoodTaste(favourite, disliked, true));
    }

    public static void onClientTaste(UUID uuid, faygolover.rpcore.api.FoodTaste taste) {
        CLIENT_TASTE.put(uuid, taste);
    }

    /** Клиент: пришла своя анкета. */
    public static void onClientSync(UUID uuid, BodyProfile profile) {
        CLIENT.put(uuid, profile);
    }

    public static void clearClient() {
        CLIENT.clear();
        CLIENT_TASTE.clear();
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) sync(sp);
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) sync(sp);
    }

    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) sync(sp);
    }
}
