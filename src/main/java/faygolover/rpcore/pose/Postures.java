package faygolover.rpcore.pose;

import com.mojang.brigadier.CommandDispatcher;
import faygolover.rpcore.CoreConfig;
import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rpcore.api.RpCoreAPI.Posture;
import faygolover.rpcore.api.event.RpPostureSleepEvent;
import faygolover.rpcore.api.event.RpPostureWakeEvent;
import faygolover.rpcore.api.event.RpSleepCountEvent;
import faygolover.rpcore.network.CoreNetwork;
import faygolover.rpcore.network.PosturePackets;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Позы и сон сидя/лёжа (сервер): {@code /sit} — сесть на землю, {@code /lay} — лечь на спину, {@code /sleep} — уснуть,
 * когда сидишь (и на сиденье мода) или лежишь. Позы — эмоции Emotecraft, запущенные сервером «принудительно»: их видят
 * все, сам игрок их не сбросит. Пока сидит или лежит, ходить не может (скорость ×0); встаёт по Shift, прыжку, сдвигу
 * с места, в воде, в седле, при смерти и выходе. Спящего будит удар, Shift и повторный {@code /sleep}. Сколько и как
 * хорошо он спал — забота RP Needs ({@link RpCoreAPI#postureSleeping}), «отлежал» — RP Medicine
 * ({@link RpPostureWakeEvent}). Спящий сидя или лёжа спит и для ночи RP Time.
 */
public final class Postures {
    private Postures() {}

    private record State(Posture kind, Vec3 pos, boolean sleeping, long sleepStartMs) {
        State asleep(boolean value) {
            return new State(kind, pos, value, value ? System.currentTimeMillis() : 0);
        }
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final UUID STILL = UUID.fromString("7d0c4a52-6f1e-4b2b-9a57-3c1f0e8b2a61");
    private static Boolean emotecraft;

    static boolean emotecraft() {
        if (emotecraft == null) emotecraft = ModList.get() != null && ModList.get().isLoaded("emotecraft");
        return emotecraft;
    }

    public static Posture posture(Player p) {
        State s = STATES.get(p.getUUID());
        if (s != null && s.kind != Posture.SEAT) return s.kind;
        return onSeat(p) ? Posture.SEAT : Posture.NONE;
    }

    public static boolean sleeping(Player p) {
        State s = STATES.get(p.getUUID());
        return s != null && s.sleeping;
    }

    /** Едет на сиденье мода: не на мобе, и транспорт сажает седока. */
    private static boolean onSeat(Player p) {
        Entity v = p.getVehicle();
        return v != null && !(v instanceof LivingEntity) && v.shouldRiderSit();
    }

    // ------------------------------------------------------------------ команды

    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("sit").requires(Postures::allowed).executes(c -> toggle(c.getSource().getPlayerOrException(), Posture.SIT)));
        d.register(Commands.literal("lay").requires(Postures::allowed).executes(c -> toggle(c.getSource().getPlayerOrException(), Posture.LAY)));
        d.register(Commands.literal("sleep").requires(Postures::allowed).executes(c -> sleep(c.getSource().getPlayerOrException())));
    }

    private static boolean allowed(CommandSourceStack src) {
        return src.getEntity() instanceof ServerPlayer && CoreConfig.SPEC.isLoaded() && CoreConfig.POSTURES_ENABLED.get();
    }

    private static int toggle(ServerPlayer sp, Posture kind) {
        State s = STATES.get(sp.getUUID());
        if (s != null && s.kind == kind) {
            stand(sp);
            return 1;
        }
        if (sp.isPassenger() || sp.isSleeping()) return fail(sp, "rpcore.posture.busy");
        if (!sp.onGround() || sp.isInWater() || sp.isSpectator() || sp.isFallFlying()) return fail(sp, "rpcore.posture.ground");
        // Из одной позы в другую — на том же месте, сон не прерывается.
        Vec3 pos = s != null && s.kind != Posture.SEAT ? s.pos : sp.position();
        STATES.put(sp.getUUID(), new State(kind, pos, s != null && s.sleeping, s != null ? s.sleepStartMs : 0));
        still(sp, true);
        if (emotecraft()) EmoteImpl.play(sp.getUUID(), kind);
        sp.displayClientMessage(Component.translatable(kind == Posture.SIT ? "rpcore.posture.sat" : "rpcore.posture.lay"), true);
        return 1;
    }

    private static int sleep(ServerPlayer sp) {
        State s = STATES.get(sp.getUUID());
        if (s != null && s.sleeping) {
            wake(sp, s);
            return 1;
        }
        Posture kind = s != null ? s.kind : onSeat(sp) ? Posture.SEAT : Posture.NONE;
        if (kind == Posture.NONE) return fail(sp, "rpcore.posture.sleep_where");
        RpPostureSleepEvent ev = new RpPostureSleepEvent(sp, kind);
        if (MinecraftForge.EVENT_BUS.post(ev)) {
            sp.displayClientMessage(ev.reason() != null ? ev.reason() : Component.translatable("rpcore.posture.cant_sleep"), true);
            return 0;
        }
        State base = s != null ? s : new State(Posture.SEAT, sp.position(), false, 0);
        STATES.put(sp.getUUID(), base.asleep(true));
        CoreNetwork.send(sp, new PosturePackets.Sleep(true));
        sp.displayClientMessage(Component.translatable("rpcore.posture.asleep"), true);
        return 1;
    }

    private static int fail(ServerPlayer sp, String key) {
        sp.displayClientMessage(Component.translatable(key), true);
        return 0;
    }

    // ------------------------------------------------------------------ встать, проснуться

    /** Разбудить спящего сидя или лёжа (выспался): поза остаётся. */
    public static void wakeUp(ServerPlayer sp) {
        State s = STATES.get(sp.getUUID());
        if (s != null && s.sleeping) wake(sp, s);
    }

    /** Свалиться и уснуть где стоит (недосып): лечь на спину и уснуть, без проверок «можно ли уснуть». */
    public static boolean collapse(ServerPlayer sp) {
        if (sleeping(sp) || sp.isSleeping() || sp.isPassenger() || !sp.onGround() || sp.isInWater()) return false;
        State s = STATES.get(sp.getUUID());
        if (s == null || s.kind != Posture.LAY) toggle(sp, Posture.LAY);
        s = STATES.get(sp.getUUID());
        if (s == null) return false;
        STATES.put(sp.getUUID(), s.asleep(true));
        CoreNetwork.send(sp, new PosturePackets.Sleep(true));
        return true;
    }


    /** Встать (и проснуться). */
    public static void stand(ServerPlayer sp) {
        State s = STATES.remove(sp.getUUID());
        if (s == null) return;
        if (s.sleeping) wakeEvent(sp, s);
        still(sp, false);
        if (s.kind != Posture.SEAT && emotecraft()) EmoteImpl.stop(sp.getUUID());
    }

    private static void wake(ServerPlayer sp, State s) {
        wakeEvent(sp, s);
        if (s.kind == Posture.SEAT) STATES.remove(sp.getUUID());
        else STATES.put(sp.getUUID(), s.asleep(false));
    }

    private static void wakeEvent(ServerPlayer sp, State s) {
        long secs = Math.max(0, (System.currentTimeMillis() - s.sleepStartMs) / 1000);
        CoreNetwork.send(sp, new PosturePackets.Sleep(false));
        MinecraftForge.EVENT_BUS.post(new RpPostureWakeEvent(sp, s.kind, secs));
    }

    /** Сидящий и лежащий не ходит: скорость ×0 (временный модификатор, в сохранение не попадает). */
    private static void still(ServerPlayer sp, boolean on) {
        AttributeInstance a = sp.getAttribute(Attributes.MOVEMENT_SPEED);
        if (a == null) return;
        a.removeModifier(STILL);
        if (on) a.addTransientModifier(new AttributeModifier(STILL, "rpcore posture", -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    // ------------------------------------------------------------------ события

    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || STATES.isEmpty()) return;
        MinecraftServer server = e.getServer();
        double far = CoreConfig.SPEC.isLoaded() ? CoreConfig.POSTURE_STAND_DISTANCE.get() : 0.6;
        for (Map.Entry<UUID, State> en : new ArrayList<>(STATES.entrySet())) {
            ServerPlayer sp = server.getPlayerList().getPlayer(en.getKey());
            State s = en.getValue();
            if (sp == null) {
                STATES.remove(en.getKey());
                continue;
            }
            if (s.kind == Posture.SEAT) {
                if (!onSeat(sp)) stand(sp);
                continue;
            }
            double dx = sp.getX() - s.pos.x, dz = sp.getZ() - s.pos.z;
            boolean moved = dx * dx + dz * dz > far * far || sp.getY() > s.pos.y + 0.4;
            if (sp.isShiftKeyDown() || moved || sp.isPassenger() || sp.isInWater() || !sp.isAlive() || sp.isSleeping()) stand(sp);
        }
    }

    public static void onHurt(LivingHurtEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && e.getAmount() > 0) {
            State s = STATES.get(sp.getUUID());
            if (s != null && s.sleeping) wake(sp, s);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) stand(sp);
    }

    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) stand(sp);
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) stand(sp);
    }

    /** Спящий сидя или лёжа спит и для ускорения ночи RP Time. */
    public static void onSleepCount(RpSleepCountEvent e) {
        if (sleeping(e.getEntity())) e.setCount(RpSleepCountEvent.Count.SLEEPING);
    }
}
