package faygolover.rpcore.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Сервер принял новую картинку по ключу или картинку удалили ({@link faygolover.rpcore.api.Pictures}). Клиентам
 * Core сам рассылает, что кэш устарел; событие — для мода-владельца ключа (например, отметить у себя, что картинка есть).
 *
 * @since 0.4.1
 */
public class PictureChangedEvent extends Event {
    private final String key;
    @Nullable
    private final ServerPlayer uploader;

    public PictureChangedEvent(String key, @Nullable ServerPlayer uploader) {
        this.key = key;
        this.uploader = uploader;
    }

    public String getKey() {
        return key;
    }

    /** Кто загрузил; null — картинку удалили ({@link faygolover.rpcore.api.Pictures#remove}). */
    @Nullable
    public ServerPlayer getUploader() {
        return uploader;
    }
}
