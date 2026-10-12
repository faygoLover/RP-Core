package faygolover.rpcore.api;

import faygolover.rpcore.photo.PhotoServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Фото игроков — сервер (медкарта RP Medicine, аватарки RP PDA). Фото — PNG скина игрока, снятый его клиентом
 * (скин с Ely.by часто есть только у клиента). Core сам просит снимок при входе, если фото ещё нет; хранит файлом в
 * мире и раздаёт клиентам. На клиенте — {@link faygolover.rpcore.client.photo.ClientPhotos}: текстура скина и лицо.
 * Методы — в потоке сервера.
 *
 * @since 0.4.0
 */
public final class Photos {
    private Photos() {}

    /** PNG фото (картинка скина) или null, если фото нет. */
    @Nullable
    public static byte[] png(MinecraftServer server, UUID player) {
        return PhotoServer.png(server, player);
    }

    public static boolean has(MinecraftServer server, UUID player) {
        return png(server, player) != null;
    }

    /**
     * Переснять: попросить клиент прислать скин заново. Не чаще раза в {@code photo.min_interval_seconds} конфига Core;
     * false — рано или запрос уже ждёт ответа. Когда снимок придёт — {@link faygolover.rpcore.api.event.PhotoChangedEvent}.
     */
    public static boolean request(ServerPlayer player) {
        return PhotoServer.request(player);
    }
}
