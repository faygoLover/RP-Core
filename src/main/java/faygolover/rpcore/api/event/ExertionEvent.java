package faygolover.rpcore.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Физическая нагрузка (сервер): RP Stamina сообщает потраченную выносливость, мод питания считает по ней
 * расход калорий. Шлётся через {@link faygolover.rpcore.api.RpCoreAPI#exertion}.
 */
public class ExertionEvent extends PlayerEvent {
    private final double stamina;

    public ExertionEvent(Player player, double stamina) {
        super(player);
        this.stamina = stamina;
    }

    /** Потраченная выносливость (единицы RP Stamina; запас обычного человека — 1000, минута бега — около 960). */
    public double stamina() {
        return stamina;
    }
}
