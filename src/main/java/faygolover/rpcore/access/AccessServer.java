package faygolover.rpcore.access;

import faygolover.rpcore.CoreConfig;
import faygolover.rpcore.network.AccessPackets;
import faygolover.rpcore.network.CoreNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Выдача доступа к скрытым предметам с сервера: ведущим (уровень прав из {@code [access]} конфига Core) на
 * выделенном сервере. В одиночном мире и в LAN не выдаётся — там игрок сам себе оператор, нужен ключ.
 */
public final class AccessServer {
    private AccessServer() {}

    private static final Map<UUID, Boolean> SENT = new ConcurrentHashMap<>();

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) update(sp, true);
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        SENT.remove(e.getEntity().getUUID());
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        SENT.clear();
    }

    /** Права могут поменяться посреди сессии (/op, /deop) — проверяем раз в 2 секунды. */
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.getServer().getTickCount() % 40 != 0) return;
        for (ServerPlayer sp : e.getServer().getPlayerList().getPlayers()) update(sp, false);
    }

    private static void update(ServerPlayer sp, boolean force) {
        MinecraftServer server = sp.getServer();
        boolean grant = server != null && server.isDedicatedServer()
                && sp.hasPermissions(CoreConfig.ACCESS_PERMISSION_LEVEL.get());
        Boolean was = SENT.put(sp.getUUID(), grant);
        if (force || was == null || was != grant) CoreNetwork.send(sp, new AccessPackets.Grant(grant));
    }
}
