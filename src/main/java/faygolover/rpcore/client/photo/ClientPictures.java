package faygolover.rpcore.client.photo;

import com.mojang.blaze3d.platform.NativeImage;
import faygolover.rpcore.RpCore;
import faygolover.rpcore.network.CoreNetwork;
import faygolover.rpcore.network.PicturePackets;
import faygolover.rpcore.photo.PhotoServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Картинки по ключу на клиенте ({@link faygolover.rpcore.api.Pictures}). Первый показ спрашивает сервер и ничего не
 * рисует; ответ кэшируется до смены картинки или выхода с сервера.
 *
 * @since 0.4.1
 */
public final class ClientPictures {
    private ClientPictures() {}

    /** Через сколько тиков переспросить, если сервер не ответил (пакет потерялся, лимит запросов). */
    private static final int RETRY_TICKS = 200;

    private static final class Entry {
        @Nullable ResourceLocation texture;
        int width, height;
        boolean answered;
        long askedAt;
    }

    private static final Map<String, Entry> CACHE = new HashMap<>();

    /** Нарисовать картинку целиком в прямоугольник; false — картинки нет или ещё не пришла. */
    public static boolean draw(GuiGraphics g, String key, int x, int y, int w, int h) {
        Entry e = entry(key);
        if (e == null || e.texture == null) return false;
        g.blit(e.texture, x, y, w, h, 0, 0, e.width, e.height, e.width, e.height);
        return true;
    }

    /** Отправить свою картинку (сервер примет, только если его мод разрешил этот ключ). PNG до 30 КБ. */
    public static boolean upload(String key, byte[] png) {
        if (png.length == 0 || png.length > PhotoServer.MAX_BYTES || Minecraft.getInstance().getConnection() == null) return false;
        CoreNetwork.sendToServer(new PicturePackets.Upload(key, png));
        return true;
    }

    @Nullable
    private static Entry entry(String key) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return null;
        Entry e = CACHE.computeIfAbsent(key, k -> new Entry());
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        if (!e.answered && (e.askedAt == 0 || now - e.askedAt > RETRY_TICKS || now < e.askedAt)) {
            e.askedAt = Math.max(1, now);
            CoreNetwork.sendToServer(new PicturePackets.Query(key));
        }
        return e;
    }

    /** Ответ сервера. */
    public static void onData(String key, byte[] png) {
        Entry e = CACHE.computeIfAbsent(key, k -> new Entry());
        release(e);
        e.answered = true;
        if (png.length == 0) return;
        try {
            NativeImage img = NativeImage.read(png);
            e.width = img.getWidth();
            e.height = img.getHeight();
            e.texture = Minecraft.getInstance().getTextureManager().register("rpcore_picture", new DynamicTexture(img));
        } catch (Exception ex) {
            RpCore.LOGGER.warn("RP Core: картинка {} не читается: {}", key, ex.toString());
        }
    }

    /** Картинка сменилась: забыть, при следующем показе спросить заново. */
    public static void forget(String key) {
        Entry e = CACHE.remove(key);
        if (e != null) release(e);
    }

    /** Выход с сервера. */
    public static void clear() {
        CACHE.values().forEach(ClientPictures::release);
        CACHE.clear();
    }

    private static void release(Entry e) {
        if (e.texture != null) Minecraft.getInstance().getTextureManager().release(e.texture);
        e.texture = null;
    }
}
