package faygolover.rpcore.gm;

import faygolover.rpcore.network.CoreNetwork;
import faygolover.rpcore.network.GmPackets;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Окно настроек «ГМских штук» — серверная сторона: открыть ведущему (ПКМ меткой по блоку) и применить присланное.
 * Права проверяются здесь, значения чистит сам блок ({@link GmConfigurable#applyFrom}).
 */
public final class GmSettings {
    private GmSettings() {}

    private static final double MAX_DIST_SQR = 64 * 64;

    public static void open(ServerPlayer sp, BlockPos pos) {
        if (!GmTools.isGm(sp)) {
            sp.displayClientMessage(Component.translatable("rpcore.gm.no_rights"), true);
            return;
        }
        if (sp.level().getBlockEntity(pos) instanceof GmConfigurable c) CoreNetwork.send(sp, new GmPackets.Open(pos, c.writeSettings()));
    }

    public static void apply(ServerPlayer sp, BlockPos pos, CompoundTag tag) {
        if (!GmTools.isGm(sp) || sp.distanceToSqr(pos.getCenter()) > MAX_DIST_SQR || !sp.level().isLoaded(pos)) return;
        if (sp.level().getBlockEntity(pos) instanceof GmConfigurable c) c.applyFrom(sp, tag);
    }
}
