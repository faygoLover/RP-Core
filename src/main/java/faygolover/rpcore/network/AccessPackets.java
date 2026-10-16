package faygolover.rpcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Пакеты замка семейства: сервер сообщает, выдан ли игроку доступ ведущего. */
public final class AccessPackets {
    private AccessPackets() {}

    /** Сервер → клиент: доступ ведущего выдан или снят. */
    public record Grant(boolean grant) {
        public static void encode(Grant p, FriendlyByteBuf buf) {
            buf.writeBoolean(p.grant);
        }

        public static Grant decode(FriendlyByteBuf buf) {
            return new Grant(buf.readBoolean());
        }

        public static void handle(Grant p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> faygolover.rpcore.client.access.ClientAccess.onServerGrant(p.grant)));
            ctx.get().setPacketHandled(true);
        }
    }
}
