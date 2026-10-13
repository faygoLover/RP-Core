package faygolover.rpcore.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Источник навыков и черт игрока. Регистрирует мод, который их ведёт (сейчас — RP Perks).
 * Методы вызываются и на сервере, и на клиенте: источник сам решает, откуда брать данные
 * (на клиенте — из синхронизированной копии).
 */
public interface TraitProvider {
    /** Ведёт ли источник этот навык (тогда у навыка есть уровни). */
    default boolean managesSkill(ResourceLocation skill) {
        return false;
    }

    /** Уровень навыка, 0–10. Вызывается, только если {@link #managesSkill} вернул true. */
    default int skill(Player player, ResourceLocation skill) {
        return 0;
    }

    /** Выставить уровень (команда ГМа); {@code level < 0} — сбросить к значению по умолчанию. false — не умеет. */
    default boolean setSkill(Player player, ResourceLocation skill, int level) {
        return false;
    }

    /**
     * Практика навыка (рост навыков): мод, где навык применяется, сообщает «сколько сделано». Сколько из этого
     * станет опытом — решает источник (настройки роста, перки). Вызывается на сервере.
     */
    default void practice(Player player, ResourceLocation skill, double amount) {}

    /**
     * Прямой опыт навыка: ровно {@code xp} опыта (до множителей таланта, особенностей, боли), без пересчёта «практики»
     * (например, «за 100 выносливости»). Так спорт даёт опыт за каждый повтор. Сервер.
     * @since 0.5.0
     */
    default void grantXp(Player player, ResourceLocation skill, double xp) {}

    /** Есть ли у игрока черта. */
    default boolean hasTrait(Player player, ResourceLocation trait) {
        return false;
    }
}
