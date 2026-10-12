package faygolover.rpcore.network;

import faygolover.rpcore.photo.PhotoServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Пакеты службы фото: сервер просит снимок, клиент присылает; клиенты спрашивают чужие фото. */
public final class PhotoPackets {
    private PhotoPackets() {}

    /** Сервер → клиент: пришли снимок своего скина. */
    public record Request() {
        public static void encode(Request p, FriendlyByteBuf buf) {}

        public static Request decode(FriendlyByteBuf buf) {
            return new Request();
        }

        public static void handle(Request p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> faygolover.rpcore.client.photo.SkinCapture::request));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Клиент → сервер: PNG своего скина (только в ответ на {@link Request}). */
    public record Upload(byte[] png) {
        public static void encode(Upload p, FriendlyByteBuf buf) {
            buf.writeByteArray(p.png);
        }

        public static Upload decode(FriendlyByteBuf buf) {
            return new Upload(buf.readByteArray(PhotoServer.MAX_BYTES));
        }

        public static void handle(Upload p, Supplier<NetworkEvent.Context> ctx) {
            var sp = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (sp != null) PhotoServer.onUpload(sp, p.png);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Клиент → сервер: пришли фото этого игрока. */
    public record Query(UUID uuid) {
        public static void encode(Query p, FriendlyByteBuf buf) {
            buf.writeUUID(p.uuid);
        }

        public static Query decode(FriendlyByteBuf buf) {
            return new Query(buf.readUUID());
        }

        public static void handle(Query p, Supplier<NetworkEvent.Context> ctx) {
            var sp = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (sp != null) PhotoServer.onQuery(sp, p.uuid);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Сервер → клиент: фото игрока (пустое — фото нет). */
    public record Data(UUID uuid, byte[] png) {
        public static void encode(Data p, FriendlyByteBuf buf) {
            buf.writeUUID(p.uuid);
            buf.writeByteArray(p.png);
        }

        public static Data decode(FriendlyByteBuf buf) {
            return new Data(buf.readUUID(), buf.readByteArray(PhotoServer.MAX_BYTES));
        }

        public static void handle(Data p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> faygolover.rpcore.client.photo.ClientPhotos.onData(p.uuid, p.png)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Сервер → все клиенты: фото игрока сменилось, кэш устарел. */
    public record Changed(UUID uuid) {
        public static void encode(Changed p, FriendlyByteBuf buf) {
            buf.writeUUID(p.uuid);
        }

        public static Changed decode(FriendlyByteBuf buf) {
            return new Changed(buf.readUUID());
        }

        public static void handle(Changed p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> faygolover.rpcore.client.photo.ClientPhotos.forget(p.uuid)));
            ctx.get().setPacketHandled(true);
        }
    }
}
