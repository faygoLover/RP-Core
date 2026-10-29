package faygolover.rpcore.network;

import faygolover.rpcore.RpCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/** Свой сетевой канал RP Core ({@code rpcore:main}). По нему ходят фото игроков, картинки по ключу, анкета тела, доступ ведущего и настройки «ГМских штук». */
public final class CoreNetwork {
    private CoreNetwork() {}

    private static final String VERSION = "8";
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
        // Картинки по ключу (0.4.1).
        CHANNEL.registerMessage(id++, PicturePackets.Upload.class, PicturePackets.Upload::encode, PicturePackets.Upload::decode,
                PicturePackets.Upload::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, PicturePackets.Query.class, PicturePackets.Query::encode, PicturePackets.Query::decode,
                PicturePackets.Query::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, PicturePackets.Data.class, PicturePackets.Data::encode, PicturePackets.Data::decode,
                PicturePackets.Data::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, PicturePackets.Changed.class, PicturePackets.Changed::encode, PicturePackets.Changed::decode,
                PicturePackets.Changed::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // Анкета тела (0.5.0).
        CHANNEL.registerMessage(id++, ProfilePackets.Sync.class, ProfilePackets.Sync::encode, ProfilePackets.Sync::decode,
                ProfilePackets.Sync::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, ProfilePackets.Submit.class, ProfilePackets.Submit::encode, ProfilePackets.Submit::decode,
                ProfilePackets.Submit::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, ProfilePackets.TasteSync.class, ProfilePackets.TasteSync::encode, ProfilePackets.TasteSync::decode,
                ProfilePackets.TasteSync::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, ProfilePackets.TasteSubmit.class, ProfilePackets.TasteSubmit::encode, ProfilePackets.TasteSubmit::decode,
                ProfilePackets.TasteSubmit::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        // Замок семейства (0.6.0).
        CHANNEL.registerMessage(id++, AccessPackets.Grant.class, AccessPackets.Grant::encode, AccessPackets.Grant::decode,
                AccessPackets.Grant::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // Окно настроек «ГМских штук» (0.8.0).
        CHANNEL.registerMessage(id++, GmPackets.Open.class, GmPackets.Open::encode, GmPackets.Open::decode,
                GmPackets.Open::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, GmPackets.Apply.class, GmPackets.Apply::encode, GmPackets.Apply::decode,
                GmPackets.Apply::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        // Эффекты на сущностях — видящим (0.8.1).
        CHANNEL.registerMessage(id++, EffectPackets.State.class, EffectPackets.State::encode, EffectPackets.State::decode,
                EffectPackets.State::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // Позы и сон сидя или лёжа (0.11.0): затемнение экрана, хитбокс у клиентов.
        CHANNEL.registerMessage(id++, PosturePackets.Sleep.class, PosturePackets.Sleep::encode, PosturePackets.Sleep::decode,
                PosturePackets.Sleep::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, PosturePackets.Pose.class, PosturePackets.Pose::encode, PosturePackets.Pose::decode,
                PosturePackets.Pose::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
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
