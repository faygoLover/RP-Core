package faygolover.rpcore.api;

import net.minecraft.world.entity.LivingEntity;

/**
 * Мод, который знает, что сущность несёт включённый свет: фонарь в руке, налобный, лампа на поясе. Отвечать надо
 * по тому, что видно у сущности на этой стороне (руки, надетое — {@link Worn}), — поставщика спрашивают и на
 * сервере, и на клиенте.
 * <p>
 * Регистрирует — {@link RpCoreAPI#registerLight}; спрашивают — {@link RpCoreAPI#carriesLight}. Списки чужих
 * светящих предметов остаются в конфиге того мода, который их ведёт: он регистрирует их своим поставщиком.
 *
 * @since 0.15.0
 */
@FunctionalInterface
public interface LightProvider {
    boolean carriesLight(LivingEntity entity);
}
