package faygolover.rpcore.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Надетое в слотах Curios: один способ на всё семейство спросить «что надето». Curios необязателен: без него все
 * методы отвечают «ничего не надето» и не падают. Работает на обеих сторонах — по тому, что известно о сущности
 * здесь (клиент знает надетое у игроков рядом, если Curios его рассылает).
 * <p>
 * <b>Когда использовать:</b> моду надо найти предмет в слотах Curios, проверить, надет ли он, или сообщить, что
 * надетый предмет изменился. Свой {@code CuriosCompat} с вложенным классом, отражением или {@code MethodHandle}
 * писать не нужно.
 * <pre>{@code
 * if (Worn.any(player, s -> s.is(MyItems.LAMP.get()))) …            // надето где угодно
 * ItemStack head = Worn.first(player, "head");                      // первый предмет в слоте «head»
 * List<ItemStack> pdas = Worn.all(player, isPda, "pda");            // сначала из своего слота, потом из остальных
 * Worn.touched(player, "belt");                                     // поменял NBT надетого — разослать
 * }</pre>
 * <b>Чего здесь нет нарочно:</b> раздача и ограничение числа слотов, обыск, перенос надетого в другое хранилище —
 * это механика конкретных модов; им нужен сам Curios.
 * <p>
 * Возвращаются сами предметы из слотов, не копии: менять можно, после правки NBT вызвать {@link #touched}.
 *
 * @since 0.15.0
 */
public final class Worn {
    private Worn() {}

    private static final boolean LOADED = ModList.get().isLoaded("curios");

    /** Стоит ли Curios. */
    public static boolean loaded() {
        return LOADED;
    }

    /** Надет ли в любом слоте предмет, подходящий под {@code test}. */
    public static boolean any(LivingEntity e, Predicate<ItemStack> test) {
        return LOADED && Impl.any(e, null, test);
    }

    /** Надет ли предмет, подходящий под {@code test}, в одном из слотов {@code slots} (имена слотов Curios). */
    public static boolean anyIn(LivingEntity e, Collection<String> slots, Predicate<ItemStack> test) {
        return LOADED && Impl.any(e, slots, test);
    }

    /** Все надетые предметы, подходящие под {@code test}, в порядке слотов Curios. */
    public static List<ItemStack> all(LivingEntity e, Predicate<ItemStack> test) {
        return LOADED ? Impl.all(e, test, null) : List.of();
    }

    /** То же, но сначала предметы из слота {@code firstSlot}, потом из остальных. */
    public static List<ItemStack> all(LivingEntity e, Predicate<ItemStack> test, String firstSlot) {
        return LOADED ? Impl.all(e, test, firstSlot) : List.of();
    }

    /** Первый непустой предмет в слоте {@code slot} или {@link ItemStack#EMPTY}. */
    public static ItemStack first(LivingEntity e, String slot) {
        return LOADED ? Impl.first(e, slot) : ItemStack.EMPTY;
    }

    /** Все непустые предметы в слоте {@code slot}. */
    public static List<ItemStack> inSlot(LivingEntity e, String slot) {
        return LOADED ? Impl.inSlot(e, slot) : List.of();
    }

    /** Сказать Curios, что предметы в слоте {@code slot} изменились (NBT), — он разошлёт их игрокам рядом. */
    public static void touched(LivingEntity e, String slot) {
        if (LOADED) Impl.touched(e, slot);
    }

    /** Классы Curios — только здесь: этот класс грузится, лишь когда Curios стоит. */
    private static final class Impl {
        static boolean any(LivingEntity e, Collection<String> slots, Predicate<ItemStack> test) {
            ICuriosItemHandler h = CuriosApi.getCuriosInventory(e).resolve().orElse(null);
            if (h == null) return false;
            if (slots == null) {
                for (ICurioStacksHandler sh : h.getCurios().values())
                    if (has(sh, test)) return true;
                return false;
            }
            for (String slot : slots) {
                ICurioStacksHandler sh = h.getCurios().get(slot);
                if (sh != null && has(sh, test)) return true;
            }
            return false;
        }

        private static boolean has(ICurioStacksHandler sh, Predicate<ItemStack> test) {
            IDynamicStackHandler stacks = sh.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack s = stacks.getStackInSlot(i);
                if (!s.isEmpty() && test.test(s)) return true;
            }
            return false;
        }

        static List<ItemStack> all(LivingEntity e, Predicate<ItemStack> test, String firstSlot) {
            ICuriosItemHandler h = CuriosApi.getCuriosInventory(e).resolve().orElse(null);
            if (h == null) return List.of();
            List<ItemStack> own = new ArrayList<>(), other = new ArrayList<>();
            for (Map.Entry<String, ICurioStacksHandler> en : h.getCurios().entrySet()) {
                List<ItemStack> to = en.getKey().equals(firstSlot) ? own : other;
                IDynamicStackHandler stacks = en.getValue().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack s = stacks.getStackInSlot(i);
                    if (!s.isEmpty() && test.test(s)) to.add(s);
                }
            }
            own.addAll(other);
            return own;
        }

        static ItemStack first(LivingEntity e, String slot) {
            ICurioStacksHandler sh = handler(e, slot);
            if (sh == null) return ItemStack.EMPTY;
            for (int i = 0; i < sh.getSlots(); i++) {
                ItemStack s = sh.getStacks().getStackInSlot(i);
                if (!s.isEmpty()) return s;
            }
            return ItemStack.EMPTY;
        }

        static List<ItemStack> inSlot(LivingEntity e, String slot) {
            ICurioStacksHandler sh = handler(e, slot);
            if (sh == null) return List.of();
            List<ItemStack> out = new ArrayList<>();
            for (int i = 0; i < sh.getSlots(); i++) {
                ItemStack s = sh.getStacks().getStackInSlot(i);
                if (!s.isEmpty()) out.add(s);
            }
            return out;
        }

        static void touched(LivingEntity e, String slot) {
            ICurioStacksHandler sh = handler(e, slot);
            if (sh == null) return;
            for (int i = 0; i < sh.getSlots(); i++) {
                ItemStack s = sh.getStacks().getStackInSlot(i);
                if (!s.isEmpty()) sh.getStacks().setStackInSlot(i, s);
            }
        }

        private static ICurioStacksHandler handler(LivingEntity e, String slot) {
            ICuriosItemHandler h = CuriosApi.getCuriosInventory(e).resolve().orElse(null);
            return h == null ? null : h.getCurios().get(slot);
        }
    }
}
