package faygolover.rpcore.network;

import faygolover.rpcore.api.BodyProfile;
import faygolover.rpcore.profile.ProfileServer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Пакеты анкеты тела: сервер присылает игроку его анкету; игрок один раз присылает заполненную. */
public final class ProfilePackets {
    private ProfilePackets() {}

    /** Сервер → клиент: своя анкета. */
    public record Sync(BodyProfile profile) {
        public static void encode(Sync p, FriendlyByteBuf buf) {
            buf.writeEnum(p.profile.sex());
            buf.writeVarInt(p.profile.age());
            buf.writeDouble(p.profile.heightCm());
            buf.writeBoolean(p.profile.filled());
        }

        public static Sync decode(FriendlyByteBuf buf) {
            return new Sync(new BodyProfile(buf.readEnum(BodyProfile.Sex.class), buf.readVarInt(), buf.readDouble(), buf.readBoolean()));
        }

        public static void handle(Sync p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.onSync(p.profile)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Клиент → сервер: заполненная анкета (принимается, только пока анкета не заполнена). */
    public record Submit(BodyProfile.Sex sex, int age, double height) {
        public static void encode(Submit p, FriendlyByteBuf buf) {
            buf.writeEnum(p.sex);
            buf.writeVarInt(p.age);
            buf.writeDouble(p.height);
        }

        public static Submit decode(FriendlyByteBuf buf) {
            return new Submit(buf.readEnum(BodyProfile.Sex.class), buf.readVarInt(), buf.readDouble());
        }

        public static void handle(Submit p, Supplier<NetworkEvent.Context> ctx) {
            var sp = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (sp != null) ProfileServer.onSubmit(sp, p.sex, p.age, p.height);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    private static final class Client {
        static void onSync(BodyProfile profile) {
            // Игрок мог ещё не появиться в мире — берём UUID учётной записи.
            var player = Minecraft.getInstance().player;
            ProfileServer.onClientSync(player != null ? player.getUUID() : Minecraft.getInstance().getUser().getProfileId(), profile);
        }
    }
}
