package faygolover.rpcore.network;

import faygolover.rpcore.photo.PhotoServer;
import faygolover.rpcore.photo.PictureServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Пакеты картинок по ключу: клиент присылает (по разрешению сервера) и спрашивает; сервер раздаёт. */
public final class PicturePackets {
    private PicturePackets() {}

    /** Клиент → сервер: PNG картинки {@code key} (принимается, только если сервер разрешил). */
    public record Upload(String key, byte[] png) {
        public static void encode(Upload p, FriendlyByteBuf buf) {
            buf.writeUtf(p.key, PictureServer.MAX_KEY);
            buf.writeByteArray(p.png);
        }

        public static Upload decode(FriendlyByteBuf buf) {
            return new Upload(buf.readUtf(PictureServer.MAX_KEY), buf.readByteArray(PhotoServer.MAX_BYTES));
        }

        public static void handle(Upload p, Supplier<NetworkEvent.Context> ctx) {
            var sp = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (sp != null) PictureServer.onUpload(sp, p.key, p.png);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Клиент → сервер: пришли картинку {@code key}. */
    public record Query(String key) {
        public static void encode(Query p, FriendlyByteBuf buf) {
            buf.writeUtf(p.key, PictureServer.MAX_KEY);
        }

        public static Query decode(FriendlyByteBuf buf) {
            return new Query(buf.readUtf(PictureServer.MAX_KEY));
        }

        public static void handle(Query p, Supplier<NetworkEvent.Context> ctx) {
            var sp = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (sp != null) PictureServer.onQuery(sp, p.key);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Сервер → клиент: картинка (пустая — её нет). */
    public record Data(String key, byte[] png) {
        public static void encode(Data p, FriendlyByteBuf buf) {
            buf.writeUtf(p.key, PictureServer.MAX_KEY);
            buf.writeByteArray(p.png);
        }

        public static Data decode(FriendlyByteBuf buf) {
            return new Data(buf.readUtf(PictureServer.MAX_KEY), buf.readByteArray(PhotoServer.MAX_BYTES));
        }

        public static void handle(Data p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> faygolover.rpcore.client.photo.ClientPictures.onData(p.key, p.png)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Сервер → все клиенты: картинка сменилась или удалена, кэш устарел. */
    public record Changed(String key) {
        public static void encode(Changed p, FriendlyByteBuf buf) {
            buf.writeUtf(p.key, PictureServer.MAX_KEY);
        }

        public static Changed decode(FriendlyByteBuf buf) {
            return new Changed(buf.readUtf(PictureServer.MAX_KEY));
        }

        public static void handle(Changed p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> faygolover.rpcore.client.photo.ClientPictures.forget(p.key)));
            ctx.get().setPacketHandled(true);
        }
    }
}
