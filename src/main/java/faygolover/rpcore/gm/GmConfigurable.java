package faygolover.rpcore.gm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Блок-сущность «ГМской штуки» с настройками: окно ведущего получает их с сервера ({@link #writeSettings}),
 * возвращает изменённые ({@link #applyFrom}). Сервер проверяет права и сам чистит значения.
 */
public interface GmConfigurable {
    CompoundTag writeSettings();

    void readSettings(CompoundTag tag);

    /** Применить настройки от ведущего (права уже проверены). */
    void applyFrom(ServerPlayer gm, CompoundTag tag);

    /** Подпись у контура для ведущего (имя, предмет); пусто — без подписи. */
    default String markerLabel() {
        return "";
    }
}
