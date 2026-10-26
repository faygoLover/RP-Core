package faygolover.rpcore.api;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Вкусы персонажа (часть анкеты): любимое и нелюбимое — вид еды («из чего») или способ («как приготовлено»).
 * Необязательно; но выбрал любимое — выбери и нелюбимое (и не то же самое). Игрок выбирает один раз (и при уже
 * заполненной анкете), дальше меняет ГМ ({@code /rpcore taste}). Читает мод питания (RP Culinary).
 * <p>
 * Значение — {@code kind:<вид>} или {@code method:<способ>}, пусто — не выбрано.
 *
 * @param chosen игрок уже выбирал (сам больше не меняет)
 * @since 0.10.0
 */
public record FoodTaste(String favourite, String disliked, boolean chosen) {
    /** Виды еды «из чего» (RP Culinary). */
    public static final List<String> KINDS = List.of("meat", "fish", "grain", "vegetables", "fruit", "sweet", "dairy", "mushrooms", "eggs");
    /** Способы «как приготовлено» (RP Culinary). */
    public static final List<String> METHODS = List.of("soup", "fried", "sandwich", "baked", "porridge", "fresh", "canned", "dessert");
    /** Ничего не выбрано (объявлено после списков: конструктор их проверяет). */
    public static final FoodTaste NONE = new FoodTaste("", "", false);

    public FoodTaste {
        favourite = valid(favourite) ? favourite : "";
        disliked = valid(disliked) ? disliked : "";
    }

    /** Все варианты по порядку: сначала виды, потом способы. */
    public static List<String> options() {
        List<String> out = new ArrayList<>();
        for (String k : KINDS) out.add("kind:" + k);
        for (String m : METHODS) out.add("method:" + m);
        return out;
    }

    public static boolean valid(String v) {
        return v != null && options().contains(v);
    }

    /** Пара допустима: обе пусты, или обе выбраны и разные. */
    public static boolean validPair(String favourite, String disliked) {
        boolean f = valid(favourite), d = valid(disliked);
        if (!f && !d) return (favourite == null || favourite.isEmpty()) && (disliked == null || disliked.isEmpty());
        return f && d && !favourite.equals(disliked);
    }

    /** Ключ перевода варианта: {@code rpcore.taste.kind.meat}. */
    public static String langKey(String v) {
        return v == null || v.isEmpty() ? "rpcore.taste.none" : "rpcore.taste." + v.replace(':', '.');
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("favourite", favourite);
        t.putString("disliked", disliked);
        t.putBoolean("chosen", chosen);
        return t;
    }

    public static FoodTaste load(CompoundTag t) {
        if (t == null || t.isEmpty()) return NONE;
        return new FoodTaste(t.getString("favourite"), t.getString("disliked"), t.getBoolean("chosen"));
    }
}
