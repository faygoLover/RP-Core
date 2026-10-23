package faygolover.rpcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Эффекты RP Core на сущности (маска {@code EffectSync}) — всем, кто её видит. */
public final class EffectPackets {
    private EffectPackets() {}

    public record State(int entityId, int mask) {
        public static void encode(State m, FriendlyByteBuf b) {
            b.writeVarInt(m.entityId);
            b.writeVarInt(m.mask);
        }

        public static State decode(FriendlyByteBuf b) {
            return new State(b.readVarInt(), b.readVarInt());
        }

        public static void handle(State m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> faygolover.rpcore.client.effect.ClientEffects.set(m.entityId, m.mask)));
            ctx.get().setPacketHandled(true);
        }
    }
}
