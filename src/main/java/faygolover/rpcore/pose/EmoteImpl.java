package faygolover.rpcore.pose;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import faygolover.rpcore.RpCore;
import faygolover.rpcore.api.RpCoreAPI;
import io.github.kosmx.emotes.api.events.server.ServerEmoteAPI;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Вызовы Emotecraft. Грузится, только если он стоит ({@link Postures#emotecraft}). */
final class EmoteImpl {
    private EmoteImpl() {}

    private static final Map<RpCoreAPI.Posture, KeyframeAnimation> CACHE = new EnumMap<>(RpCoreAPI.Posture.class);

    static void play(UUID player, RpCoreAPI.Posture posture) {
        KeyframeAnimation a = CACHE.computeIfAbsent(posture, EmoteImpl::load);
        if (a != null) ServerEmoteAPI.forcePlayEmote(player, a);
    }

    static void stop(UUID player) {
        if (ServerEmoteAPI.getPlayedEmote(player) != null) ServerEmoteAPI.playEmote(player, null, false);
    }

    private static KeyframeAnimation load(RpCoreAPI.Posture posture) {
        String file = posture == RpCoreAPI.Posture.LAY ? "lay.emotecraft" : "sit.emotecraft";
        try (InputStream in = EmoteImpl.class.getResourceAsStream("/assets/rpcore/poses/" + file)) {
            if (in == null) return null;
            List<KeyframeAnimation> list = ServerEmoteAPI.deserializeEmote(in, file, "emotecraft");
            return list == null || list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            RpCore.LOGGER.warn("RP Core: не загрузилась поза {}: {}", file, e.toString());
            return null;
        }
    }
}
