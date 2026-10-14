package faygolover.rpcore.client.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import faygolover.rpcore.RpCore;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Раскладка HUD игрока: {@code config/rpcore-hud.json}. Элемент, которого в файле нет, получает значения по
 * умолчанию из {@link HudElement} (так переезжают раскладки из старых конфигов модов).
 */
public final class HudLayout {
    private HudLayout() {}

    /** Положение одного элемента. */
    public static final class Entry {
        public Anchor anchor;
        public int x;
        public int y;
        public double scale;
        public boolean visible;
        /** Иконка элемента ({@link HudElement#hasIcon}). */
        public boolean icon = true;

        Entry(Anchor anchor, int x, int y, double scale, boolean visible) {
            this.anchor = anchor;
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.visible = visible;
        }

        static Entry defaults(HudElement e) {
            return new Entry(e.defaultAnchor(), e.defaultX(), e.defaultY(), e.defaultScale(), e.defaultVisible());
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Entry> ENTRIES = new HashMap<>();
    private static boolean loaded;

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rpcore-hud.json");
    }

    public static synchronized Entry get(HudElement e) {
        if (!loaded) load();
        return ENTRIES.computeIfAbsent(e.id().toString(), k -> Entry.defaults(e));
    }

    /** Рисовать ли иконку элемента. @since 0.5.2 */
    public static boolean iconShown(HudElement e) {
        return get(e).icon;
    }

    public static synchronized void reset(HudElement e) {
        ENTRIES.put(e.id().toString(), Entry.defaults(e));
    }

    /** Прямоугольник элемента на экране: {x, y, w, h}. */
    public static int[] rect(HudElement e, int screenW, int screenH) {
        Entry c = get(e);
        int w = (int) Math.round(e.width() * c.scale);
        int h = (int) Math.round(e.height() * c.scale);
        int x = Math.round(screenW * c.anchor.fx - w * c.anchor.fx) + c.x;
        int y = Math.round(screenH * c.anchor.fy - h * c.anchor.fy) + c.y;
        return new int[]{x, y, w, h};
    }

    private static void load() {
        loaded = true;
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            for (var en : root.entrySet()) {
                JsonObject o = en.getValue().getAsJsonObject();
                Entry c = new Entry(
                        Anchor.parse(o.has("anchor") ? o.get("anchor").getAsString() : "", Anchor.TOP_LEFT),
                        o.has("x") ? o.get("x").getAsInt() : 0,
                        o.has("y") ? o.get("y").getAsInt() : 0,
                        o.has("scale") ? Math.max(0.25, Math.min(4.0, o.get("scale").getAsDouble())) : 1.0,
                        !o.has("visible") || o.get("visible").getAsBoolean());
                c.icon = !o.has("icon") || o.get("icon").getAsBoolean();
                ENTRIES.put(en.getKey(), c);
            }
        } catch (Exception ex) {
            RpCore.LOGGER.warn("RP Core: не удалось прочитать {}: {}", f, ex.toString());
        }
    }

    public static synchronized void save() {
        JsonObject root = new JsonObject();
        for (var en : ENTRIES.entrySet()) {
            Entry c = en.getValue();
            JsonObject o = new JsonObject();
            o.addProperty("anchor", c.anchor.name());
            o.addProperty("x", c.x);
            o.addProperty("y", c.y);
            o.addProperty("scale", c.scale);
            o.addProperty("visible", c.visible);
            if (!c.icon) o.addProperty("icon", false);
            root.add(en.getKey(), o);
        }
        try {
            Files.createDirectories(file().getParent());
            try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (Exception ex) {
            RpCore.LOGGER.warn("RP Core: не удалось сохранить {}: {}", file(), ex.toString());
        }
    }
}
