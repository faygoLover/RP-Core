package faygolover.rpcore.network;

import faygolover.rpcore.RpCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/** Свой сетевой канал RP Core ({@code rpcore:main}). Пока по нему ходят только фото игроков. */
public final class CoreNetwork {
    private CoreNetwork() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RpCore.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, PhotoPackets.Request.class, PhotoPackets.Request::encode, PhotoPackets.Request::decode,
                PhotoPackets.Request::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, PhotoPackets.Upload.class, PhotoPackets.Upload::encode, PhotoPackets.Upload::decode,
                PhotoPackets.Upload::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, PhotoPackets.Query.class, PhotoPackets.Query::encode, PhotoPackets.Query::decode,
                PhotoPackets.Query::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, PhotoPackets.Data.class, PhotoPackets.Data::encode, PhotoPackets.Data::decode,
                PhotoPackets.Data::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, PhotoPackets.Changed.class, PhotoPackets.Changed::encode, PhotoPackets.Changed::decode,
                PhotoPackets.Changed::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void send(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToAll(Object msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }
}
