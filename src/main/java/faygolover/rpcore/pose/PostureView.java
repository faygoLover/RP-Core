package faygolover.rpcore.pose;

import faygolover.rpcore.api.RpCoreAPI.Posture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Клиент: какая поза у игроков вокруг (для хитбокса). Без клиентских классов — его читает общий код размеров. */
public final class PostureView {
    private PostureView() {}

    private static final Map<Integer, Posture> SHOWN = new ConcurrentHashMap<>();

    public static Posture get(int entityId) {
        return SHOWN.getOrDefault(entityId, Posture.NONE);
    }

    public static void set(int entityId, Posture p) {
        if (p == Posture.NONE) SHOWN.remove(entityId);
        else SHOWN.put(entityId, p);
    }

    public static void clear() {
        SHOWN.clear();
    }
}
