package faygolover.rpcore.photo;

import faygolover.rpcore.CoreConfig;
import faygolover.rpcore.RpCore;
import faygolover.rpcore.api.event.PhotoChangedEvent;
import faygolover.rpcore.network.CoreNetwork;
import faygolover.rpcore.network.PhotoPackets;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Служба фото на сервере (ревью 11.10, п. 1). Фото — PNG скина игрока, снятый его же клиентом: скин с Ely.by часто
 * есть только у клиента. Правила:
 * <ul>
 *   <li>снимок принимается только после запроса сервера и не чаще раза в {@code photo.min_interval_seconds};</li>
 *   <li>до 30 КБ; ширина и высота читаются из заголовка PNG до распаковки: 64–512 × 32–512;</li>
 *   <li>хранится файлом {@code <мир>/rpcore/photos/<uuid>.png}; клиенты получают по запросу и кэшируют у себя.</li>
 * </ul>
 * Всё — в потоке сервера (пакеты приходят через enqueueWork), запись файла — в фоне.
 */
public final class PhotoServer {
    private PhotoServer() {}

    public static final int MAX_BYTES = 30000;
    /** Сколько фото держать в памяти (остальные читаются с диска). */
    private static final int CACHE_SIZE = 64;
    /** Сколько запросов чужих фото принимать от одного клиента за секунду. */
    private static final int QUERIES_PER_SECOND = 20;
    private static final byte[] NONE = new byte[0];

    /** Кого попросили прислать снимок и ещё не получили ответ. */
    private static final Set<UUID> ASKED = new HashSet<>();
    /** Когда принят последний снимок (мс). */
    private static final Map<UUID, Long> LAST = new HashMap<>();
    /** Счётчики запросов чужих фото: [секунда, сколько]. */
    private static final Map<UUID, long[]> QUERIES = new HashMap<>();
    private static final Map<UUID, byte[]> CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, byte[]> e) {
            return size() > CACHE_SIZE;
        }
    };

    private static Path dir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("rpcore").resolve("photos");
    }

    private static Path file(MinecraftServer server, UUID uuid) {
        return dir(server).resolve(uuid + ".png");
    }

    // ------------------------------------------------------------------ для API

    /** PNG фото или null. */
    @Nullable
    public static byte[] png(MinecraftServer server, UUID uuid) {
        byte[] png = CACHE.get(uuid);
        if (png == null) {
            Path f = file(server, uuid);
            try {
                png = Files.exists(f) ? Files.readAllBytes(f) : NONE;
            } catch (IOException e) {
                RpCore.LOGGER.warn("RP Core: не прочитать фото {}: {}", f, e.toString());
                png = NONE;
            }
            CACHE.put(uuid, png);
        }
        return png.length == 0 ? null : png;
    }

    /** Попросить игрока прислать снимок (не чаще раза в интервал). true — запрос ушёл. */
    public static boolean request(ServerPlayer sp) {
        UUID id = sp.getUUID();
        if (tooSoon(id) || ASKED.contains(id)) return false;
        ASKED.add(id);
        CoreNetwork.send(sp, new PhotoPackets.Request());
        return true;
    }

    private static boolean tooSoon(UUID id) {
        Long last = LAST.get(id);
        return last != null && System.currentTimeMillis() - last < CoreConfig.PHOTO_INTERVAL.get() * 1000L;
    }

    // ------------------------------------------------------------------ сеть

    /** Снимок от клиента: только после запроса, только настоящий PNG разумного размера. */
    public static void onUpload(ServerPlayer sp, byte[] png) {
        UUID id = sp.getUUID();
        if (!ASKED.remove(id) || tooSoon(id)) return;
        if (png.length == 0 || png.length > MAX_BYTES || !validPng(png)) {
            RpCore.LOGGER.warn("RP Core: снимок от {} отклонён ({} байт)", sp.getGameProfile().getName(), png.length);
            return;
        }
        LAST.put(id, System.currentTimeMillis());
        CACHE.put(id, png);
        save(sp.server, id, png);
        CoreNetwork.sendToAll(new PhotoPackets.Changed(id));
        MinecraftForge.EVENT_BUS.post(new PhotoChangedEvent(sp));
    }

    /** Клиент просит чужое фото (медкарта, аватарка КПК). */
    public static void onQuery(ServerPlayer sp, UUID uuid) {
        long second = System.currentTimeMillis() / 1000;
        long[] q = QUERIES.computeIfAbsent(sp.getUUID(), k -> new long[2]);
        if (q[0] != second) {
            q[0] = second;
            q[1] = 0;
        }
        if (++q[1] > QUERIES_PER_SECOND) return;
        byte[] png = png(sp.server, uuid);
        CoreNetwork.send(sp, new PhotoPackets.Data(uuid, png == null ? NONE : png));
    }

    /** Размер из заголовка, без распаковки картинки. */
    static boolean validPng(byte[] png) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(png))) {
            if (in == null) return false;
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("png");
            if (!readers.hasNext()) return false;
            ImageReader r = readers.next();
            try {
                r.setInput(in, true, true);
                int w = r.getWidth(0), h = r.getHeight(0);
                return w >= 64 && w <= 512 && h >= 32 && h <= 512;
            } finally {
                r.dispose();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static void save(MinecraftServer server, UUID uuid, byte[] png) {
        Path d = dir(server);
        Util.ioPool().execute(() -> {
            try {
                Files.createDirectories(d);
                Path tmp = d.resolve(uuid + ".png.tmp");
                Files.write(tmp, png);
                Files.move(tmp, d.resolve(uuid + ".png"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                RpCore.LOGGER.error("RP Core: не сохранить фото {}: {}", uuid, e.toString());
            }
        });
    }

    // ------------------------------------------------------------------ события

    /** Вход: фото ещё нет — попросить снимок. */
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && png(sp.server, sp.getUUID()) == null) request(sp);
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        ASKED.remove(id);
        LAST.remove(id);
        QUERIES.remove(id);
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        ASKED.clear();
        LAST.clear();
        QUERIES.clear();
        CACHE.clear();
    }
}
