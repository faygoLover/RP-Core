package faygolover.rpcore.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Игрок съел или выпил долю предмета (сервер): укус большого блюда, глоток напитка, последний кусок. Сюда смотрят
 * все, кому важно, ЧТО употреблено (вещества RP Medicine, бодрость RP Stamina, кофеин RP Perks, стресс RP Psyche),
 * и берут свою долю: {@code fraction} — часть целого предмета (1 — весь).
 * <p>
 * Шлёт мод питания (RP Culinary — он ведёт укусы и глотки, {@link faygolover.rpcore.api.RpIds#FOOD_PORTIONS}).
 * Без него RP Core сам шлёт событие с долей 1, когда предмет доеден или допит. Слушать стоит это событие, а не
 * ванильное «доел»: иначе укусы и глотки не засчитаются, а последний — засчитается как целый предмет.
 *
 * @since 0.10.0
 */
public class RpConsumeEvent extends PlayerEvent {
    private final ItemStack stack;
    private final double fraction;

    public RpConsumeEvent(Player player, ItemStack stack, double fraction) {
        super(player);
        this.stack = stack;
        this.fraction = fraction;
    }

    /** Что употреблено (копия; у надкусанного и начатого — с пометками укусов и глотков). */
    public ItemStack stack() {
        return stack;
    }

    /** Доля целого предмета, 0–1. */
    public double fraction() {
        return fraction;
    }
}
