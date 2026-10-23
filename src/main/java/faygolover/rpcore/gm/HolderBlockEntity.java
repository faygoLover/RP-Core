package faygolover.rpcore.gm;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Держатель предмета (настенный и «сферический в вакууме»): сам предмет со всеми данными и то, как его показывать.
 * Игрок забирает предмет ПКМ — держатель пропадает (или ждёт {@link #respawn} секунд и показывает копию снова).
 */
public class HolderBlockEntity extends BlockEntity implements GmConfigurable {
    public ItemStack item = ItemStack.EMPTY;
    /** Вид: flat — плоско, как в рамке; item — как выброшенный предмет; block — в натуральную величину (блоки). */
    public String render;
    public float scale = 1.0f;
    /** Поворот вокруг оси держателя (у настенного — оси стены, у сферического — вертикали), градусы. */
    public float rot;
    /** Наклон (сферический), градусы. */
    public float tilt;
    /** Вращение, градусов в секунду. */
    public float spin;
    public boolean bob;
    /** Кто может забрать: all — все, gm — только ведущий. */
    public String takeBy = "all";
    /** Через сколько секунд предмет появится снова; 0 — держатель пропадает, когда предмет забрали. */
    public int respawn;

    private ItemStack template = ItemStack.EMPTY;
    private long respawnAt;

    public HolderBlockEntity(BlockPos pos, BlockState state) {
        super(GmRegistry.HOLDER_BE.get(), pos, state);
        render = isWall() ? "flat" : "item";
    }

    public boolean isWall() {
        return getBlockState().getBlock() instanceof WallHolderBlock;
    }

    public void setItem(ItemStack stack) {
        item = stack;
        if (respawn > 0) template = stack.copy();
        changed();
    }

    /** Предмет забрали: убрать держатель или ждать копию. Возвращает true, если держатель остаётся. */
    boolean taken() {
        if (respawn > 0 && level != null) {
            if (template.isEmpty()) template = item.copy();
            item = ItemStack.EMPTY;
            respawnAt = level.getGameTime() + respawn * 20L;
            changed();
            return true;
        }
        item = ItemStack.EMPTY;
        return false;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, HolderBlockEntity be) {
        if (be.respawnAt > 0 && level.getGameTime() >= be.respawnAt) {
            be.respawnAt = 0;
            if (be.item.isEmpty() && !be.template.isEmpty()) {
                be.item = be.template.copy();
                be.changed();
            }
        }
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Размер видимой части (для формы выделения и рамки отрисовки), в блоках. */
    public float size() {
        float base = "block".equals(render) ? 1.0f : "item".equals(render) ? 0.5f : 0.6f;
        return Mth.clamp(base * scale, 0.1f, 4.0f);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(Math.max(0.5, size()));
    }

    // ---------------------------------------------------------------- маркеры ведущего
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) GmMarkerSet.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) GmMarkerSet.remove(this);
    }

    @Override
    public String markerLabel() {
        return item.isEmpty() ? "" : item.getHoverName().getString();
    }

    // ---------------------------------------------------------------- настройки
    @Override
    public CompoundTag writeSettings() {
        CompoundTag t = new CompoundTag();
        t.putString("render", render);
        t.putFloat("scale", scale);
        t.putFloat("rot", rot);
        t.putFloat("tilt", tilt);
        t.putFloat("spin", spin);
        t.putBoolean("bob", bob);
        t.putString("takeBy", takeBy);
        t.putInt("respawn", respawn);
        t.putBoolean("collide", getBlockState().getValue(HolderBlock.COLLIDE));
        t.putInt("count", item.isEmpty() ? (template.isEmpty() ? 0 : template.getCount()) : item.getCount());
        return t;
    }

    @Override
    public void readSettings(CompoundTag t) {
        render = isWall() ? HiddenPlateBlockEntity.oneOf(t.getString("render"), "flat", "block")
                : HiddenPlateBlockEntity.oneOf(t.getString("render"), "item", "flat", "block");
        scale = Mth.clamp(t.contains("scale") ? t.getFloat("scale") : 1.0f, 0.1f, 4.0f);
        rot = Mth.wrapDegrees(t.getFloat("rot"));
        tilt = Mth.clamp(t.getFloat("tilt"), -90f, 90f);
        spin = Mth.clamp(t.getFloat("spin"), -720f, 720f);
        bob = t.getBoolean("bob");
        takeBy = HiddenPlateBlockEntity.oneOf(t.getString("takeBy"), "all", "gm");
        respawn = Mth.clamp(t.getInt("respawn"), 0, 7 * 24 * 3600);
    }

    @Override
    public void applyFrom(ServerPlayer gm, CompoundTag t) {
        readSettings(t);
        // Сколько выдать: можно поменять после того, как предмет положили (до размера стака предмета).
        if (t.contains("count")) {
            for (ItemStack s : new ItemStack[]{item, template}) {
                if (!s.isEmpty()) s.setCount(Mth.clamp(t.getInt("count"), 1, s.getMaxStackSize()));
            }
        }
        if (respawn > 0 && template.isEmpty()) template = item.copy();
        if (respawn == 0) {
            template = ItemStack.EMPTY;
            respawnAt = 0;
        }
        if (level instanceof ServerLevel sl) {
            BlockState st = getBlockState();
            boolean collide = t.getBoolean("collide");
            if (st.getValue(HolderBlock.COLLIDE) != collide) sl.setBlock(worldPosition, st.setValue(HolderBlock.COLLIDE, collide), 3);
        }
        changed();
    }

    // ---------------------------------------------------------------- сохранение и синхронизация
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeShared(tag);
        if (!template.isEmpty()) tag.put("template", template.save(new CompoundTag()));
        if (respawnAt > 0) tag.putLong("respawnAt", respawnAt);
    }

    private void writeShared(CompoundTag tag) {
        tag.put("settings", writeSettings());
        if (!item.isEmpty()) tag.put("item", item.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("settings")) readSettings(tag.getCompound("settings"));
        item = tag.contains("item") ? ItemStack.of(tag.getCompound("item")) : ItemStack.EMPTY;
        template = tag.contains("template") ? ItemStack.of(tag.getCompound("template")) : ItemStack.EMPTY;
        respawnAt = tag.getLong("respawnAt");
    }

    /** Клиентам — предмет и вид (это видят все), без копии для повторного появления. */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = new CompoundTag();
        writeShared(t);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
