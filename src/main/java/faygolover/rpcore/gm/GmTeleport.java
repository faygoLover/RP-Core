package faygolover.rpcore.gm;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

/**
 * Перенос сущности для пластин и порталов. Игрок — в любое измерение, остальные сущности — только в пределах своего.
 * После переноса сущность несколько тиков не замечают другие пластины и порталы (иначе на точке прибытия с
 * пластиной её тут же отправило бы обратно). Игрок после переноса секунду неуязвим, а точка прибытия поднимается
 * на верх ковра, плиты и т. п., если попала в них.
 */
public final class GmTeleport {
    private GmTeleport() {}

    private static final String KEY = "rpcore_tp_until";
    private static final String SAFE_KEY = "rpcore_tp_safe_until";
    /** Неуязвимость игрока после переноса, тиков. */
    public static final int ARRIVAL_SAFE_TICKS = 20;
    /** Насколько высоко можно поднять точку прибытия (ковёр, плита, снег, ступень). */
    private static final double MAX_LIFT = 1.0;

    /** Сущность недавно перенесли — пластины и порталы её пока не трогают. */
    public static boolean onCooldown(Entity e) {
        return e.getPersistentData().getLong(KEY) > e.level().getGameTime();
    }

    /** Отметить перенос: пластины и порталы не трогают сущность {@code ticks} тиков, игрок секунду неуязвим. */
    public static void setCooldown(Entity e, int ticks) {
        long now = e.level().getGameTime();
        e.getPersistentData().putLong(KEY, now + Math.max(0, ticks));
        if (e instanceof Player) e.getPersistentData().putLong(SAFE_KEY, now + ARRIVAL_SAFE_TICKS);
    }

    /** Урон по игроку сразу после переноса отменяется (кроме того, что пробивает неуязвимость: /kill, пустота). */
    public static void onAttack(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (sp.getPersistentData().getLong(SAFE_KEY) > sp.level().getGameTime()) e.setCanceled(true);
    }

    /**
     * Точка прибытия без застревания: если тело сущности в {@code pos} задевает блоки не выше {@link #MAX_LIFT}
     * (ковёр, плита, снег), точка поднимается на их верх — при условии, что там свободно. Иначе {@code pos} как есть.
     */
    public static Vec3 safeSpot(Entity e, Level level, Vec3 pos) {
        AABB box = e.getDimensions(e.getPose()).makeBoundingBox(pos).deflate(1.0E-7);
        double top = Double.NEGATIVE_INFINITY;
        for (VoxelShape s : level.getBlockCollisions(e, box)) top = Math.max(top, s.max(Direction.Axis.Y));
        if (top == Double.NEGATIVE_INFINITY || top <= pos.y || top - pos.y > MAX_LIFT) return pos;
        Vec3 up = new Vec3(pos.x, top, pos.z);
        AABB moved = box.move(0, top - pos.y, 0);
        return level.getBlockCollisions(e, moved).iterator().hasNext() ? pos : up;
    }

    /**
     * @param keepView не менять взгляд (только место)
     * @return перенесли ли
     */
    public static boolean teleport(Entity e, ServerLevel to, Vec3 pos, float yaw, float pitch, boolean keepView, int cooldown) {
        float y = keepView ? e.getYRot() : yaw, p = keepView ? e.getXRot() : pitch;
        Vec3 motion = e.getDeltaMovement();
        if (e instanceof ServerPlayer sp) {
            pos = safeSpot(sp, to, pos);
            boolean sameLevel = sp.level() == to;
            sp.teleportTo(to, pos.x, pos.y, pos.z, y, p);
            // G2 (02.11): проверка скорости считает сдвиг от firstGood*, а их ставит только resetPosition() в начале
            // тика — без этого шаг клиента сразу после переноса давал «moved too quickly» и откат. Другое измерение
            // сервер и так не проверяет.
            if (sameLevel) sp.connection.resetPosition();
        } else {
            if (e.level() != to) return false;
            pos = safeSpot(e, to, pos);
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
