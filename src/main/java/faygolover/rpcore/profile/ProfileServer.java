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

    /** Клиентская копия своей анкеты (по UUID; на выделенном сервере пуста). */
    private static final Map<UUID, BodyProfile> CLIENT = new ConcurrentHashMap<>();

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
    }

    /** Клиент: пришла своя анкета. */
    public static void onClientSync(UUID uuid, BodyProfile profile) {
        CLIENT.put(uuid, profile);
    }

    public static void clearClient() {
        CLIENT.clear();
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
