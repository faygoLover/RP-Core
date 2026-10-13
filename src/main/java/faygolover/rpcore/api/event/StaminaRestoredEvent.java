package faygolover.rpcore.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Выносливость восстановилась (сервер): RP Stamina сообщает, сколько вернулось за шаг — отдыхом, предметами,
 * стимуляторами. Мод питания (RP Culinary) берёт за это калории: еда тратится на отдых после нагрузки, а не на саму
 * нагрузку. Слушать на {@code MinecraftForge.EVENT_BUS}; посылать — {@link faygolover.rpcore.api.RpCoreAPI#staminaRestored}.
 *
 * @since 0.5.0
 */
public class StaminaRestoredEvent extends PlayerEvent {
    private final double amount;

    public StaminaRestoredEvent(Player player, double amount) {
        super(player);
        this.amount = amount;
    }

    /** Сколько выносливости вернулось (единицы RP Stamina; запас обычного человека — 1000). */
    public double amount() {
        return amount;
    }
}
