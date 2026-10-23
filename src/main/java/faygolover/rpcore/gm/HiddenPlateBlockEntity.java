package faygolover.rpcore.gm;

import faygolover.rpcore.RpCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Невидимая пластина. Каждый тик сама смотрит, кто стоит в её области (прямоугольник {@link #width}×{@link #length}
 * вокруг блока, повёрнутый по стрелке {@link #yaw}, высотой полтора блока), — так она не зависит от того, как часто
 * игрок шлёт свои координаты, и отпускает в тот же тик, как он сошёл. Каждая сущность срабатывает сама по себе, когда
 * встаёт на пластину: несколько игроков с разных концов в один тик переносятся все. Стрелка — ещё и взгляд тех, кто
 * прибывает к этой пластине по её имени.
 */
public class HiddenPlateBlockEntity extends BlockEntity implements GmConfigurable {
    // ---------------------------------------------------------------- настройки
    public String name = "";
    /** Стрелка: куда смотрят прибывшие к этой пластине по имени; вдоль неё — длина области, поперёк — ширина. */
    public float yaw;
    public float width = 1f, length = 1f;
    /** Кого замечает: players — игроков, non_gm — игроков без прав ведущего, living — всех живых. */
    public String trigger = "players";
    /** Что делает: none, teleport (координаты), anchor (именованная точка), command. */
    public String action = "none";
    public double tx, ty, tz;
    public float tyaw, tpitch;
    /** Измерение цели, пусто — это же. */
    public String tdim = "";
    /** Взгляд после переноса: target — как у цели (стрелка пластины, точки или поля «взгляд»), keep — не менять. */
    public String view = "target";
    /** Сохранить сдвиг от центра пластины (кто стоял левее — прибудет левее). */
    public boolean keepOffset = false;
    public String anchor = "";
    public String command = "";
    public int cmdLevel = 2;
    /** Тиков между срабатываниями для одной сущности (и «не замечать после переноса»). */
    public int cooldown = 20;
    /** Красный камень: off, hold (пока стоят), pulse (импульс при наступании). */
    public String redstone = "hold";
    public int power = 15;
    public int pulse = 10;
    public boolean sound = false;

    // ---------------------------------------------------------------- состояние
    private final Set<UUID> present = new HashSet<>();
    private final Map<UUID, Long> lastAction = new HashMap<>();
    private int pulseLeft;
    private boolean unloading;

    public HiddenPlateBlockEntity(BlockPos pos, BlockState state) {
        super(GmRegistry.HIDDEN_PLATE_BE.get(), pos, state);
    }

    public Vec3 center() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);
    }

    /** Вперёд по стрелке (yaw как у сущностей). */
    public Vec3 forward() {
        float r = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    /** Вправо, если смотреть по стрелке. */
    public Vec3 right() {
        Vec3 f = forward();
        return new Vec3(-f.z, 0, f.x);
    }

    /** Углы прямоугольника области (по кругу), на высоте блока. */
    public Vec3[] corners() {
        Vec3 c = center(), f = forward().scale(length / 2), r = right().scale(width / 2);
        return new Vec3[]{c.add(f).add(r), c.add(f).subtract(r), c.subtract(f).subtract(r), c.subtract(f).add(r)};
    }

    private boolean insideArea(Vec3 p) {
        Vec3 d = p.subtract(center());
        if (d.y < -0.25 || d.y > 1.5) return false;
        return Math.abs(d.dot(right())) <= width / 2 + 0.3 && Math.abs(d.dot(forward())) <= length / 2 + 0.3;
    }

    private AABB areaBox() {
        double ext = Math.hypot(width, length) / 2 + 0.5;
        Vec3 c = center();
        return new AABB(c.x - ext, c.y - 0.25, c.z - ext, c.x + ext, c.y + 1.5, c.z + ext);
    }

    private boolean matches(Entity e) {
        if (!e.isAlive() || e.isSpectator()) return false;
        return switch (trigger) {
            case "living" -> e instanceof LivingEntity;
            case "non_gm" -> e instanceof Player p && !GmTools.isGm(p);
            default -> e instanceof Player;
        };
    }

    static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, HiddenPlateBlockEntity be) {
        be.tick((ServerLevel) level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        long now = level.getGameTime();
        List<Entity> on = new ArrayList<>();
        for (Entity e : level.getEntities((Entity) null, areaBox(), this::matches)) if (insideArea(e.position())) on.add(e);
        Set<UUID> nowIds = new HashSet<>();
        List<Entity> entered = new ArrayList<>();
        for (Entity e : on) {
            nowIds.add(e.getUUID());
            if (!present.contains(e.getUUID())) entered.add(e);
        }
        boolean wasEmpty = present.isEmpty();
        present.clear();
        present.addAll(nowIds);
        if (!entered.isEmpty()) {
            if (sound && wasEmpty) level.playSound(null, pos, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 0.3f, 0.6f);
            if ("pulse".equals(redstone)) pulseLeft = pulse;
            for (Entity e : entered) act(level, e, now);
        }
        if (!wasEmpty && present.isEmpty() && sound) level.playSound(null, pos, SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF, SoundSource.BLOCKS, 0.3f, 0.5f);
        if (pulseLeft > 0) pulseLeft--;
        if (lastAction.size() > 64) lastAction.values().removeIf(t -> now - t > cooldown);
        int want = switch (redstone) {
            case "hold" -> present.isEmpty() ? 0 : power;
            case "pulse" -> pulseLeft > 0 ? power : 0;
            default -> 0;
        };
        if (state.getValue(HiddenPlateBlock.POWER) != want) {
            level.setBlock(pos, state.setValue(HiddenPlateBlock.POWER, want), 3);
            level.updateNeighborsAt(pos, state.getBlock());
            level.updateNeighborsAt(pos.below(), state.getBlock());
        }
    }

    private void act(ServerLevel level, Entity e, long now) {
        if ("none".equals(action) || GmTeleport.onCooldown(e)) return;
        Long last = lastAction.get(e.getUUID());
        if (last != null && now - last < cooldown) return;
        lastAction.put(e.getUUID(), now);
        Vec3 offset = Vec3.ZERO;
        if (keepOffset) {
            Vec3 d = e.position().subtract(center());
            offset = new Vec3(d.x, 0, d.z);
        }
        boolean keep = "keep".equals(view);
        switch (action) {
            case "teleport" -> {
                ServerLevel to = level;
                if (!tdim.isEmpty()) {
                    ResourceLocation id = ResourceLocation.tryParse(tdim);
                    to = id == null ? null : level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id));
                }
                if (to == null) {
                    RpCore.LOGGER.warn("RP Core: пластина {} — нет измерения {}", worldPosition, tdim);
                    return;
                }
                GmTeleport.teleport(e, to, new Vec3(tx, ty, tz).add(offset), tyaw, tpitch, keep, cooldown);
            }
            case "anchor" -> {
                RpAnchors.Anchor a = RpAnchors.get(level.getServer()).find(anchor);
                ServerLevel to = a == null ? null : RpAnchors.level(level.getServer(), a);
                if (to == null) {
                    RpCore.LOGGER.warn("RP Core: пластина {} — нет точки «{}»", worldPosition, anchor);
                    return;
                }
                GmTeleport.teleport(e, to, a.pos().add(offset), a.yaw(), a.pitch(), keep, cooldown);
            }
            case "command" -> {
                if (command.isEmpty()) return;
                var src = e.createCommandSourceStack().withPermission(cmdLevel).withSuppressedOutput();
                level.getServer().getCommands().performPrefixedCommand(src, command);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- точка с именем
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) GmMarkerSet.add(this);
        else registerAnchor();
    }

    @Override
    public void onChunkUnloaded() {
        unloading = true;
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level == null) return;
        if (level.isClientSide) GmMarkerSet.remove(this);
        else if (!unloading && !name.isEmpty() && level instanceof ServerLevel sl) RpAnchors.get(sl.getServer()).remove(name, owner());
    }

    private String owner() {
        return "plate@" + level.dimension().location() + ":" + worldPosition.toShortString();
    }

    private void registerAnchor() {
        if (!(level instanceof ServerLevel sl) || name.isEmpty()) return;
        RpAnchors.get(sl.getServer()).put(new RpAnchors.Anchor(name, level.dimension(), center(), yaw, 0f, owner()));
    }

    // ---------------------------------------------------------------- подсветка ведущему
    @Override
    public String markerLabel() {
        return name;
    }

    @Override
    public List<Vec3[]> markerLines() {
        List<Vec3[]> out = new ArrayList<>();
        Vec3[] c = corners();
        Vec3 up = new Vec3(0, 0.03, 0);
        for (int i = 0; i < 4; i++) out.add(new Vec3[]{c[i].add(up), c[(i + 1) % 4].add(up)});
        // Стрелка взгляда из центра.
        Vec3 o = center().add(0, 0.05, 0), f = forward(), r = right();
        double len = Math.min(0.9, Math.max(0.5, Math.min(width, length) / 2));
        Vec3 tip = o.add(f.scale(len));
        out.add(new Vec3[]{o, tip});
        out.add(new Vec3[]{tip, tip.subtract(f.scale(0.25)).add(r.scale(0.18))});
        out.add(new Vec3[]{tip, tip.subtract(f.scale(0.25)).subtract(r.scale(0.18))});
        return out;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return areaBox();
    }

    // ---------------------------------------------------------------- настройки: NBT, окно ведущего
    @Override
    public CompoundTag writeSettings() {
        CompoundTag t = new CompoundTag();
        t.putString("name", name);
        t.putFloat("yaw", yaw);
        t.putFloat("width", width);
        t.putFloat("length", length);
        t.putString("trigger", trigger);
        t.putString("action", action);
        t.putDouble("tx", tx);
        t.putDouble("ty", ty);
        t.putDouble("tz", tz);
        t.putFloat("tyaw", tyaw);
        t.putFloat("tpitch", tpitch);
        t.putString("tdim", tdim);
        t.putString("view", view);
        t.putBoolean("keepOffset", keepOffset);
        t.putString("anchor", anchor);
        t.putString("command", command);
        t.putInt("cmdLevel", cmdLevel);
        t.putInt("cooldown", cooldown);
        t.putString("redstone", redstone);
        t.putInt("power", power);
        t.putInt("pulse", pulse);
        t.putBoolean("sound", sound);
        return t;
    }

    @Override
    public void readSettings(CompoundTag t) {
        name = GmTools.clean(t.getString("name"), 32);
        yaw = Mth.wrapDegrees(t.getFloat("yaw"));
        width = Mth.clamp(t.contains("width") ? t.getFloat("width") : 1f, 0.5f, 32f);
        length = Mth.clamp(t.contains("length") ? t.getFloat("length") : 1f, 0.5f, 32f);
        trigger = oneOf(t.getString("trigger"), "players", "non_gm", "living");
        action = oneOf(t.getString("action"), "none", "teleport", "anchor", "command");
        tx = t.getDouble("tx");
        ty = t.getDouble("ty");
        tz = t.getDouble("tz");
        tyaw = Mth.wrapDegrees(t.getFloat("tyaw"));
        tpitch = Mth.clamp(t.getFloat("tpitch"), -90, 90);
        tdim = GmTools.clean(t.getString("tdim"), 128);
        view = oneOf(t.getString("view"), "target", "keep");
        keepOffset = t.getBoolean("keepOffset");
        anchor = GmTools.clean(t.getString("anchor"), 32);
        command = GmTools.clean(t.getString("command"), 256);
        cmdLevel = Mth.clamp(t.contains("cmdLevel") ? t.getInt("cmdLevel") : 2, 0, 4);
        cooldown = Mth.clamp(t.contains("cooldown") ? t.getInt("cooldown") : 20, 0, 20 * 3600);
        redstone = oneOf(t.getString("redstone"), "hold", "off", "pulse");
        power = Mth.clamp(t.contains("power") ? t.getInt("power") : 15, 1, 15);
        pulse = Mth.clamp(t.contains("pulse") ? t.getInt("pulse") : 10, 1, 1200);
        sound = t.getBoolean("sound");
    }

    @Override
    public void applyFrom(ServerPlayer gm, CompoundTag t) {
        String oldName = name;
        readSettings(t);
        if (level instanceof ServerLevel sl && !oldName.isEmpty() && !oldName.equals(name)) RpAnchors.get(sl.getServer()).remove(oldName, owner());
        // Команда пластины не сильнее прав того, кто её записал.
        int own = gm.server.getProfilePermissions(gm.getGameProfile());
        if (cmdLevel > own) cmdLevel = own;
        registerAnchor();
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    static String oneOf(String v, String def, String... others) {
        if (v.equals(def)) return def;
        for (String o : others) if (o.equals(v)) return o;
        return def;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("settings", writeSettings());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("settings")) readSettings(tag.getCompound("settings"));
        else if (tag.contains("public")) {
            CompoundTag p = tag.getCompound("public");
            name = p.getString("name");
            yaw = p.getFloat("yaw");
            width = p.getFloat("width");
            length = p.getFloat("length");
        }
    }

    /** Клиентам — только то, что нужно для подсветки ведущему (имя, стрелка, область); команда и цели — на сервере. */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag p = new CompoundTag();
        p.putString("name", name);
        p.putFloat("yaw", yaw);
        p.putFloat("width", width);
        p.putFloat("length", length);
        CompoundTag t = new CompoundTag();
        t.put("public", p);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
