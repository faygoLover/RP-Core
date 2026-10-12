package faygolover.rpcore.client.photo;

import com.mojang.blaze3d.platform.NativeImage;
import faygolover.rpcore.RpCore;
import faygolover.rpcore.network.CoreNetwork;
import faygolover.rpcore.network.PhotoPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Фото игроков на клиенте (медкарта, аватарки КПК). Фото — картинка скина игрока. Первый вызов для игрока
 * спрашивает сервер и возвращает null; ответ кэшируется до смены фото или выхода с сервера.
 *
 * @since 0.4.0
 */
public final class ClientPhotos {
    private ClientPhotos() {}

    /** Через сколько тиков переспросить, если сервер не ответил (пакет потерялся, лимит запросов). */
    private static final int RETRY_TICKS = 200;

    private static final class Entry {
        @Nullable ResourceLocation texture;
        @Nullable DynamicTexture dynamic;
        int width, height;
        boolean answered;
        long askedAt;
    }

    private static final Map<UUID, Entry> CACHE = new HashMap<>();

    /** Текстура скина игрока с фото или null (фото нет или ещё не пришло). */
    @Nullable
    public static ResourceLocation skin(UUID player) {
        Entry e = entry(player);
        return e == null ? null : e.texture;
    }

    /**
     * Нарисовать лицо (с верхним слоем) квадратом {@code size}. false — фото нет или ещё не пришло: вызывающий
     * рисует своё (например, лицо по текущему скину через {@code PlayerFaceRenderer}).
     */
    public static boolean drawFace(GuiGraphics g, UUID player, int x, int y, int size) {
        Entry e = entry(player);
        if (e == null || e.texture == null) return false;
        // Скины бывают 64×64, 64×32 и HD: лицо — квадрат 1/8 ширины в (1/8, 1/8), шапка — в (5/8, 1/8).
        int u = e.width / 8;
        g.blit(e.texture, x, y, size, size, u, u, u, u, e.width, e.height);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.blit(e.texture, x, y, size, size, u * 5, u, u, u, e.width, e.height);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        return true;
    }

    @Nullable
    private static Entry entry(UUID player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return null;
        Entry e = CACHE.computeIfAbsent(player, k -> new Entry());
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        if (!e.answered && (e.askedAt == 0 || now - e.askedAt > RETRY_TICKS || now < e.askedAt)) {
            e.askedAt = Math.max(1, now);
            CoreNetwork.sendToServer(new PhotoPackets.Query(player));
        }
        return e;
    }

    /** Ответ сервера. */
    public static void onData(UUID player, byte[] png) {
        Entry e = CACHE.computeIfAbsent(player, k -> new Entry());
        release(e);
        e.answered = true;
        if (png.length == 0) return;
        try {
            NativeImage img = NativeImage.read(png);
            e.width = img.getWidth();
            e.height = img.getHeight();
            e.dynamic = new DynamicTexture(img);
            e.texture = Minecraft.getInstance().getTextureManager().register("rpcore_photo", e.dynamic);
        } catch (Exception ex) {
            RpCore.LOGGER.warn("RP Core: фото {} не читается: {}", player, ex.toString());
        }
    }

    /** Фото сменилось: забыть, при следующем показе спросить заново. */
    public static void forget(UUID player) {
        Entry e = CACHE.remove(player);
        if (e != null) release(e);
    }

    /** Выход с сервера. */
    public static void clear() {
        CACHE.values().forEach(ClientPhotos::release);
        CACHE.clear();
    }

    private static void release(Entry e) {
        if (e.texture != null) Minecraft.getInstance().getTextureManager().release(e.texture);
        e.texture = null;
        e.dynamic = null;
    }
}
