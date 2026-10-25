package faygolover.rpcore.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import faygolover.rpcore.RpCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Общая база того, что пьют, едят и курят (датапак {@code rpcore/consumables}): вода, кофеин, алкоголь, никотин.
 * Её читают RP Culinary (вода, «без калорий»), RP Medicine (вещества), RP Stamina (бодрость), RP Perks (зависимости)
 * и RP Psyche (снятие стресса) — один список на всё семейство.
 * <pre>{"items": ["sa_combat:energy_drink"],        // допил или доел
 *  "right_click": ["the_dirty_stuff:cigarette"],     // ПКМ предметом (курение без «допивания»)
 *  "effects": ["tobacconistmod:nicotine"],           // наложенный эффект
 *  "potion": "minecraft:water",                      // только это зелье (для minecraft:potion)
 *  "water": 20,                                      // проценты жажды; минус — сушит
 *  "calories": false,                                // не еда: сытость не даёт (вода в бутылке)
 *  "caffeine": "strong",                             // trace | weak | strong или число (strong = 1)
 *  "alcohol": "medium",                              // weak | medium | strong или число (weak = 1, как пиво)
 *  "nicotine": 1.0,
 *  "cooldown_seconds": 30}                           // повтор раньше — без доз веществ</pre>
 * Свойство, которого нет в файле, — «не задано» (у воды и веществ — 0, у калорий — есть).
 */
public final class Consumables {
    private Consumables() {}

    /** Свойства предмета. {@code water} — NaN, если файл про воду молчит. */
    public record Entry(double water, boolean calories, double caffeine, double alcohol, double nicotine, double cooldownSeconds) {
        public boolean hasWater() {
            return !Double.isNaN(water);
        }

        public boolean hasSubstance() {
            return caffeine > 0 || alcohol > 0 || nicotine > 0;
        }
    }

    private record Rule(@Nullable ResourceLocation potion, Entry entry) {}

    private static volatile Map<ResourceLocation, List<Rule>> byItem = Map.of();
    private static volatile Map<ResourceLocation, Entry> byClick = Map.of();
    private static volatile Map<ResourceLocation, Entry> byEffect = Map.of();

    public static final SimpleJsonResourceReloadListener LOADER = new SimpleJsonResourceReloadListener(new Gson(), "rpcore/consumables") {
        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
            Map<ResourceLocation, List<Rule>> items = new HashMap<>();
            Map<ResourceLocation, Entry> clicks = new HashMap<>();
            Map<ResourceLocation, Entry> effects = new HashMap<>();
            for (var f : files.entrySet()) {
                try {
                    JsonObject o = f.getValue().getAsJsonObject();
                    Entry e = new Entry(
                            o.has("water") ? GsonHelper.getAsDouble(o, "water") : Double.NaN,
                            GsonHelper.getAsBoolean(o, "calories", true),
                            amount(o, "caffeine", CAFFEINE),
                            amount(o, "alcohol", ALCOHOL),
                            amount(o, "nicotine", Map.of()),
                            GsonHelper.getAsDouble(o, "cooldown_seconds", 0));
                    ResourceLocation potion = o.has("potion") ? ResourceLocation.tryParse(GsonHelper.getAsString(o, "potion")) : null;
                    for (ResourceLocation id : ids(o, "items")) items.computeIfAbsent(id, k -> new ArrayList<>()).add(new Rule(potion, e));
                    for (ResourceLocation id : ids(o, "right_click")) clicks.put(id, e);
                    for (ResourceLocation id : ids(o, "effects")) effects.put(id, e);
                } catch (Exception ex) {
                    RpCore.LOGGER.error("RP Core: consumables {}: {}", f.getKey(), ex.getMessage());
                }
            }
            // Правило с зельем проверяется раньше общего.
            items.values().forEach(l -> l.sort((a, b) -> Boolean.compare(b.potion != null, a.potion != null)));
            byItem = Map.copyOf(items);
            byClick = Map.copyOf(clicks);
            byEffect = Map.copyOf(effects);
            RpCore.LOGGER.info("RP Core: consumables — предметов {}, по ПКМ {}, эффектов {}", items.size(), clicks.size(), effects.size());
        }
    };

    /** Слова силы: кофеин в «чашках кофе», алкоголь в «кружках пива». */
    private static final Map<String, Double> CAFFEINE = Map.of("trace", 0.15, "weak", 0.5, "strong", 1.0);
    private static final Map<String, Double> ALCOHOL = Map.of("weak", 1.0, "medium", 1.5, "strong", 2.0);

    private static double amount(JsonObject o, String key, Map<String, Double> words) {
        if (!o.has(key)) return 0;
        JsonElement el = o.get(key);
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) return Math.max(0, el.getAsDouble());
        Double v = words.get(el.getAsString());
        if (v == null) throw new IllegalArgumentException(key + ": неизвестная сила «" + el.getAsString() + "»");
        return v;
    }

    private static List<ResourceLocation> ids(JsonObject o, String key) {
        List<ResourceLocation> out = new ArrayList<>();
        for (JsonElement el : GsonHelper.getAsJsonArray(o, key, new JsonArray())) {
            ResourceLocation rl = ResourceLocation.tryParse(el.getAsString());
            if (rl != null) out.add(rl);
        }
        return out;
    }

    /** Допитый или доеденный предмет (null — в базе нет). */
    @Nullable
    public static Entry forItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        List<Rule> rules = byItem.get(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        if (rules == null) return null;
        ResourceLocation potion = null;
        for (Rule r : rules) {
            if (r.potion == null) return r.entry;
            if (potion == null) potion = ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(stack));
            if (r.potion.equals(potion)) return r.entry;
        }
        return null;
    }

    /** ПКМ предметом (сигареты без «допивания»). */
    @Nullable
    public static Entry forClick(Item item) {
        return byClick.get(ForgeRegistries.ITEMS.getKey(item));
    }

    /** Наложенный эффект (никотин Tobacconist). */
    @Nullable
    public static Entry forEffect(MobEffect effect) {
        return byEffect.get(ForgeRegistries.MOB_EFFECTS.getKey(effect));
    }
}
