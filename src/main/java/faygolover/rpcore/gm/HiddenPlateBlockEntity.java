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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Невидимая пластина: кто на ней стоит (по тикам), что делать при наступании и какой сигнал красного камня давать.
 * Каждая сущность срабатывает отдельно, когда встаёт на пластину, — «паровозик» игроков проходит без очереди.
 * Сошла — в тот же тик отпускает. Настройки видит и меняет только ведущий (команда в пластине — его права).
 */
public class HiddenPlateBlockEntity extends BlockEntity implements GmConfigurable {
    // ---------------------------------------------------------------- настройки
    public String name = "";
    /** Кого замечает: players — игроков, non_gm — игроков без прав ведущего, living — всех живых. */
    public String trigger = "players";
    /** Что делает: none, teleport (координаты), anchor (именованная точка), command. */
    public String action = "none";
    public double tx, ty, tz;
    public float tyaw, tpitch;
    /** Измерение цели, пусто — это же. */
    public String tdim = "";
    public boolean keepView = true;
    /** Сохранить сдвиг от центра пластины — «паровозик» прибывает тем же строем. */
    public boolean keepOffset = true;
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
    private final Map<UUID, Long> present = new HashMap<>();
    private final Map<UUID, Long> lastAction = new HashMap<>();
    private final List<Entity> entered = new ArrayList<>();
    private int pulseLeft;
    private boolean unloading;

    public HiddenPlateBlockEntity(BlockPos pos, BlockState state) {
        super(GmRegistry.HIDDEN_PLATE_BE.get(), pos, state);
    }

    /** Из {@code entityInside}: сущность в объёме блока в этот тик. */
    void touch(Entity e) {
        if (!matches(e)) return;
        long now = e.level().getGameTime();
        Long before = present.put(e.getUUID(), now);
        if (before == null) entered.add(e);
    }

    private boolean matches(Entity e) {
        if (!e.isAlive() || e.isSpectator()) return false;
        return switch (trigger) {
            case "living" -> e instanceof LivingEntity;
            case "non_gm" -> e instanceof Player p && !GmTools.isGm(p);
            default -> e instanceof Player;
        };
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, HiddenPlateBlockEntity be) {
        be.tick((ServerLevel) level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        long now = level.getGameTime();
        boolean left = present.values().removeIf(t -> t < now);
        if (!entered.isEmpty()) {
            List<Entity> list = new ArrayList<>(entered);
            entered.clear();
            for (Entity e : list) {
                if (e.isRemoved()) continue;
                if (sound) level.playSound(null, pos, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 0.3f, 0.6f);
                if ("pulse".equals(redstone)) pulseLeft = pulse;
                act(level, pos, e, now);
            }
        }
        if (left && sound && present.isEmpty()) level.playSound(null, pos, SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF, SoundSource.BLOCKS, 0.3f, 0.5f);
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

    private void act(ServerLevel level, BlockPos pos, Entity e, long now) {
        if ("none".equals(action) || GmTeleport.onCooldown(e)) return;
        Long last = lastAction.get(e.getUUID());
        if (last != null && now - last < cooldown) return;
        lastAction.put(e.getUUID(), now);
        Vec3 offset = keepOffset ? new Vec3(e.getX() - (pos.getX() + 0.5), 0, e.getZ() - (pos.getZ() + 0.5)) : Vec3.ZERO;
        switch (action) {
            case "teleport" -> {
                ServerLevel to = level;
                if (!tdim.isEmpty()) {
                    ResourceLocation id = ResourceLocation.tryParse(tdim);
                    to = id == null ? null : level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id));
                }
                if (to == null) {
                    RpCore.LOGGER.warn("RP Core: пластина {} — нет измерения {}", pos, tdim);
                    return;
                }
                GmTeleport.teleport(e, to, new Vec3(tx, ty, tz).add(offset), tyaw, tpitch, keepView, cooldown);
            }
            case "anchor" -> {
                RpAnchors.Anchor a = RpAnchors.get(level.getServer()).find(anchor);
                ServerLevel to = a == null ? null : RpAnchors.level(level.getServer(), a);
                if (to == null) {
                    RpCore.LOGGER.warn("RP Core: пластина {} — нет точки «{}»", pos, anchor);
                    return;
                }
                GmTeleport.teleport(e, to, a.pos().add(offset), a.yaw(), a.pitch(), keepView, cooldown);
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
        RpAnchors.get(sl.getServer()).put(new RpAnchors.Anchor(name, level.dimension(),
                new Vec3(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5), tyaw, tpitch, owner()));
    }

    // ---------------------------------------------------------------- настройки: NBT, окно ведущего
    @Override
    public CompoundTag writeSettings() {
        CompoundTag t = new CompoundTag();
        t.putString("name", name);
        t.putString("trigger", trigger);
        t.putString("action", action);
        t.putDouble("tx", tx);
        t.putDouble("ty", ty);
        t.putDouble("tz", tz);
        t.putFloat("tyaw", tyaw);
        t.putFloat("tpitch", tpitch);
        t.putString("tdim", tdim);
        t.putBoolean("keepView", keepView);
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
        String oldName = name;
        name = GmTools.clean(t.getString("name"), 32);
        trigger = oneOf(t.getString("trigger"), "players", "non_gm", "living");
        action = oneOf(t.getString("action"), "none", "teleport", "anchor", "command");
        tx = t.getDouble("tx");
        ty = t.getDouble("ty");
        tz = t.getDouble("tz");
        tyaw = Mth.wrapDegrees(t.getFloat("tyaw"));
        tpitch = Mth.clamp(t.getFloat("tpitch"), -90, 90);
        tdim = GmTools.clean(t.getString("tdim"), 128);
        keepView = !t.contains("keepView") || t.getBoolean("keepView");
        keepOffset = !t.contains("keepOffset") || t.getBoolean("keepOffset");
        anchor = GmTools.clean(t.getString("anchor"), 32);
        command = GmTools.clean(t.getString("command"), 256);
        cmdLevel = Mth.clamp(t.contains("cmdLevel") ? t.getInt("cmdLevel") : 2, 0, 4);
        cooldown = Mth.clamp(t.contains("cooldown") ? t.getInt("cooldown") : 20, 0, 20 * 3600);
        redstone = oneOf(t.getString("redstone"), "hold", "off", "pulse");
        power = Mth.clamp(t.contains("power") ? t.getInt("power") : 15, 1, 15);
        pulse = Mth.clamp(t.contains("pulse") ? t.getInt("pulse") : 10, 1, 1200);
        sound = t.getBoolean("sound");
        if (level instanceof ServerLevel sl && !oldName.equals(name) && !oldName.isEmpty()) RpAnchors.get(sl.getServer()).remove(oldName, owner());
    }

    @Override
    public void applyFrom(ServerPlayer gm, CompoundTag t) {
        readSettings(t);
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
        else if (tag.contains("name")) name = tag.getString("name");   // пакет клиенту: только имя для подписи
    }

    /** Клиентам — только имя (подпись у контура для ведущего); команда и цели остаются на сервере. */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = new CompoundTag();
        t.putString("name", name);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public String markerLabel() {
        return name;
    }
}
