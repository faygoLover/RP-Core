package faygolover.rpcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Сон сидя или лёжа: самому игроку — затемнить экран или снять затемнение (0.11.0). */
public final class PosturePackets {
    private PosturePackets() {}

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
