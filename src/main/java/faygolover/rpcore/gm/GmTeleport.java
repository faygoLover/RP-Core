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
    /**
     * Насколько высоко можно поднять точку прибытия: ковёр, плита, ступень — и целый блок пола с ковром сверху
     * (координаты, взятые с блока пола, а не с ног, давали перенос на блок ниже).
     */
    private static final double MAX_LIFT = 1.5;

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
        if (!level.getBlockCollisions(e, box).iterator().hasNext()) return pos;
        // Опора — только то, что у ног (до MAX_LIFT): перемычка двери над головой не в счёт.
        AABB feet = new AABB(box.minX, pos.y, box.minZ, box.maxX, Math.min(box.maxY, pos.y + MAX_LIFT), box.maxZ);
        java.util.TreeSet<Double> tops = new java.util.TreeSet<>();
        for (VoxelShape s : level.getBlockCollisions(e, feet)) {
            double top = s.max(Direction.Axis.Y);
            if (top > pos.y && top - pos.y <= MAX_LIFT) tops.add(top);
        }
        for (double top : tops)
            if (!level.getBlockCollisions(e, box.move(0, top - pos.y, 0)).iterator().hasNext()) return new Vec3(pos.x, top, pos.z);
        return pos;
    }

    /**
     * @param keepView не менять взгляд (только место)
     * @return перенесли ли
     */
    public static boolean teleport(Entity e, ServerLevel to, Vec3 pos, float yaw, float pitch, boolean keepView, int cooldown) {
        float y = keepView ? e.getYRot() : yaw, p = keepView ? e.getXRot() : pitch;
        Vec3 motion = e.getDeltaMovement();
        if (e instanceof ServerPlayer sp) {
            // С кресла, лошади и т. п. — сначала слезть (место считаем уже для стоящего).
            sp.stopRiding();
            Entity root = sp.getRootVehicle();
            if (root != sp && sp.level() == to) {
                // Мод не отпустил (лежачего с кресла и каталки снимают только волоком) — переносим вместе с транспортом.
                Vec3 at = safeSpot(root, to, pos);
                root.teleportTo(to, at.x, at.y, at.z, java.util.Set.of(), keepView ? root.getYRot() : yaw, root.getXRot());
                root.resetFallDistance();
                setCooldown(root, cooldown);
                for (Entity r : root.getIndirectPassengers()) {
                    setCooldown(r, cooldown);
                    r.resetFallDistance();
                    if (r instanceof ServerPlayer rp) rp.connection.resetPosition();
                }
                return true;
            }
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
