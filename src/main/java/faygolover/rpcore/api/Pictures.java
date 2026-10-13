package faygolover.rpcore.api;

import faygolover.rpcore.photo.PictureServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Картинки по ключу — сервер (свои аватарки RP PDA, в том числе у NPC). В отличие от {@link Photos} не привязаны к
 * игроку: ключ задаёт мод, например {@code "rppda:avatar/12345678"} (строчные латинские буквы, цифры, «_ - . /»).
 * <p>Загрузка — в два шага: мод на сервере решает, что игроку можно, и вызывает {@link #allow}; клиент отправляет
 * PNG через {@link faygolover.rpcore.client.photo.ClientPictures#upload}. Core принимает только по разрешению, до 30 КБ,
 * размер — из заголовка до распаковки. Принял — {@link faygolover.rpcore.api.event.PictureChangedEvent}.
 * На клиенте — {@link faygolover.rpcore.client.photo.ClientPictures}. Методы — в потоке сервера.
 *
 * @since 0.4.1
 */
public final class Pictures {
    private Pictures() {}

    /** PNG картинки или null, если её нет. */
    @Nullable
    public static byte[] png(MinecraftServer server, String key) {
        return PictureServer.png(server, key);
    }

    public static boolean has(MinecraftServer server, String key) {
        return png(server, key) != null;
    }

    /**
     * Разрешить игроку одну загрузку картинки {@code key} ровно {@code width}×{@code height} (16–512) в течение минуты.
     * Новое разрешение заменяет прежнее. Как часто разрешать — решает мод.
     */
    public static void allow(ServerPlayer player, String key, int width, int height) {
        PictureServer.allow(player, key, width, height);
    }

    /** Удалить картинку (например, ГМ сбросил аватарку). */
    public static void remove(MinecraftServer server, String key) {
        PictureServer.remove(server, key);
    }

    public static boolean validKey(String key) {
        return PictureServer.validKey(key);
    }
}
