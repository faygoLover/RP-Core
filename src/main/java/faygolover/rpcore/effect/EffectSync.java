package faygolover.rpcore.effect;

import faygolover.rpcore.network.CoreNetwork;
import faygolover.rpcore.network.EffectPackets;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Сервер: кто под «Красными глазами» и «Помехами» — рассылается всем, кто видит сущность (ванилла сообщает эффекты
 * только самому игроку). События эффектов Forge приходят до изменения, поэтому сущность лишь отмечается, а итог
 * считается и рассылается в конце тика. Там же у этих эффектов убираются частицы (зелье даёт их всегда).
 */
public final class EffectSync {
    private EffectSync() {}

    public static final int RED_EYES = 1, INTERFERENCE = 2;

    private static final Set<LivingEntity> DIRTY = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<Entity, Integer> SENT = new WeakHashMap<>();

    private static boolean ours(MobEffect e) {
        return e == RpEffects.RED_EYES.get() || e == RpEffects.INTERFERENCE.get();
    }

    /** Маска эффектов; в битах 8+ — уровень «Помех» (усиление + 1), для их радиуса у клиентов. */
    public static int mask(LivingEntity e) {
        int m = 0;
        if (e.hasEffect(RpEffects.RED_EYES.get())) m |= RED_EYES;
        MobEffectInstance inter = e.getEffect(RpEffects.INTERFERENCE.get());
        if (inter != null) m |= INTERFERENCE | Math.min(15, inter.getAmplifier() + 1) << 8;
        return m;
    }

    public static void onAdded(MobEffectEvent.Added e) {
        if (!e.getEntity().level().isClientSide && ours(e.getEffectInstance().getEffect())) DIRTY.add(e.getEntity());
    }

    public static void onRemove(MobEffectEvent.Remove e) {
        if (!e.getEntity().level().isClientSide && e.getEffect() != null && ours(e.getEffect())) DIRTY.add(e.getEntity());
    }

    public static void onExpired(MobEffectEvent.Expired e) {
        if (!e.getEntity().level().isClientSide && e.getEffectInstance() != null && ours(e.getEffectInstance().getEffect())) DIRTY.add(e.getEntity());
    }

    /** Загрузились с эффектами (вход, смена измерения, сущность из сохранения). */
    public static void onJoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide && e.getEntity() instanceof LivingEntity le && mask(le) != 0) {
            SENT.remove(le);   // клиент после смены измерения забыл — прислать заново
            DIRTY.add(le);
        }
    }

    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof LivingEntity le && e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
            int m = mask(le);
            if (m != 0) CoreNetwork.send(sp, new EffectPackets.State(le.getId(), m));
        }
    }

    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || DIRTY.isEmpty()) return;
        LivingEntity[] list = DIRTY.toArray(new LivingEntity[0]);
        DIRTY.clear();
        for (LivingEntity le : list) {
            if (le.isRemoved()) continue;
            hideParticles(le, RpEffects.RED_EYES.get());
            hideParticles(le, RpEffects.INTERFERENCE.get());
            int m = mask(le);
            Integer before = SENT.get(le);
            if (before != null && before == m) continue;
            SENT.put(le, m);
            CoreNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> le), new EffectPackets.State(le.getId(), m));
        }
    }

    /** Заменить видимый эффект на такой же без частиц (значок остаётся). */
    private static void hideParticles(LivingEntity le, MobEffect effect) {
        MobEffectInstance inst = le.getEffect(effect);
        if (inst == null || !inst.isVisible()) return;
        MobEffectInstance quiet = new MobEffectInstance(effect, inst.getDuration(), inst.getAmplifier(), inst.isAmbient(), false, inst.showIcon());
        le.removeEffect(effect);
        le.addEffect(quiet);
        DIRTY.remove(le);   // removeEffect/addEffect снова отметили — итог и так считается сразу после
    }
}
