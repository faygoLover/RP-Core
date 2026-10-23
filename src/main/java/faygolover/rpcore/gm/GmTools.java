package faygolover.rpcore.gm;

import faygolover.rpcore.CoreConfig;
import faygolover.rpcore.RpCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;

/**
 * Общее для «ГМских штук» (невидимая пластина, держатели, порталы RP Portals): кто ведущий и что значит
 * «держит предмет-метку». Метки — тег {@code rpcore:gm_markers}: с любым из этих предметов в руке ведущий видит
 * контуры всех таких блоков, может их выделить, сломать и открыть их настройки.
 */
public final class GmTools {
    private GmTools() {}

    public static final TagKey<Item> MARKERS = TagKey.create(Registries.ITEM, new ResourceLocation(RpCore.MODID, "gm_markers"));

    /** В руке (любой) предмет-метка. На клиенте этого достаточно, чтобы видеть и выделять блоки: у игроков меток нет. */
    public static boolean holdsMarker(Entity e) {
        return e instanceof Player p && (p.getMainHandItem().is(MARKERS) || p.getOffhandItem().is(MARKERS));
    }

    /** Контекст формы — от игрока с меткой в руке (выделение и ломание блока ведущим). */
    public static boolean markerContext(CollisionContext ctx) {
        return ctx instanceof EntityCollisionContext ecc && holdsMarker(ecc.getEntity());
    }

    /** Ведущий на сервере: права из {@code access.gm_permission_level} Core (по умолчанию 2). */
    public static boolean isGm(Player p) {
        return p.hasPermissions(gmLevel());
    }

    public static int gmLevel() {
        try {
            return CoreConfig.ACCESS_PERMISSION_LEVEL.get();
        } catch (IllegalStateException notLoaded) {
            return 2;
        }
    }

    /** Строка из настроек: обрезать до {@code max} символов, без переводов строк. */
    public static String clean(String s, int max) {
        if (s == null) return "";
        s = s.replace('\n', ' ').replace('\r', ' ').strip();
        return s.length() > max ? s.substring(0, max) : s;
    }
}
