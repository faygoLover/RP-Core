package faygolover.rpcore.api.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;

/**
 * Будильник (или другой громкий сигнал) в точке: моды сна будят спящих в радиусе. Сервер,
 * {@code MinecraftForge.EVENT_BUS}; слать — {@link faygolover.rpcore.api.RpCoreAPI#alarm}. Будильник КПК (RP PDA) — позже.
 *
 * @since 0.11.0
 */
public class RpAlarmEvent extends Event {
    private final ServerLevel level;
    private final Vec3 pos;
    private final double radius;

    public RpAlarmEvent(ServerLevel level, Vec3 pos, double radius) {
        this.level = level;
        this.pos = pos;
        this.radius = radius;
    }

    public ServerLevel level() {
        return level;
    }

    public Vec3 pos() {
        return pos;
    }

    public double radius() {
        return radius;
    }
}
