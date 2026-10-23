package faygolover.rpcore.gm;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Именованные точки мира: пластина или портал с именем записывает сюда, куда к нему прибывать. Телепорт пластины
 * «к точке» и связи порталов RP Portals ищут цель по имени, поэтому её можно передвинуть, не перенастраивая
 * всё, что на неё ведёт. Хранится в мире ({@code data/rpcore_anchors.dat}).
 */
public final class RpAnchors extends SavedData {
    private static final String FILE = "rpcore_anchors";

    /** Точка прибытия: измерение, место, взгляд. {@code owner} — кто записал (блок пластины, портал), для отладки. */
    public record Anchor(String name, ResourceKey<Level> dim, Vec3 pos, float yaw, float pitch, String owner) {}

    private final Map<String, Anchor> anchors = new LinkedHashMap<>();

    public static RpAnchors get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(RpAnchors::load, RpAnchors::new, FILE);
    }

    @Nullable
    public Anchor find(String name) {
        return name == null || name.isEmpty() ? null : anchors.get(name);
    }

    public Collection<Anchor> all() {
        return anchors.values();
    }

    public void put(Anchor a) {
        if (a.name().isEmpty()) return;
        Anchor old = anchors.put(a.name(), a);
        if (!a.equals(old)) setDirty();
    }

    /** Убрать точку, только если её записал этот же владелец (два блока с одним именем не стирают друг друга). */
    public void remove(String name, String owner) {
        Anchor a = anchors.get(name);
        if (a != null && a.owner().equals(owner)) {
            anchors.remove(name);
            setDirty();
        }
    }

    @Nullable
    public static ServerLevel level(MinecraftServer server, Anchor a) {
        return server.getLevel(a.dim());
    }

    private static RpAnchors load(CompoundTag tag) {
        RpAnchors d = new RpAnchors();
        for (Tag t : tag.getList("anchors", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(c.getString("dim")));
            Anchor a = new Anchor(c.getString("name"), dim, new Vec3(c.getDouble("x"), c.getDouble("y"), c.getDouble("z")),
                    c.getFloat("yaw"), c.getFloat("pitch"), c.getString("owner"));
            d.anchors.put(a.name(), a);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Anchor a : anchors.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("name", a.name());
            c.putString("dim", a.dim().location().toString());
            c.putDouble("x", a.pos().x);
            c.putDouble("y", a.pos().y);
            c.putDouble("z", a.pos().z);
            c.putFloat("yaw", a.yaw());
            c.putFloat("pitch", a.pitch());
            c.putString("owner", a.owner());
            list.add(c);
        }
        tag.put("anchors", list);
        return tag;
    }
}
