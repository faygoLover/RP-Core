package faygolover.rpcore.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * С игроком случилось что-то, что влияет на настроение (сервер). Шлют моды-источники, а что из этого следует (стресс,
 * рассудок, одиночество) — решает мод психики (RP Psyche) своими числами.
 * <ul>
 *   <li>{@link Kind#MESSAGE_SENT} — написал сообщение с КПК (RP PDA): одиночество немного отступает.</li>
 *   <li>{@link Kind#FED_CONTENT} — «Сыт и доволен»: разнообразно и сбалансированно поел за RP-сутки (RP Culinary).</li>
 *   <li>{@link Kind#FAVOURITE_FOOD} / {@link Kind#DISLIKED_FOOD} — съел любимое или нелюбимое (RP Culinary).</li>
 * </ul>
 * {@code strength} — доля силы события (1 — полная порция; укус большого блюда — меньше).
 */
public class RpMoodEvent extends PlayerEvent {
    /** NIGHTMARE — приснился кошмар (RP Needs), RESTED — выспался в нормальном месте (RP Needs). @since 0.11.0 */
    public enum Kind { MESSAGE_SENT, FED_CONTENT, FAVOURITE_FOOD, DISLIKED_FOOD, NIGHTMARE, RESTED }

    private final Kind kind;
    private final double strength;

    public RpMoodEvent(Player player, Kind kind, double strength) {
        super(player);
        this.kind = kind;
        this.strength = strength;
    }

    public Kind kind() {
        return kind;
    }

    public double strength() {
        return strength;
    }
}
