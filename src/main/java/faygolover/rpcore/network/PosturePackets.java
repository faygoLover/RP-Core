package faygolover.rpcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Сон сидя или лёжа: самому игроку — затемнить экран или снять затемнение (0.11.0). */
public final class PosturePackets {
    private PosturePackets() {}

    /** Поза игрока для хитбокса у клиентов: id сущности и номер позы. */
    public record Pose(int entityId, int posture) {
        public static void encode(Pose m, FriendlyByteBuf buf) {
            buf.writeVarInt(m.entityId);
            buf.writeVarInt(m.posture);
        }

        public static Pose decode(FriendlyByteBuf buf) {
            return new Pose(buf.readVarInt(), buf.readVarInt());
        }

        public static void handle(Pose m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                var values = faygolover.rpcore.api.RpCoreAPI.Posture.values();
                faygolover.rpcore.pose.PostureView.set(m.entityId, values[Math.max(0, Math.min(values.length - 1, m.posture))]);
                var level = net.minecraft.client.Minecraft.getInstance().level;
                var e = level == null ? null : level.getEntity(m.entityId);
                if (e != null) e.refreshDimensions();
            }));
            ctx.get().setPacketHandled(true);
        }
    }

    public record Sleep(boolean asleep) {
        public static void encode(Sleep m, FriendlyByteBuf buf) {
            buf.writeBoolean(m.asleep);
        }

        public static Sleep decode(FriendlyByteBuf buf) {
            return new Sleep(buf.readBoolean());
        }

        public static void handle(Sleep m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> faygolover.rpcore.client.PostureOverlay.setAsleep(m.asleep)));
            ctx.get().setPacketHandled(true);
        }
    }
}
