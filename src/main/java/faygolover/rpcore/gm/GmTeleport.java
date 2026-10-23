package faygolover.rpcore.gm;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Перенос сущности для пластин и порталов. Игрок — в любое измерение, остальные сущности — только в пределах своего.
 * После переноса сущность несколько тиков не замечают другие пластины и порталы (иначе на точке прибытия с
 * пластиной её тут же отправило бы обратно).
 */
public final class GmTeleport {
    private GmTeleport() {}

    private static final String KEY = "rpcore_tp_until";

    /** Сущность недавно перенесли — пластины и порталы её пока не трогают. */
    public static boolean onCooldown(Entity e) {
        return e.getPersistentData().getLong(KEY) > e.level().getGameTime();
    }

    public static void setCooldown(Entity e, int ticks) {
        e.getPersistentData().putLong(KEY, e.level().getGameTime() + Math.max(0, ticks));
    }

    /**
     * @param keepView не менять взгляд (только место)
     * @return перенесли ли
     */
    public static boolean teleport(Entity e, ServerLevel to, Vec3 pos, float yaw, float pitch, boolean keepView, int cooldown) {
        float y = keepView ? e.getYRot() : yaw, p = keepView ? e.getXRot() : pitch;
        Vec3 motion = e.getDeltaMovement();
        if (e instanceof ServerPlayer sp) {
            sp.teleportTo(to, pos.x, pos.y, pos.z, y, p);
        } else {
            if (e.level() != to) return false;
            e.teleportTo(pos.x, pos.y, pos.z);
            e.setYRot(y);
            e.setXRot(p);
        }
        e.setDeltaMovement(motion);
        e.resetFallDistance();
        setCooldown(e, cooldown);
        return true;
    }
}
