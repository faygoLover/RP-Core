package faygolover.rpcore.api;

import net.minecraft.world.entity.player.Player;

/**
 * Мод, который ведёт сон (RP Needs). Единый признак «спит» для заживления, психики и т. д. и стадия недосыпа.
 * Регистрирует — {@link RpCoreAPI#registerSleep}. Методы вызываются на сервере.
 * @since 0.14.0
 */
public interface SleepProvider {
    /** Спит по-настоящему: «лёг, но не уснул» не считается. */
    boolean asleep(Player player);

    /** Стадия недосыпа: 0 бодр, 1 хочется спать, 2 сонливость, 3 валится с ног, 4 засыпает на ходу. */
    int stage(Player player);
}
