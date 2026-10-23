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

    /** Где висит подпись (мировые координаты); по умолчанию — над блоком. */
    default net.minecraft.world.phys.Vec3 markerLabelPos() {
        return ((net.minecraft.world.level.block.entity.BlockEntity) this).getBlockPos().getCenter().add(0, 0.7, 0);
    }

    /** Дополнительные линии подсветки (отрезки в мировых координатах): область, стрелки. */
    default java.util.List<net.minecraft.world.phys.Vec3[]> markerLines() {
        return java.util.List.of();
    }
}
