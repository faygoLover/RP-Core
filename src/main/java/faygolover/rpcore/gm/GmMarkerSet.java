package faygolover.rpcore.gm;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Клиент: загруженные блок-сущности «ГМских штук» (пластины, держатели, порталы RP Portals). Подсветка ведущего
 * рисует их контуры по этому списку, без перебора блоков вокруг. Блоки сами добавляются при загрузке и убираются
 * при удалении; слабые ссылки — на случай, если чанк выгрузился без {@code setRemoved}.
 */
public final class GmMarkerSet {
    private GmMarkerSet() {}

    private static final Set<BlockEntity> ALL = Collections.newSetFromMap(new WeakHashMap<>());

    public static synchronized void add(BlockEntity be) {
        ALL.add(be);
    }

    public static synchronized void remove(BlockEntity be) {
        ALL.remove(be);
    }

    public static synchronized BlockEntity[] snapshot() {
        return ALL.toArray(new BlockEntity[0]);
    }

    public static synchronized void clear() {
        ALL.clear();
    }
}
