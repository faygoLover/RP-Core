package faygolover.rpcore.photo;

import faygolover.rpcore.RpCore;
import faygolover.rpcore.api.event.PictureChangedEvent;
import faygolover.rpcore.network.CoreNetwork;
import faygolover.rpcore.network.PicturePackets;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Картинки по ключу (свои аватарки КПК, в том числе у NPC): в отличие от фото, не привязаны к игроку. Правила те же:
 * <ul>
 *   <li>картинка принимается, только если мод на сервере разрешил этому игроку загрузить именно этот ключ
 *       ({@link #allow}), один раз и в течение {@link #PERMIT_MS};</li>
 *   <li>до {@link PhotoServer#MAX_BYTES}; ширина и высота читаются из заголовка PNG до распаковки и должны совпасть
 *       с разрешёнными;</li>
 *   <li>файл {@code <мир>/rpcore/pictures/<namespace>/<path>.png}; клиенты получают по запросу и кэшируют.</li>
 * </ul>
 */
public final class PictureServer {
    private PictureServer() {}

    public static final int MAX_KEY = 128;
    static final long PERMIT_MS = 60_000L;
    private static final int CACHE_SIZE = 64;
    private static final int QUERIES_PER_SECOND = 20;
    private static final byte[] NONE = new byte[0];

    private record Permit(String key, int width, int height, long until) {}

    private static final Map<UUID, Permit> PERMITS = new HashMap<>();
    private static final Map<UUID, long[]> QUERIES = new HashMap<>();
    private static final Map<String, byte[]> CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> e) {
            return size() > CACHE_SIZE;
        }
    };

    /** Ключ — «мод:путь» из строчных латинских букв, цифр, «_ - . /», без пустых, «.» и «..» частей пути. */
    public static boolean validKey(String key) {
        if (key == null || key.isEmpty() || key.length() > MAX_KEY) return false;
        ResourceLocation rl = ResourceLocation.tryParse(key);
        if (rl == null || !key.contains(":")) return false;
        for (String part : rl.getPath().split("/", -1)) if (part.isEmpty() || part.equals(".") || part.equals("..")) return false;
        return true;
    }

    @Nullable
    private static Path file(MinecraftServer server, String key) {
        ResourceLocation rl = ResourceLocation.tryParse(key);
        if (rl == null) return null;
        return server.getWorldPath(LevelResource.ROOT).resolve("rpcore").resolve("pictures")
                .resolve(rl.getNamespace()).resolve(rl.getPath() + ".png");
    }

    // ------------------------------------------------------------------ для API

    @Nullable
    public static byte[] png(MinecraftServer server, String key) {
        if (!validKey(key)) return null;
        byte[] png = CACHE.get(key);
        if (png == null) {
            Path f = file(server, key);
            try {
                png = f != null && Files.exists(f) ? Files.readAllBytes(f) : NONE;
            } catch (IOException e) {
                RpCore.LOGGER.warn("RP Core: не прочитать картинку {}: {}", f, e.toString());
                png = NONE;
            }
            CACHE.put(key, png);
        }
        return png.length == 0 ? null : png;
    }

    /** Разрешить игроку одну загрузку картинки {@code key} размером ровно {@code width}×{@code height}. */
    public static void allow(ServerPlayer sp, String key, int width, int height) {
        if (!validKey(key)) throw new IllegalArgumentException("RP Core: неверный ключ картинки: " + key);
        if (width < 16 || width > 512 || height < 16 || height > 512)
            throw new IllegalArgumentException("RP Core: размер картинки 16–512: " + width + "×" + height);
        PERMITS.put(sp.getUUID(), new Permit(key, width, height, System.currentTimeMillis() + PERMIT_MS));
    }

    public static void remove(MinecraftServer server, String key) {
        if (!validKey(key)) return;
        CACHE.put(key, NONE);
        Path f = file(server, key);
        if (f != null) Util.ioPool().execute(() -> {
            try {
                Files.deleteIfExists(f);
            } catch (IOException e) {
                RpCore.LOGGER.warn("RP Core: не удалить картинку {}: {}", f, e.toString());
            }
        });
        CoreNetwork.sendToAll(new PicturePackets.Changed(key));
        MinecraftForge.EVENT_BUS.post(new PictureChangedEvent(key, null));
    }

    // ------------------------------------------------------------------ сеть

    /** Картинка от клиента: только по разрешению на этот ключ, только настоящий PNG разрешённого размера. */
    public static void onUpload(ServerPlayer sp, String key, byte[] png) {
        Permit p = PERMITS.get(sp.getUUID());
        if (p == null || !p.key.equals(key) || p.until < System.currentTimeMillis()) return;
        PERMITS.remove(sp.getUUID());
        if (png.length == 0 || png.length > PhotoServer.MAX_BYTES || !sizeIs(png, p.width, p.height)) {
            RpCore.LOGGER.warn("RP Core: картинка {} от {} отклонена ({} байт)", key, sp.getGameProfile().getName(), png.length);
            return;
        }
        CACHE.put(key, png);
        save(sp.server, key, png);
        CoreNetwork.sendToAll(new PicturePackets.Changed(key));
        MinecraftForge.EVENT_BUS.post(new PictureChangedEvent(key, sp));
    }

    public static void onQuery(ServerPlayer sp, String key) {
        long second = System.currentTimeMillis() / 1000;
        long[] q = QUERIES.computeIfAbsent(sp.getUUID(), k -> new long[2]);
        if (q[0] != second) {
            q[0] = second;
            q[1] = 0;
        }
        if (++q[1] > QUERIES_PER_SECOND || !validKey(key)) return;
        byte[] png = png(sp.server, key);
        CoreNetwork.send(sp, new PicturePackets.Data(key, png == null ? NONE : png));
    }

    /** Размер из заголовка, без распаковки картинки. */
    static boolean sizeIs(byte[] png, int width, int height) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(png))) {
            if (in == null) return false;
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("png");
            if (!readers.hasNext()) return false;
            ImageReader r = readers.next();
            try {
                r.setInput(in, true, true);
                return r.getWidth(0) == width && r.getHeight(0) == height;
            } finally {
                r.dispose();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static void save(MinecraftServer server, String key, byte[] png) {
        Path f = file(server, key);
        if (f == null) return;
        Util.ioPool().execute(() -> {
            try {
                Files.createDirectories(f.getParent());
                Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
                Files.write(tmp, png);
                Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                RpCore.LOGGER.error("RP Core: не сохранить картинку {}: {}", key, e.toString());
            }
        });
    }

    // ------------------------------------------------------------------ события

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        PERMITS.remove(e.getEntity().getUUID());
        QUERIES.remove(e.getEntity().getUUID());
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        PERMITS.clear();
        QUERIES.clear();
        CACHE.clear();
    }
}
