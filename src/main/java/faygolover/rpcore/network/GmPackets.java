package faygolover.rpcore.network;

import faygolover.rpcore.gm.GmSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Окно настроек «ГМской штуки»: сервер присылает текущие настройки, клиент возвращает изменённые. */
public final class GmPackets {
    private GmPackets() {}

    /** Сервер → ведущему: открыть окно настроек блока в {@code pos}. */
    public record Open(BlockPos pos, CompoundTag settings) {
        public static void encode(Open m, FriendlyByteBuf buf) {
            buf.writeBlockPos(m.pos);
            buf.writeNbt(m.settings);
        }

        public static Open decode(FriendlyByteBuf buf) {
            return new Open(buf.readBlockPos(), buf.readNbt());
        }

        public static void handle(Open m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> faygolover.rpcore.client.gm.GmSettingsScreen.open(m.pos, m.settings)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Ведущий → сервер: применить настройки. */
    public record Apply(BlockPos pos, CompoundTag settings) {
        public static void encode(Apply m, FriendlyByteBuf buf) {
            buf.writeBlockPos(m.pos);
            buf.writeNbt(m.settings);
        }

        public static Apply decode(FriendlyByteBuf buf) {
            return new Apply(buf.readBlockPos(), buf.readNbt());
        }

        public static void handle(Apply m, Supplier<NetworkEvent.Context> ctx) {
            var sender = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (sender != null && m.settings != null) GmSettings.apply(sender, m.pos, m.settings);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
