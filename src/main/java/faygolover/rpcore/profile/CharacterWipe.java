package faygolover.rpcore.profile;

import faygolover.rpcore.RpCore;
import faygolover.rpcore.api.BodyProfile;
import faygolover.rpcore.api.FoodTaste;
import faygolover.rpcore.api.event.RpCharacterWipeEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Вайп персонажа по слову ГМа: анкета, вкусы и данные всех модов (через {@link RpCharacterWipeEvent}). Игрока нет в
 * сети — ставим метку, сработает при входе. Вещи и КПК остаются.
 */
public final class CharacterWipe {
    private CharacterWipe() {}

    private static final String FILE = "rpcore_character_wipes";

    /** Обнулить сейчас или при следующем входе. true — уже сделано, false — отложено до входа. */
    public static boolean wipe(MinecraftServer server, UUID id, String name, String by) {
        ServerPlayer sp = server.getPlayerList().getPlayer(id);
        if (sp != null) {
            apply(sp, by);
            return true;
        }
        Pending p = pending(server);
        p.marks.put(id, by);
        p.setDirty();
        RpCore.LOGGER.info("[RP Core] {} поставил вайп персонажа {} ({}): сработает при входе", by, name, id);
        return false;
    }

    private static void apply(ServerPlayer sp, String by) {
        ProfileServer.set(sp, BodyProfile.DEFAULT);
        ProfileServer.setTaste(sp, FoodTaste.NONE);
        MinecraftForge.EVENT_BUS.post(new RpCharacterWipeEvent(sp));
        ProfileServer.sync(sp);
        sp.sendSystemMessage(Component.translatable("rpcore.msg.character_wiped"));
        RpCore.LOGGER.info("[RP Core] {} обнулил персонажа {} ({})", by, sp.getScoreboardName(), sp.getUUID());
    }

    /** Вход: метка есть — обнуляем. Слушатель последний: моды к этому моменту подняли своё. */
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        Pending p = pending(sp.server);
        String by = p.marks.remove(sp.getUUID());
        if (by == null) return;
        p.setDirty();
        apply(sp, by);
    }

    private static Pending pending(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Pending::load, Pending::new, FILE);
    }

    private static final class Pending extends SavedData {
        final Map<UUID, String> marks = new HashMap<>();

        static Pending load(CompoundTag tag) {
            Pending p = new Pending();
            for (String key : tag.getAllKeys()) p.marks.put(UUID.fromString(key), tag.getString(key));
            return p;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            marks.forEach((id, by) -> tag.putString(id.toString(), by));
            return tag;
        }
    }
}
